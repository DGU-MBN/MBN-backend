package com.hackathon.MBN.news;

import com.hackathon.MBN.ai.EventExtractionService;
import com.hackathon.MBN.domain.type.SourceType;
import com.hackathon.MBN.web.ApiException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminIngestController {

    private static final int MAX_COUNT = 10;

    private final NewsIngestService newsIngestService;
    private final YoutubeIngestService youtubeIngestService;
    private final CommunityIngestService communityIngestService;
    private final NaverBlogClient naverBlogClient;
    private final NaverCafeClient naverCafeClient;
    private final EventExtractionService eventExtractionService;
    private final WatchlistProperties watchlist;

    public AdminIngestController(
            NewsIngestService newsIngestService,
            YoutubeIngestService youtubeIngestService,
            CommunityIngestService communityIngestService,
            NaverBlogClient naverBlogClient,
            NaverCafeClient naverCafeClient,
            EventExtractionService eventExtractionService,
            WatchlistProperties watchlist) {
        this.newsIngestService = newsIngestService;
        this.youtubeIngestService = youtubeIngestService;
        this.communityIngestService = communityIngestService;
        this.naverBlogClient = naverBlogClient;
        this.naverCafeClient = naverCafeClient;
        this.eventExtractionService = eventExtractionService;
        this.watchlist = watchlist;
    }

    // 수집 직후 바로 이어서 AI 추출까지 돌린다 (별도로 /admin/extract-events 호출할 필요 없음)
    // query를 안 주면 watchlist.queries-by-category에서 count개를 카테고리 고루 섞어 무작위로 골라 각각 수집한다.
    @PostMapping("/ingest")
    public IngestAndExtractResult ingest(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "10") int display,
            @RequestParam(defaultValue = "1") int count) {
        List<String> queries = resolveQueries(query, count);
        int safeDisplay = clampDisplay(display);
        NewsIngestResult ingestResult = ingestAll(queries, q -> newsIngestService.ingest(q, safeDisplay));
        var extractionResult = eventExtractionService.extractEvents(safeDisplay * queries.size());
        return new IngestAndExtractResult(ingestResult, extractionResult);
    }

    // channelId를 안 주면 watchlist.youtube-channel-ids에서 count개를 중복 없이 무작위로 골라 각각 수집한다.
    @PostMapping("/ingest/youtube")
    public IngestAndExtractResult ingestYoutube(
            @RequestParam(required = false) String channelId,
            @RequestParam(defaultValue = "10") int display,
            @RequestParam(defaultValue = "1") int count) {
        List<String> channelIds = StringUtils.hasText(channelId)
                ? List.of(channelId)
                : pickDistinctRandom(watchlist.youtubeChannelIds(), clampCount(count),
                        "NO_CHANNEL_CONFIGURED", "channelId is required and watchlist.youtube-channel-ids is empty");
        int safeDisplay = clampDisplay(display);
        NewsIngestResult ingestResult = ingestAll(channelIds, id -> youtubeIngestService.ingest(id, safeDisplay));
        return new IngestAndExtractResult(ingestResult, eventExtractionService.extractEvents(safeDisplay * channelIds.size()));
    }

    @PostMapping("/ingest/blog")
    public IngestAndExtractResult ingestBlog(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "10") int display,
            @RequestParam(defaultValue = "1") int count) {
        List<String> queries = resolveQueries(query, count);
        int safeDisplay = clampDisplay(display);
        NewsIngestResult ingestResult = ingestAll(queries, q -> communityIngestService.ingest(
                naverBlogClient::search, SourceType.NAVER_BLOG, NaverBlogClient.SOURCE_NAME, NaverBlogClient.BLOG_ENDPOINT,
                q, safeDisplay));
        return new IngestAndExtractResult(ingestResult, eventExtractionService.extractEvents(safeDisplay * queries.size()));
    }

    @PostMapping("/ingest/cafe")
    public IngestAndExtractResult ingestCafe(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "10") int display,
            @RequestParam(defaultValue = "1") int count) {
        List<String> queries = resolveQueries(query, count);
        int safeDisplay = clampDisplay(display);
        NewsIngestResult ingestResult = ingestAll(queries, q -> communityIngestService.ingest(
                naverCafeClient::search, SourceType.NAVER_CAFE, NaverCafeClient.SOURCE_NAME, NaverCafeClient.CAFE_ENDPOINT,
                q, safeDisplay));
        return new IngestAndExtractResult(ingestResult, eventExtractionService.extractEvents(safeDisplay * queries.size()));
    }

    private static NewsIngestResult ingestAll(List<String> keys, java.util.function.Function<String, NewsIngestResult> ingestOne) {
        int fetched = 0;
        int saved = 0;
        int skipped = 0;
        for (String key : keys) {
            NewsIngestResult result = ingestOne.apply(key);
            fetched += result.fetched();
            saved += result.saved();
            skipped += result.skipped();
        }
        return new NewsIngestResult(fetched, saved, skipped);
    }

    // query가 명시되면 그것만, 아니면 watchlist.queries-by-category의 카테고리들을 무작위 순서로 순회하며
    // count개를 뽑는다 (카테고리 수보다 count가 크면 순환하며 카테고리별로 다시 하나씩 더 뽑는다).
    private List<String> resolveQueries(String requested, int count) {
        if (StringUtils.hasText(requested)) {
            return List.of(requested);
        }
        List<String> categories = new ArrayList<>(watchlist.queriesByCategory().keySet());
        if (categories.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "NO_QUERY_CONFIGURED",
                    "query is required and watchlist.queries-by-category is empty");
        }
        Collections.shuffle(categories);
        int safeCount = clampCount(count);
        List<String> picks = new ArrayList<>();
        for (int i = 0; i < safeCount; i++) {
            List<String> pool = watchlist.queriesByCategory().get(categories.get(i % categories.size()));
            if (pool != null && !pool.isEmpty()) {
                picks.add(pool.get(ThreadLocalRandom.current().nextInt(pool.size())));
            }
        }
        return picks;
    }

    private static List<String> pickDistinctRandom(List<String> pool, int count, String errorCode, String errorMessage) {
        if (pool.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, errorCode, errorMessage);
        }
        List<String> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    private static int clampDisplay(int display) {
        return Math.min(Math.max(display, 1), 100);
    }

    private static int clampCount(int count) {
        return Math.min(Math.max(count, 1), MAX_COUNT);
    }
}
