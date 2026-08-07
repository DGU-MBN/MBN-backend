package com.hackathon.MBN.web;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.MBN.domain.EventLocation;
import com.hackathon.MBN.repository.EventLocationRepository;
import com.hackathon.MBN.repository.EventRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class MapController {

    private final EventLocationRepository eventLocationRepository;
    private final EventRepository eventRepository;

    @GetMapping("/map/pins")
    public List<Map<String, Object>> getPins(
            @RequestParam double minLat,
            @RequestParam double maxLat,
            @RequestParam double minLng,
            @RequestParam double maxLng) {
        return eventLocationRepository.findByLatBetweenAndLngBetween(minLat, maxLat, minLng, maxLng).stream()
                .map(this::toPinResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/map/heatmap")
    public List<Map<String, Object>> getHeatmap(
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLat,
            @RequestParam(required = false) Double minLng,
            @RequestParam(required = false) Double maxLng) {
        List<EventLocation> locations = (minLat != null && maxLat != null && minLng != null && maxLng != null)
                ? eventLocationRepository.findByLatBetweenAndLngBetween(minLat, maxLat, minLng, maxLng)
                : eventLocationRepository.findAll();

        return locations.stream()
                .collect(Collectors.groupingBy(location -> {
                    double roundedLat = Math.round(location.getLat() * 10) / 10.0;
                    double roundedLng = Math.round(location.getLng() * 10) / 10.0;
                    return roundedLat + "," + roundedLng;
                }))
                .entrySet().stream()
                .map(entry -> Map.<String, Object>of(
                        "lat", Double.parseDouble(entry.getKey().split(",")[0]),
                        "lng", Double.parseDouble(entry.getKey().split(",")[1]),
                        "count", entry.getValue().size()))
                .collect(Collectors.toList());
    }

    private Map<String, Object> toPinResponse(EventLocation location) {
        return Map.<String, Object>of(
                "id", location.getId(),
                "eventId", location.getEvent().getId(),
                "title", location.getEvent().getTitle(),
                "category", location.getEvent().getCategory(),
                "pinType", location.getEvent().getPinType().name(),
                "lat", location.getLat(),
                "lng", location.getLng(),
                "locationName", location.getLocationName(),
                "precision", location.getPrecision().name());
    }
}
