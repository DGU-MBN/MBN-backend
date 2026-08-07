package com.hackathon.MBN.ai;

import com.hackathon.MBN.domain.type.AiConfidence;
import com.hackathon.MBN.domain.type.LocationPrecision;

public record ArticleExtraction(
        String eventTitle,
        String locationName,
        String adminArea,
        Double lat,
        Double lng,
        LocationPrecision locationPrecision,
        String category,
        AiConfidence confidence,
        String evidence,
        String body) {

    public ArticleExtraction(
            String eventTitle,
            String locationName,
            String adminArea,
            Double lat,
            Double lng,
            LocationPrecision locationPrecision,
            String category,
            AiConfidence confidence,
            String evidence) {
        this(eventTitle, locationName, adminArea, lat, lng, locationPrecision, category, confidence, evidence, null);
    }
}
