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
public class CommunityIngestService {

    private final RawArticleRepository rawArticles;
    private final SourceRepository sources;
    private final ArticleBodyFetcher bodyFetcher;

    public CommunityIngestService(RawArticleRepository rawArticles, SourceRepository sources, ArticleBodyFetcher bodyFetcher) {
        this.rawArticles = rawArticles;
        this.sources = sources;
        this.bodyFetcher = bodyFetcher;
    }

    @Transactional
    public <T extends SearchableContentItem> NewsIngestResult ingest(
            ContentSearcher<T> searcher, SourceType sourceType, String sourceName, String endpoint,
            String query, int display) {
        Source source = findOrCreateSource(sourceType, sourceName, endpoint);
        List<T> items = searcher.search(query, display);
        int saved = 0;
        int skipped = 0;
        Instant now = Instant.now();

        for (T item : items) {
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
                    .content(fetchBodyOrNull(item.url()))
                    .publishedAt(item.publishedAt())
                    .lastSeenAt(now)
                    .build());
            saved++;
        }

        return new NewsIngestResult(items.size(), saved, skipped);
    }

    private Source findOrCreateSource(SourceType sourceType, String name, String endpoint) {
        return sources.findByName(name)
                .orElseGet(() -> sources.save(Source.builder()
                        .sourceType(sourceType)
                        .name(name)
                        .endpoint(endpoint)
                        .mapsToPinType(PinType.ORIGIN)
                        .build()));
    }

    static String contentHash(SearchableContentItem item) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((item.title() + "\n" + item.url()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private String fetchBodyOrNull(String url) {
        try {
            return bodyFetcher.fetch(url);
        } catch (Exception ex) {
            // 비공개 카페 글 등 크롤링 실패 시 description 스니펫으로 폴백
            return null;
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
