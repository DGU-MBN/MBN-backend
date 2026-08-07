package com.hackathon.MBN.web;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.MBN.domain.Event;
import com.hackathon.MBN.domain.EventEntity;
import com.hackathon.MBN.domain.EventLocation;
import com.hackathon.MBN.domain.RawArticle;
import com.hackathon.MBN.domain.Short;
import com.hackathon.MBN.repository.EventEntityRepository;
import com.hackathon.MBN.repository.EventLocationRepository;
import com.hackathon.MBN.repository.EventRepository;
import com.hackathon.MBN.repository.ShortRepository;

import lombok.RequiredArgsConstructor;

/** 기능명세서 10~12(Feed/오토플레이/출처), 08(이벤트 상세 카드) 담당. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FeedController {

    private static final int DEFAULT_LIMIT = 10;

    private final EventRepository eventRepository;
    private final ShortRepository shortRepository;
    private final EventEntityRepository eventEntityRepository;
    private final EventLocationRepository eventLocationRepository;

    @GetMapping("/feed")
    public Map<String, Object> getFeed(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long artistId,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String sort) {

        Long cursorId = (cursor != null && !cursor.isBlank()) ? Long.valueOf(cursor) : null;
        int pageSize = (limit == null || limit <= 0) ? DEFAULT_LIMIT : limit;
        Pageable pageable = PageRequest.of(0, pageSize);
        boolean popular = "popular".equalsIgnoreCase(sort);

        List<Event> events = eventRepository.findFeedCandidates(cursorId, category, artistId, country, language, popular, pageable);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("items", events.stream().map(e -> toFeedItem(e, language)).collect(Collectors.toList()));
        response.put("nextCursor", events.size() < pageSize || events.isEmpty()
                ? null
                : String.valueOf(events.get(events.size() - 1).getId()));
        return response;
    }

    @GetMapping("/events/{eventId}")
    public Map<String, Object> getEventDetail(@PathVariable Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "Event not found: " + eventId));
        return toEventDetail(event);
    }

    @GetMapping("/events/{eventId}/variants")
    public List<Map<String, Object>> getEventVariants(@PathVariable Long eventId) {
        return shortRepository.findByEventId(eventId).stream()
                .map(this::toShortVariant)
                .collect(Collectors.toList());
    }

    /** 기능명세서 12: AI가 재구성한 팩트가 아니라 실제로 근거한 원문 출처 목록. */
    @GetMapping("/events/{eventId}/sources")
    public List<Map<String, Object>> getEventSources(@PathVariable Long eventId) {
        return toSources(eventEntityRepository.findByEventId(eventId));
    }

    @GetMapping("/shorts/{shortId}")
    public Map<String, Object> getShort(@PathVariable Long shortId) {
        Short shortItem = shortRepository.findById(shortId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SHORT_NOT_FOUND", "Short not found: " + shortId));
        return toShortResponse(shortItem);
    }

    private Map<String, Object> toFeedItem(Event event, String language) {
        Short representative = (language != null && !language.isBlank())
                ? shortRepository.findFirstByEventIdAndLang(event.getId(), language)
                        .or(() -> shortRepository.findFirstByEventId(event.getId()))
                        .orElse(null)
                : shortRepository.findFirstByEventId(event.getId()).orElse(null);
        EventLocation location = eventLocationRepository.findFirstByEventIdAndPrimaryTrue(event.getId()).orElse(null);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("eventId", event.getId());
        item.put("title", event.getTitle());
        item.put("category", event.getCategory());
        item.put("videoUrl", representative != null ? representative.getVideoUrl() : null);
        item.put("location", location != null ? location.getLocationName() : null);
        item.put("country", location != null ? location.getCountry() : null);
        item.put("language", representative != null ? representative.getLang() : null);
        item.put("byline", event.getByline());
        return item;
    }

    private Map<String, Object> toEventDetail(Event event) {
        List<EventEntity> entities = eventEntityRepository.findByEventId(event.getId());
        EventLocation location = eventLocationRepository.findFirstByEventIdAndPrimaryTrue(event.getId()).orElse(null);
        Short representative = shortRepository.findFirstByEventId(event.getId()).orElse(null);

        Map<String, Object> locationResponse = null;
        if (location != null) {
            locationResponse = new LinkedHashMap<>();
            locationResponse.put("name", location.getLocationName());
            locationResponse.put("country", location.getCountry());
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", event.getId());
        response.put("title", event.getTitle());
        response.put("category", event.getCategory());
        response.put("pinType", event.getPinType().name());
        response.put("status", event.getStatus().name());
        response.put("confidence", event.getConfidence().name());
        response.put("reviewReason", event.getReviewReason());
        response.put("byline", event.getByline());
        response.put("programName", event.getProgramName());
        response.put("publishedAt", event.getPublishedAt());
        response.put("popularity", event.getPopularity());
        response.put("location", locationResponse);
        response.put("videoUrl", representative != null ? representative.getVideoUrl() : null);
        response.put("facts", entities.stream().map(this::toFact).collect(Collectors.toList()));
        response.put("sources", toSources(entities));
        return response;
    }

    private List<Map<String, Object>> toSources(List<EventEntity> entities) {
        // 같은 원문이 여러 팩트의 근거로 쓰일 수 있으므로 rawArticle 기준 중복 제거
        LinkedHashSet<RawArticle> articles = entities.stream()
                .map(EventEntity::getRawArticle)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return articles.stream().map(article -> {
            Map<String, Object> source = new LinkedHashMap<>();
            source.put("publisher", article.getSource() != null ? article.getSource().getName() : null);
            source.put("title", article.getTitle());
            source.put("url", article.getUrl());
            source.put("publishedAt", article.getPublishedAt());
            return source;
        }).collect(Collectors.toList());
    }

    private Map<String, Object> toShortResponse(Short shortItem) {
        Long eventId = shortItem.getEvent() != null ? shortItem.getEvent().getId() : null;
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", shortItem.getId());
        response.put("eventId", eventId);
        response.put("title", shortItem.getTitle());
        response.put("lang", shortItem.getLang());
        response.put("style", shortItem.getStyle() != null ? shortItem.getStyle().name() : null);
        response.put("videoUrl", shortItem.getVideoUrl());
        response.put("hlsUrl", shortItem.getHlsUrl());
        response.put("thumbnailUrl", shortItem.getThumbnailUrl());
        response.put("views", shortItem.getViews());
        response.put("likes", shortItem.getLikes());
        return response;
    }

    private Map<String, Object> toShortVariant(Short shortItem) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", shortItem.getId());
        response.put("title", shortItem.getTitle());
        response.put("lang", shortItem.getLang());
        response.put("style", shortItem.getStyle() != null ? shortItem.getStyle().name() : null);
        response.put("thumbnailUrl", shortItem.getThumbnailUrl());
        return response;
    }

    private Map<String, Object> toFact(EventEntity entity) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", entity.getId());
        response.put("factText", entity.getFactText());
        response.put("verified", entity.isVerified());
        return response;
    }
}
