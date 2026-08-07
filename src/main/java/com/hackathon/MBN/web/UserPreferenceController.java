package com.hackathon.MBN.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.MBN.domain.User;
import com.hackathon.MBN.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserRepository userRepository;

    @GetMapping("/language")
    public Map<String, Object> getLanguage(@CurrentUser User user) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("language", normalizeLanguage(user.getPreferredLang()));
        return response;
    }

    @PostMapping("/language")
    public Map<String, Object> updateLanguage(@CurrentUser User user, @RequestBody LanguageRequest request) {
        String language = normalizeLanguage(request.language());
        user.setPreferredLang(language);
        userRepository.save(user);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("language", language);
        return response;
    }

    @GetMapping("/categories")
    public Map<String, Object> getCategories(@CurrentUser User user) {
        Set<String> categories = user.getInterestedCategories() == null ? Set.of() : user.getInterestedCategories();
        List<String> categoryList = categories.stream().sorted().collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("categories", categoryList);
        return response;
    }

    @PostMapping("/categories")
    public Map<String, Object> updateCategories(@CurrentUser User user, @RequestBody CategoryRequest request) {
        Set<String> categories = request.categories() == null ? Set.of() : new java.util.HashSet<>(request.categories());
        user.setInterestedCategories(categories);
        userRepository.save(user);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("categories", categories.stream().sorted().collect(Collectors.toList()));
        return response;
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "en";
        }
        return switch (language.toLowerCase()) {
            case "ko", "en", "ja", "zh" -> language.toLowerCase();
            default -> "en";
        };
    }

    public record LanguageRequest(String language) {
    }

    public record CategoryRequest(List<String> categories) {
    }
}
