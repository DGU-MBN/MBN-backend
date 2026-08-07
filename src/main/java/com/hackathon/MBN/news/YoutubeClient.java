package com.hackathon.MBN.news;

import com.hackathon.MBN.web.ApiException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class YoutubeClient {

    static final String API_BASE_URL = "https://www.googleapis.com/youtube/v3";
    static final String CHANNELS_PATH = "/channels";
    static final String PLAYLIST_ITEMS_PATH = "/playlistItems";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final String apiKey;

    public YoutubeClient(WebClient.Builder webClientBuilder, @Value("${youtube.api-key:}") String apiKey) {
        this.webClient = webClientBuilder.baseUrl(API_BASE_URL).build();
        this.apiKey = apiKey;
    }

    // channels.list(part=contentDetails) — 1 유닛. 채널당 한 번만 호출해도 되도록 호출측에서 캐시하는 걸 권장.
    public String resolveUploadsPlaylistId(String channelId) {
        requireApiKey();
        try {
            String body = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(CHANNELS_PATH)
                            .queryParam("part", "contentDetails")
                            .queryParam("id", channelId)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));
            return parseUploadsPlaylistId(body);
        } catch (WebClientResponseException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "YOUTUBE_API_ERROR", "YouTube channel lookup failed");
        }
    }

    // playlistItems.list(part=snippet) — 1 유닛. search.list(100 유닛)는 절대 쓰지 않는다.
    public List<YoutubeVideoItem> latestVideos(String playlistId, int maxResults) {
        requireApiKey();
        try {
            String body = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(PLAYLIST_ITEMS_PATH)
                            .queryParam("part", "snippet")
                            .queryParam("playlistId", playlistId)
                            .queryParam("maxResults", maxResults)
                            .queryParam("key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));
            return parseVideoItems(body);
        } catch (WebClientResponseException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "YOUTUBE_API_ERROR", "YouTube playlist lookup failed");
        }
    }

    private void requireApiKey() {
        if (!StringUtils.hasText(apiKey)) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "YOUTUBE_CONFIG_MISSING", "YouTube API key is missing");
        }
    }

    static String parseUploadsPlaylistId(String json) {
        try {
            JsonNode items = OBJECT_MAPPER.readTree(json).path("items");
            if (!items.isArray() || items.isEmpty()) {
                throw new IllegalArgumentException("YouTube channel not found");
            }
            String uploads = items.get(0)
                    .path("contentDetails").path("relatedPlaylists").path("uploads").asText();
            if (!StringUtils.hasText(uploads)) {
                throw new IllegalArgumentException("Channel has no uploads playlist");
            }
            return uploads;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid YouTube channels response", ex);
        }
    }

    static List<YoutubeVideoItem> parseVideoItems(String json) {
        try {
            JsonNode items = OBJECT_MAPPER.readTree(json).path("items");
            List<YoutubeVideoItem> result = new ArrayList<>();
            if (!items.isArray()) {
                return result;
            }
            for (JsonNode item : items) {
                JsonNode snippet = item.path("snippet");
                String videoId = snippet.path("resourceId").path("videoId").asText();
                String title = snippet.path("title").asText();
                if (!StringUtils.hasText(videoId) || !StringUtils.hasText(title)) {
                    continue;
                }
                result.add(new YoutubeVideoItem(
                        videoId,
                        title,
                        snippet.path("description").asText(),
                        "https://youtu.be/" + videoId,
                        parsePublishedAt(snippet.path("publishedAt").asText())));
            }
            return result;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid YouTube playlistItems response", ex);
        }
    }

    private static Instant parsePublishedAt(String value) {
        return StringUtils.hasText(value) ? Instant.parse(value) : null;
    }
}
