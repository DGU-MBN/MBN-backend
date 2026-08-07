package com.hackathon.MBN.web;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.MBN.domain.Event;
import com.hackathon.MBN.domain.EventLocation;
import com.hackathon.MBN.domain.Interaction;
import com.hackathon.MBN.domain.type.InteractionType;
import com.hackathon.MBN.repository.EventLocationRepository;
import com.hackathon.MBN.repository.InteractionRepository;

import lombok.RequiredArgsConstructor;

/** 기능명세서 03~06(지도 조회/Origin Pin/클러스터링/필터), 09(Reaction Heatmap) 담당. */
@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

    private final EventLocationRepository eventLocationRepository;
    private final InteractionRepository interactionRepository;

    @GetMapping("/pins")
    public List<Map<String, Object>> getPins(
            @RequestParam double north,
            @RequestParam double south,
            @RequestParam double east,
            @RequestParam double west,
            @RequestParam(required = false) Integer zoom,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String sort) {

        Instant fromInstant = resolveFrom(period, from);
        Instant toInstant = (to != null && !to.isBlank()) ? Instant.parse(to) : null;

        List<EventLocation> locations = eventLocationRepository
                .findByLatBetweenAndLngBetween(south, north, west, east).stream()
                .filter(loc -> category == null || category.isBlank() || category.equalsIgnoreCase(loc.getEvent().getCategory()))
                .filter(loc -> fromInstant == null || loc.getEvent().getPublishedAt() == null
                        || !loc.getEvent().getPublishedAt().isBefore(fromInstant))
                .filter(loc -> toInstant == null || loc.getEvent().getPublishedAt() == null
                        || !loc.getEvent().getPublishedAt().isAfter(toInstant))
                .collect(Collectors.toList());

        if ("POPULAR".equalsIgnoreCase(sort)) {
            locations.sort(Comparator.comparing((EventLocation l) -> l.getEvent().getPopularity(),
                    Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
        } else {
            locations.sort(Comparator.comparing((EventLocation l) -> l.getEvent().getPublishedAt(),
                    Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
        }

        return clusterByZoom(locations, zoom);
    }

    /**
     * 기능명세서 05: 줌 레벨이 낮을수록(축소) 좌표를 성기게 반올림해 묶는다.
     * 같은 격자에 핀이 1개면 단일 핀 그대로, 2개 이상이면 클러스터로 반환한다.
     */
    private List<Map<String, Object>> clusterByZoom(List<EventLocation> locations, Integer zoom) {
        int precision = precisionForZoom(zoom);

        Map<String, List<EventLocation>> grouped = new LinkedHashMap<>();
        for (EventLocation location : locations) {
            String key = round(location.getLat(), precision) + "," + round(location.getLng(), precision);
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(location);
        }

        List<Map<String, Object>> pins = new ArrayList<>();
        for (List<EventLocation> group : grouped.values()) {
            if (group.size() == 1) {
                pins.add(toPinResponse(group.get(0)));
            } else {
                EventLocation representative = group.get(0);
                Map<String, Object> cluster = new LinkedHashMap<>();
                cluster.put("type", "cluster");
                cluster.put("latitude", representative.getLat());
                cluster.put("longitude", representative.getLng());
                cluster.put("count", group.size());
                cluster.put("category", representative.getEvent().getCategory());
                pins.add(cluster);
            }
        }
        return pins;
    }

    private int precisionForZoom(Integer zoom) {
        int z = (zoom == null) ? 10 : zoom;
        if (z <= 5) return 0;
        if (z <= 9) return 1;
        if (z <= 13) return 2;
        return 6; // 사실상 반올림 없이 개별 핀 유지
    }

    private double round(double value, int precision) {
        double factor = Math.pow(10, precision);
        return Math.round(value * factor) / factor;
    }

    private Map<String, Object> toPinResponse(EventLocation location) {
        Event event = location.getEvent();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("type", "single");
        response.put("eventId", event.getId());
        response.put("latitude", location.getLat());
        response.put("longitude", location.getLng());
        response.put("category", event.getCategory());
        response.put("title", event.getTitle());
        response.put("publishedAt", event.getPublishedAt());
        response.put("popularity", event.getPopularity());
        return response;
    }

    private Instant resolveFrom(String period, String from) {
        if (from != null && !from.isBlank()) {
            return Instant.parse(from);
        }
        if (period == null || period.isBlank()) {
            return null;
        }
        Instant now = Instant.now();
        return switch (period.toUpperCase()) {
            case "24H" -> now.minus(Duration.ofHours(24));
            case "7D" -> now.minus(Duration.ofDays(7));
            case "30D" -> now.minus(Duration.ofDays(30));
            default -> null;
        };
    }

    /**
     * 기능명세서 09: Origin이 "어디서 일어났는가"라면 Reaction은 "어디서 반응하는가".
     * interactions.country_code를 국가 단위로 배치 집계한다.
     */
    @GetMapping("/heatmap")
    public Map<String, Object> getHeatmap(@RequestParam(required = false) Long eventId) {
        List<Interaction> interactions = (eventId != null)
                ? interactionRepository.findByShortVideo_Event_Id(eventId)
                : interactionRepository.findAll();

        Map<String, List<Interaction>> byCountry = interactions.stream()
                .filter(i -> i.getCountryCode() != null && !i.getCountryCode().isBlank())
                .collect(Collectors.groupingBy(Interaction::getCountryCode));

        List<Map<String, Object>> cells = byCountry.entrySet().stream()
                .map(entry -> toHeatmapCell(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Long.compare((long) b.get("reactionScore"), (long) a.get("reactionScore")))
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("eventId", eventId);
        response.put("cells", cells);
        return response;
    }

    private Map<String, Object> toHeatmapCell(String countryCode, List<Interaction> countryInteractions) {
        long views = countInteractions(countryInteractions, InteractionType.VIEW);
        long likes = countInteractions(countryInteractions, InteractionType.LIKE);
        long shares = countInteractions(countryInteractions, InteractionType.SHARE);
        // 기능명세서 09: "정확한 산식은 나중, MVP에서는 단순 가중합으로 충분"
        long reactionScore = views + (likes * 2) + (shares * 3);

        Map<String, Object> cell = new LinkedHashMap<>();
        cell.put("country", countryCode);
        cell.put("views", views);
        cell.put("likes", likes);
        cell.put("shares", shares);
        cell.put("reactionScore", reactionScore);
        return cell;
    }

    private long countInteractions(List<Interaction> interactions, InteractionType type) {
        return interactions.stream().filter(i -> i.getType() == type).count();
    }
}
