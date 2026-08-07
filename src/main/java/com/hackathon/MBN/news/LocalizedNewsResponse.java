package com.hackathon.MBN.news;

import com.hackathon.MBN.domain.Short;
import java.time.Instant;

public record LocalizedNewsResponse(
        Long id,
        Long eventId,
        String lang,
        String title,
        String body,
        Instant createdAt) {

    static LocalizedNewsResponse from(Short aShort) {
        return new LocalizedNewsResponse(
                aShort.getId(),
                aShort.getEvent().getId(),
                aShort.getLang(),
                aShort.getTitle(),
                aShort.getBody(),
                aShort.getCreatedAt());
    }
}
