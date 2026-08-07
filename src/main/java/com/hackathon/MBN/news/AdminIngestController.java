package com.hackathon.MBN.news;

import com.hackathon.MBN.ai.EventExtractionService;
import com.hackathon.MBN.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminIngestController {

    private final NewsIngestService newsIngestService;
    private final EventExtractionService eventExtractionService;

    public AdminIngestController(NewsIngestService newsIngestService, EventExtractionService eventExtractionService) {
        this.newsIngestService = newsIngestService;
        this.eventExtractionService = eventExtractionService;
    }

    // 수집 직후 바로 이어서 AI 추출까지 돌린다 (별도로 /admin/extract-events 호출할 필요 없음)
    @PostMapping("/ingest")
    public IngestAndExtractResult ingest(
            @RequestParam(defaultValue = "MBN") String query,
            @RequestParam(defaultValue = "10") int display) {
        if (query == null || query.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_QUERY", "query is required");
        }
        int safeDisplay = Math.min(Math.max(display, 1), 100);
        NewsIngestResult ingestResult = newsIngestService.ingest(query, safeDisplay);
        var extractionResult = eventExtractionService.extractEvents(safeDisplay);
        return new IngestAndExtractResult(ingestResult, extractionResult);
    }
}
