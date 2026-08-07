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
        String evidence) {}
