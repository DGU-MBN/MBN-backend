package com.hackathon.MBN.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.MBN.domain.Interaction;
import com.hackathon.MBN.domain.Short;
import com.hackathon.MBN.domain.User;
import com.hackathon.MBN.domain.type.InteractionType;
import com.hackathon.MBN.repository.InteractionRepository;
import com.hackathon.MBN.repository.ShortRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class InteractionController {

    private final ShortRepository shortRepository;
    private final InteractionRepository interactionRepository;

    @PostMapping("/shorts/{shortId}/interactions")
    public ResponseEntity<Map<String, Object>> createInteraction(
            @PathVariable Long shortId,
            @CurrentUser User user,
            @RequestBody InteractionRequest request) {
        Short shortVideo = shortRepository.findById(shortId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SHORT_NOT_FOUND", "Short not found: " + shortId));

        InteractionType type = InteractionType.valueOf(request.type().toUpperCase());
        Interaction interaction = Interaction.builder()
                .shortVideo(shortVideo)
                .user(user)
                .type(type)
                .watchRatio(request.watchRatio())
                .countryCode(request.countryCode())
                .build();

        interactionRepository.save(interaction);

        return ResponseEntity.accepted().body(Map.of(
                "status", "accepted",
                "shortId", shortId,
                "interactionType", type.name()));
    }

    public record InteractionRequest(
            String type,
            Double watchRatio,
            String countryCode) {
    }
}
