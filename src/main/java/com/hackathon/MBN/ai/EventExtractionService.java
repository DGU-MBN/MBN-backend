package com.hackathon.MBN.ai;

import com.hackathon.MBN.domain.Event;
import com.hackathon.MBN.domain.EventLocation;
import com.hackathon.MBN.domain.RawArticle;
import com.hackathon.MBN.domain.Short;
import com.hackathon.MBN.domain.type.LocationPrecision;
import com.hackathon.MBN.domain.type.NewsCategory;
import com.hackathon.MBN.domain.type.PinType;
import com.hackathon.MBN.repository.EventLocationRepository;
import com.hackathon.MBN.repository.EventRepository;
import com.hackathon.MBN.repository.RawArticleRepository;
import com.hackathon.MBN.repository.ShortRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class EventExtractionService {

    // ponytail: 3개 언어 하드코딩. 언어 늘어나면 설정으로 빼기
    private record TargetLanguage(String code, String displayName) {}

    private static final List<TargetLanguage> TARGET_LANGUAGES = List.of(
            new TargetLanguage("en", "English"),
            new TargetLanguage("zh", "Chinese"),
            new TargetLanguage("ja", "Japanese"));

    private final RawArticleRepository rawArticles;
    private final EventRepository events;
    private final EventLocationRepository eventLocations;
    private final ShortRepository shorts;
    private final AiEventExtractor extractor;
    private final AiLocalizer localizer;

    public EventExtractionService(
            RawArticleRepository rawArticles,
            EventRepository events,
            EventLocationRepository eventLocations,
            ShortRepository shorts,
            AiEventExtractor extractor,
            AiLocalizer localizer) {
        this.rawArticles = rawArticles;
        this.events = events;
        this.eventLocations = eventLocations;
        this.shorts = shorts;
        this.extractor = extractor;
        this.localizer = localizer;
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
                        .summary(extractedBodyOrRawText(extraction, article))
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
                localizeAndSave(event);
            } catch (Exception ex) {
                skipped++;
            }
        }

        return new EventExtractionResult(unprocessed.size(), created, skipped);
    }

    // 언어별로 별도 호출 — 번역이 아니라 그 언어 기자가 새로 쓴 것처럼 재작성. 한 언어 실패해도 나머지는 계속
    private void localizeAndSave(Event event) {
        for (TargetLanguage lang : TARGET_LANGUAGES) {
            try {
                LocalizedContent localized = localizer.localize(event, lang.displayName());
                shorts.save(Short.builder()
                        .event(event)
                        .lang(lang.code())
                        .title(truncate(localized.title(), 500))
                        .body(localized.body())
                        .build());
            } catch (Exception ignored) {
                // 이 언어만 스킵, 이벤트 자체는 이미 생성됨
            }
        }
    }

    // 크롤링된 본문 전문이 있으면 그걸, 실패했으면 네이버 검색 API 스니펫으로 폴백
    private static String fullTextOrSnippet(RawArticle article) {
        return StringUtils.hasText(article.getContent()) ? article.getContent() : article.getDescription();
    }

    private static String extractedBodyOrRawText(ArticleExtraction extraction, RawArticle article) {
        return StringUtils.hasText(extraction.body()) ? extraction.body() : fullTextOrSnippet(article);
    }

    private static String normalizeCategory(String raw) {
        if (raw != null) {
            try {
                return NewsCategory.valueOf(raw.trim()).name();
            } catch (IllegalArgumentException ignored) {
                // 알 수 없는 카테고리는 사회로 폴백 (기타 카테고리 없음)
            }
        }
        return NewsCategory.사회.name();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
