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
public class NewsIngestService {

    private final NaverNewsClient naverNewsClient;
    private final RawArticleRepository rawArticles;
    private final SourceRepository sources;

    public NewsIngestService(
            NaverNewsClient naverNewsClient,
            RawArticleRepository rawArticles,
            SourceRepository sources) {
        this.naverNewsClient = naverNewsClient;
        this.rawArticles = rawArticles;
        this.sources = sources;
    }

    @Transactional
    public NewsIngestResult ingest(String query, int display) {
        Source source = naverSource();
        List<NaverNewsItem> items = naverNewsClient.search(query, display);
        int saved = 0;
        int skipped = 0;
        Instant now = Instant.now();

        for (NaverNewsItem item : items) {
            String hash = contentHash(item);
            if (rawArticles.existsByContentHash(hash)) {
                skipped++;
                continue;
            }
            rawArticles.save(RawArticle.builder()
                    .source(source)
                    .url(truncate(item.url(), 1000))
                    .contentHash(hash)
                    .title(truncate(item.title(), 500))
                    .description(item.description())
                    .publishedAt(item.publishedAt())
                    .lastSeenAt(now)
                    .build());
            saved++;
        }

        return new NewsIngestResult(items.size(), saved, skipped);
    }

    static String contentHash(NaverNewsItem item) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((item.title() + "\n" + item.url()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private Source naverSource() {
        return sources.findByName(NaverNewsClient.SOURCE_NAME)
                .orElseGet(() -> sources.save(Source.builder()
                        .sourceType(SourceType.NEWS_RSS)
                        .name(NaverNewsClient.SOURCE_NAME)
                        .endpoint(NaverNewsClient.NEWS_ENDPOINT)
                        .pollIntervalMin(10)
                        .mapsToPinType(PinType.ORIGIN)
                        .build()));
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
