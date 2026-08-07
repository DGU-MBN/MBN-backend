package com.hackathon.MBN.ai;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminEventExtractionController {

    private final EventExtractionService eventExtractionService;

    public AdminEventExtractionController(EventExtractionService eventExtractionService) {
        this.eventExtractionService = eventExtractionService;
    }

    @PostMapping("/extract-events")
    public EventExtractionResult extractEvents(@RequestParam(defaultValue = "10") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return eventExtractionService.extractEvents(safeLimit);
    }
}
