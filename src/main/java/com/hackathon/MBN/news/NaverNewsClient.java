package com.hackathon.MBN.news;

import com.hackathon.MBN.web.ApiException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class NaverNewsClient {

    static final String SOURCE_NAME = "Naver News";
    static final String API_BASE_URL = "https://naverapihub.apigw.ntruss.com";
    static final String NEWS_PATH = "/search/v1/news";
    static final String NEWS_ENDPOINT = API_BASE_URL + NEWS_PATH;
    static final String HEADER_CLIENT_ID = "X-NCP-APIGW-API-KEY-ID";
    static final String HEADER_CLIENT_SECRET = "X-NCP-APIGW-API-KEY";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;

    public NaverNewsClient(
            WebClient.Builder webClientBuilder,
            @Value("${naver.client-id:}") String clientId,
            @Value("${naver.client-secret:}") String clientSecret) {
        this.webClient = webClientBuilder.baseUrl(API_BASE_URL).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public List<NaverNewsItem> search(String query, int display) {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_CONFIG_MISSING", "Naver API keys are missing");
        }
        try {
            String body = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(NEWS_PATH)
                            .queryParam("query", query)
                            .queryParam("display", display)
                            .queryParam("start", 1)
                            .queryParam("sort", "date")
                            .queryParam("format", "json")
                            .build())
                    .header(HEADER_CLIENT_ID, clientId)
                    .header(HEADER_CLIENT_SECRET, clientSecret)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));
            return parseItems(body);
        } catch (WebClientResponseException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "NAVER_API_ERROR", "Naver API request failed");
        }
    }

    static List<NaverNewsItem> parseItems(String json) {
        try {
            JsonNode items = OBJECT_MAPPER.readTree(json).path("items");
            List<NaverNewsItem> result = new ArrayList<>();
            if (!items.isArray()) {
                return result;
            }
            for (JsonNode item : items) {
                String title = clean(item.path("title").asText());
                String url = firstText(item.path("originallink").asText(), item.path("link").asText());
                if (!StringUtils.hasText(title) || !StringUtils.hasText(url)) {
                    continue;
                }
                result.add(new NaverNewsItem(
                        title,
                        url,
                        clean(item.path("description").asText()),
                        parsePublishedAt(item.path("pubDate").asText())));
            }
            return result;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid Naver news response", ex);
        }
    }

    private static String clean(String value) {
        return HtmlUtils.htmlUnescape(value.replaceAll("<[^>]*>", "")).trim();
    }

    private static String firstText(String preferred, String fallback) {
        return StringUtils.hasText(preferred) ? preferred : fallback;
    }

    private static Instant parsePublishedAt(String value) {
        try {
            return StringUtils.hasText(value)
                    ? ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
                    : null;
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
