package com.hackathon.MBN.news;

import com.hackathon.MBN.domain.Event;
import java.time.Instant;

public record KoreanNewsResponse(
        Long id,
        String category,
        String title,
        String locationName,
        String adminArea,
        String summary,
        Instant publishedAt) {

    static KoreanNewsResponse from(Event event) {
        return new KoreanNewsResponse(
                event.getId(),
                event.getCategory(),
                event.getTitle(),
                event.getLocationName(),
                event.getAdminArea(),
                event.getSummary(),
                event.getPublishedAt());
    }
}
