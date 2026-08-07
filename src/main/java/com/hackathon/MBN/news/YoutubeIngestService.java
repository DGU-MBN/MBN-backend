package com.hackathon.MBN.news;

import com.hackathon.MBN.domain.RawArticle;
import com.hackathon.MBN.domain.Source;
import com.hackathon.MBN.domain.type.PinType;
import com.hackathon.MBN.domain.type.SourceType;
import com.hackathon.MBN.repository.RawArticleRepository;
import com.hackathon.MBN.repository.SourceRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class YoutubeIngestService {

    private final YoutubeClient client;
    private final RawArticleRepository rawArticles;
    private final SourceRepository sources;

    public YoutubeIngestService(YoutubeClient client, RawArticleRepository rawArticles, SourceRepository sources) {
        this.client = client;
        this.rawArticles = rawArticles;
        this.sources = sources;
    }

    @Transactional
    public NewsIngestResult ingest(String channelId, int display) {
        Source source = channelSource(channelId);
        String playlistId = client.resolveUploadsPlaylistId(channelId);
        List<YoutubeVideoItem> videos = client.latestVideos(playlistId, display);
        int saved = 0;
        int skipped = 0;
        Instant now = Instant.now();

        for (YoutubeVideoItem video : videos) {
            String hash = contentHash(video);
            if (rawArticles.existsByContentHash(hash)) {
                skipped++;
                continue;
            }
            rawArticles.save(RawArticle.builder()
                    .source(source)
                    .url(video.url())
                    .contentHash(hash)
                    .title(truncate(video.title(), 500))
                    .description(video.description())
                    .content(video.description())
                    .publishedAt(video.publishedAt())
                    .lastSeenAt(now)
                    .build());
            saved++;
        }

        return new NewsIngestResult(videos.size(), saved, skipped);
    }

    private Source channelSource(String channelId) {
        String name = "YouTube:" + channelId;
        return sources.findByName(name)
                .orElseGet(() -> sources.save(Source.builder()
                        .sourceType(SourceType.YOUTUBE_OFFICIAL)
                        .name(name)
                        .endpoint("https://www.youtube.com/channel/" + channelId)
                        .mapsToPinType(PinType.ORIGIN)
                        .build()));
    }

    static String contentHash(YoutubeVideoItem video) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((video.title() + "\n" + video.url()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
