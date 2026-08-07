package com.hackathon.MBN.web;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.MBN.domain.Artist;
import com.hackathon.MBN.repository.ArtistRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class MetaController {

    private final ArtistRepository artistRepository;

    @GetMapping("/artists")
    public List<Map<String, Object>> getArtists() {
        return artistRepository.findAll().stream()
                .map(artist -> Map.<String, Object>of(
                        "id", artist.getId(),
                        "name", artist.getName(),
                        "aliases", artist.getAliases(),
                        "category", artist.getCategory()))
                .collect(Collectors.toList());
    }

    @GetMapping("/categories")
    public List<Map<String, Object>> getCategories() {
        return artistRepository.findAll().stream()
                .map(Artist::getCategory)
                .filter(category -> category != null && !category.isBlank())
                .distinct()
                .sorted()
                .map(category -> Map.<String, Object>of("name", category))
                .collect(Collectors.toList());
    }
}
