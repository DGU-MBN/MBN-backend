package com.hackathon.MBN.news;

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

    public AdminIngestController(NewsIngestService newsIngestService) {
        this.newsIngestService = newsIngestService;
    }

    @PostMapping("/ingest")
    public NewsIngestResult ingest(
            @RequestParam(defaultValue = "MBN") String query,
            @RequestParam(defaultValue = "10") int display) {
        if (query == null || query.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_QUERY", "query is required");
        }
        int safeDisplay = Math.min(Math.max(display, 1), 100);
        return newsIngestService.ingest(query, safeDisplay);
    }
}
