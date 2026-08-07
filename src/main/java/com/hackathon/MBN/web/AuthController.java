package com.hackathon.MBN.web;

import java.util.Map;
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
@RequestMapping
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;

    @PostMapping("/auth/device")
    public Map<String, Object> createDeviceToken() {
        String token = UUID.randomUUID().toString().replace("-", "");
        User user = userRepository.save(User.builder().deviceToken(token).build());
        return Map.of("token", user.getDeviceToken());
    }

    @GetMapping("/me/preferences")
    public Map<String, Object> getPreferences(@CurrentUser User user) {
        return Map.of(
                "preferredLang", user.getPreferredLang(),
                "pinTypeFilter", user.getPinTypeFilter(),
                "interestedArtistIds", user.getInterestedArtistIds(),
                "interestedCategories", user.getInterestedCategories());
    }

    @PutMapping("/me/preferences")
    public Map<String, Object> updatePreferences(@CurrentUser User user, @RequestBody PreferencesRequest request) {
        if (request.preferredLang() != null) {
            user.setPreferredLang(request.preferredLang());
        }
        if (request.pinTypeFilter() != null) {
            user.setPinTypeFilter(request.pinTypeFilter());
        }
        if (request.interestedArtistIds() != null) {
            user.setInterestedArtistIds(new java.util.HashSet<>(request.interestedArtistIds()));
        }
        if (request.interestedCategories() != null) {
            user.setInterestedCategories(new java.util.HashSet<>(request.interestedCategories()));
        }
        userRepository.save(user);
        return Map.of(
                "preferredLang", user.getPreferredLang(),
                "pinTypeFilter", user.getPinTypeFilter(),
                "interestedArtistIds", user.getInterestedArtistIds(),
                "interestedCategories", user.getInterestedCategories());
    }

    public record PreferencesRequest(
            String preferredLang,
            String pinTypeFilter,
            java.util.Set<Long> interestedArtistIds,
            java.util.Set<String> interestedCategories) {
    }
}
