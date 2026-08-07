package com.hackathon.MBN.news;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hackathon.MBN.domain.RawArticle;
import com.hackathon.MBN.domain.Source;
import com.hackathon.MBN.domain.type.NewsCategory;
import com.hackathon.MBN.domain.type.PinType;
import com.hackathon.MBN.domain.type.SourceType;
import com.hackathon.MBN.repository.RawArticleRepository;
import com.hackathon.MBN.repository.SourceRepository;

@Service
public class NewsIngestService {

    /*
     * 네이버 뉴스 API는 키워드 검색이라 카테고리로 직접 못 부른다.
     * 카테고리마다 대표 검색어를 정의해 그 키워드들로 수집한다. (검색어는 튜닝 가능)
     */
    private static final Map<NewsCategory, List<String>> CATEGORY_KEYWORDS = Map.of(
            NewsCategory.정치, List.of("정치", "정부 정책"),
            NewsCategory.경제, List.of("경제", "산업 동향"),
            NewsCategory.사회, List.of("사회 이슈"),
            NewsCategory.문화, List.of("문화 행사", "공연 전시"),
            NewsCategory.스포츠, List.of("스포츠"),
            NewsCategory.연예, List.of("연예"),
            NewsCategory.식품, List.of("식품", "먹거리", "외식 트렌드"),
            NewsCategory.의료, List.of("의료", "병원", "건강 의학"));

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
        int[] savedSkipped = saveItems(items, source, Instant.now());
        return new NewsIngestResult(items.size(), savedSkipped[0], savedSkipped[1]);
    }

    /**
     * 카테고리의 대표 검색어들로 각각 수집한다. displayPerKeyword는 키워드당 가져올 기사 수.
     * 매핑이 없는 카테고리는 카테고리명 자체를 검색어로 쓴다.
     */
    @Transactional
    public NewsIngestResult ingestByCategory(NewsCategory category, int displayPerKeyword) {
        List<String> keywords = CATEGORY_KEYWORDS.getOrDefault(category, List.of(category.name()));
        Source source = naverSource();
        Instant now = Instant.now();
        int fetched = 0;
        int saved = 0;
        int skipped = 0;
        for (String keyword : keywords) {
            List<NaverNewsItem> items = naverNewsClient.search(keyword, displayPerKeyword);
            fetched += items.size();
            int[] savedSkipped = saveItems(items, source, now);
            saved += savedSkipped[0];
            skipped += savedSkipped[1];
        }
        return new NewsIngestResult(fetched, saved, skipped);
    }

    /** 기사 목록을 저장한다. contentHash 중복은 건너뛴다. 반환: [saved, skipped]. */
    private int[] saveItems(List<NaverNewsItem> items, Source source, Instant now) {
        int saved = 0;
        int skipped = 0;
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
        return new int[] {saved, skipped};
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
