package com.hackathon.MBN.ai;

import com.hackathon.MBN.domain.Event;
import com.hackathon.MBN.domain.EventLocation;
import com.hackathon.MBN.domain.RawArticle;
import com.hackathon.MBN.domain.type.LocationPrecision;
import com.hackathon.MBN.domain.type.NewsCategory;
import com.hackathon.MBN.domain.type.PinType;
import com.hackathon.MBN.repository.EventLocationRepository;
import com.hackathon.MBN.repository.EventRepository;
import com.hackathon.MBN.repository.RawArticleRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EventExtractionService {

    private final RawArticleRepository rawArticles;
    private final EventRepository events;
    private final EventLocationRepository eventLocations;
    private final AiEventExtractor extractor;

    public EventExtractionService(
            RawArticleRepository rawArticles,
            EventRepository events,
            EventLocationRepository eventLocations,
            AiEventExtractor extractor) {
        this.rawArticles = rawArticles;
        this.events = events;
        this.eventLocations = eventLocations;
        this.extractor = extractor;
    }

    @Transactional
    public EventExtractionResult extractEvents(int limit) {
        List<RawArticle> unprocessed = rawArticles.findUnprocessed(PageRequest.of(0, limit));
        int created = 0;
        int skipped = 0;

        for (RawArticle article : unprocessed) {
            // 기사 하나가 AI 호출/파싱에 실패해도 나머지는 계속 처리한다
            try {
                ArticleExtraction extraction = extractor.extract(article);
                if (!StringUtils.hasText(extraction.locationName())) {
                    skipped++;
                    continue;
                }
                Event event = events.save(Event.builder()
                        .sourceArticle(article)
                        .pinType(PinType.ORIGIN)
                        .category(normalizeCategory(extraction.category()))
                        .title(truncate(extraction.eventTitle(), 500))
                        .locationName(extraction.locationName())
                        .adminArea(extraction.adminArea())
                        .summary(article.getDescription())
                        .evidence(extraction.evidence())
                        .aiConfidence(extraction.confidence())
                        .publishedAt(article.getPublishedAt())
                        .build());
                if (extraction.lat() != null && extraction.lng() != null) {
                    eventLocations.save(EventLocation.builder()
                            .event(event)
                            .lat(extraction.lat())
                            .lng(extraction.lng())
                            .precision(extraction.locationPrecision() != null
                                    ? extraction.locationPrecision()
                                    : LocationPrecision.CITY)
                            .locationName(extraction.locationName())
                            .build());
                }
                created++;
            } catch (Exception ex) {
                skipped++;
            }
        }

        return new EventExtractionResult(unprocessed.size(), created, skipped);
    }

    private static String normalizeCategory(String raw) {
        if (raw != null) {
            try {
                return NewsCategory.valueOf(raw.trim()).name();
            } catch (IllegalArgumentException ignored) {
                // 알 수 없는 카테고리는 사회일반으로 폴백 (기타 카테고리 없음)
            }
        }
        return NewsCategory.사회일반.name();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
