package com.hackathon.MBN.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.MBN.domain.Artist;
import com.hackathon.MBN.domain.Event;
import com.hackathon.MBN.repository.ArtistRepository;
import com.hackathon.MBN.repository.EventRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MetaController {

    /** 기능명세서 02: 관심 카테고리는 이 8개 중에서 고른다. Artist.category(자유 문자열)와는 별개의 고정 목록. */
    private static final List<String> CATEGORIES = List.of(
            "KPOP", "DRAMA", "MOVIE", "ENTERTAINMENT", "FASHION", "BEAUTY", "FOOD", "SPORTS");

    private final ArtistRepository artistRepository;
    private final EventRepository eventRepository;

    @GetMapping("/artists")
    public List<Map<String, Object>> getArtists(@RequestParam(required = false) String q) {
        List<Artist> artists = (q == null || q.isBlank())
                ? artistRepository.findAll()
                : artistRepository.findByNameContainingIgnoreCase(q);
        return artists.stream()
                .map(artist -> Map.<String, Object>of(
                        "id", artist.getId(),
                        "name", artist.getName(),
                        "aliases", artist.getAliases(),
                        "category", artist.getCategory()))
                .collect(Collectors.toList());
    }

    @GetMapping("/categories")
    public List<String> getCategories() {
        return CATEGORIES;
    }

    /** 기능명세서 07: 아티스트 + 뉴스 제목 통합 검색. 2자 미만 호출 여부는 클라이언트 책임. */
    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }

        List<Map<String, Object>> results = new ArrayList<>();
        artistRepository.findByNameContainingIgnoreCase(q).forEach(artist -> results.add(Map.of(
                "type", "ARTIST",
                "id", artist.getId(),
                "name", artist.getName())));

        String needle = q.toLowerCase(Locale.ROOT);
        for (Event event : eventRepository.findAll()) {
            if (event.getTitle() != null && event.getTitle().toLowerCase(Locale.ROOT).contains(needle)) {
                results.add(Map.of(
                        "type", "EVENT",
                        "id", event.getId(),
                        "name", event.getTitle()));
            }
        }
        return results;
    }
}
