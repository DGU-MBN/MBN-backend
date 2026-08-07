package com.hackathon.MBN.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.MBN.domain.Event;
import com.hackathon.MBN.domain.EventEntity;
import com.hackathon.MBN.domain.Short;
import com.hackathon.MBN.repository.EventEntityRepository;
import com.hackathon.MBN.repository.EventRepository;
import com.hackathon.MBN.repository.ShortRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class FeedController {

    private final EventRepository eventRepository;
    private final ShortRepository shortRepository;
    private final EventEntityRepository eventEntityRepository;

    @GetMapping("/feed")
    public List<Map<String, Object>> getFeed() {
        return eventRepository.findAll().stream()
                .limit(10)
                .map(this::toEventSummary)
                .collect(Collectors.toList());
    }

    @GetMapping("/events/{eventId}")
    public Map<String, Object> getEventDetail(@PathVariable Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EVENT_NOT_FOUND", "Event not found: " + eventId));
        return toEventDetail(event);
    }

    @GetMapping("/events/{eventId}/variants")
    public List<Map<String, Object>> getEventVariants(@PathVariable Long eventId) {
        return shortRepository.findAll().stream()
                .filter(shortItem -> shortItem.getEvent() != null
                        && shortItem.getEvent().getId() != null
                        && eventId != null
                        && eventId.equals(shortItem.getEvent().getId()))
                .map(this::toShortVariant)
                .collect(Collectors.toList());
    }

    @GetMapping("/shorts/{shortId}")
    public Map<String, Object> getShort(@PathVariable Long shortId) {
        Short shortItem = shortRepository.findById(shortId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SHORT_NOT_FOUND", "Short not found: " + shortId));
        return toShortResponse(shortItem);
    }

    private Map<String, Object> toEventSummary(Event event) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", event.getId());
        response.put("title", event.getTitle());
        response.put("category", event.getCategory());
        response.put("pinType", event.getPinType() != null ? event.getPinType().name() : null);
        response.put("status", event.getStatus() != null ? event.getStatus().name() : null);
        response.put("confidence", event.getConfidence() != null ? event.getConfidence().name() : null);
        return response;
    }

    private Map<String, Object> toEventDetail(Event event) {
        List<EventEntity> entities = eventEntityRepository.findAll().stream()
                .filter(entity -> entity.getEvent() != null && entity.getEvent().getId().equals(event.getId()))
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", event.getId());
        response.put("title", event.getTitle());
        response.put("category", event.getCategory());
        response.put("pinType", event.getPinType().name());
        response.put("status", event.getStatus().name());
        response.put("confidence", event.getConfidence().name());
        response.put("reviewReason", event.getReviewReason());
        response.put("programName", event.getProgramName());
        response.put("publishedAt", event.getPublishedAt());
        response.put("popularity", event.getPopularity());
        response.put("facts", entities.stream().map(this::toFact).collect(Collectors.toList()));
        return response;
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
