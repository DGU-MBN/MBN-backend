package com.hackathon.MBN.web;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.MBN.domain.User;
import com.hackathon.MBN.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    /** 기능명세서 01: 지원하지 않는 언어는 영어로 대체 */
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of("ko", "en", "ja", "zh");

    private final UserRepository userRepository;

    @PostMapping("/auth/device")
    public Map<String, Object> createDeviceToken() {
        String token = UUID.randomUUID().toString().replace("-", "");
        User user = userRepository.save(User.builder().deviceToken(token).build());
        return Map.of("token", user.getDeviceToken());
    }

    @GetMapping("/me/preferences")
    public Map<String, Object> getPreferences(@CurrentUser User user) {
        return toResponse(user);
    }

    @PutMapping("/me/preferences")
    public Map<String, Object> updatePreferences(@CurrentUser User user, @RequestBody PreferencesRequest request) {
        if (request.preferredLang() != null) {
            user.setPreferredLang(normalizeLanguage(request.preferredLang()));
        }
        if (request.pinTypeFilter() != null) {
            user.setPinTypeFilter(request.pinTypeFilter());
        }
        if (request.interestedArtistIds() != null) {
            user.setInterestedArtistIds(new HashSet<>(request.interestedArtistIds()));
        }
        if (request.interestedCategories() != null) {
            user.setInterestedCategories(new HashSet<>(request.interestedCategories()));
        }
        userRepository.save(user);
        return toResponse(user);
    }

    private Map<String, Object> toResponse(User user) {
        // Map.of는 null 값을 허용하지 않아 pinTypeFilter처럼 비어있는 필드에서 터진다.
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("preferredLang", normalizeLanguage(user.getPreferredLang()));
        response.put("pinTypeFilter", user.getPinTypeFilter());
        response.put("interestedArtistIds", user.getInterestedArtistIds());
        response.put("interestedCategories", user.getInterestedCategories());
        return response;
    }

    private String normalizeLanguage(String language) {
        if (language == null || !SUPPORTED_LANGUAGES.contains(language.toLowerCase())) {
            return "en";
        }
        return language.toLowerCase();
    }

    public record PreferencesRequest(
            String preferredLang,
            String pinTypeFilter,
            Set<Long> interestedArtistIds,
            Set<String> interestedCategories) {
    }
}
