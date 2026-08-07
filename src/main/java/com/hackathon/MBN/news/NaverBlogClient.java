package com.hackathon.MBN.news;

import com.hackathon.MBN.web.ApiException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
public class NaverBlogClient {

    static final String SOURCE_NAME = "Naver Blog";
    static final String API_BASE_URL = "https://openapi.naver.com";
    static final String BLOG_PATH = "/v1/search/blog.json";
    static final String BLOG_ENDPOINT = API_BASE_URL + BLOG_PATH;
    static final String HEADER_CLIENT_ID = "X-Naver-Client-Id";
    static final String HEADER_CLIENT_SECRET = "X-Naver-Client-Secret";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;

    public NaverBlogClient(
            WebClient.Builder webClientBuilder,
            @Value("${naver.open-client-id:}") String clientId,
            @Value("${naver.open-client-secret:}") String clientSecret) {
        this.webClient = webClientBuilder.baseUrl(API_BASE_URL).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public List<NaverBlogItem> search(String query, int display) {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_OPEN_CONFIG_MISSING", "Naver Open API keys are missing");
        }
        try {
            String body = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(BLOG_PATH)
                            .queryParam("query", query)
                            .queryParam("display", display)
                            .queryParam("start", 1)
                            .queryParam("sort", "date")
                            .build())
                    .header(HEADER_CLIENT_ID, clientId)
                    .header(HEADER_CLIENT_SECRET, clientSecret)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));
            return parseItems(body);
        } catch (WebClientResponseException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "NAVER_OPEN_API_ERROR", "Naver Blog search failed");
        }
    }

    static List<NaverBlogItem> parseItems(String json) {
        try {
            JsonNode items = OBJECT_MAPPER.readTree(json).path("items");
            List<NaverBlogItem> result = new ArrayList<>();
            if (!items.isArray()) {
                return result;
            }
            for (JsonNode item : items) {
                String title = clean(item.path("title").asText());
                String url = item.path("link").asText();
                if (!StringUtils.hasText(title) || !StringUtils.hasText(url)) {
                    continue;
                }
                result.add(new NaverBlogItem(
                        title,
                        url,
                        clean(item.path("description").asText()),
                        parsePublishedAt(item.path("postdate").asText())));
            }
            return result;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid Naver Blog response", ex);
        }
    }

    private static String clean(String value) {
        return HtmlUtils.htmlUnescape(value.replaceAll("<[^>]*>", "")).trim();
    }

    private static Instant parsePublishedAt(String value) {
        try {
            return StringUtils.hasText(value)
                    ? LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE)
                            .atStartOfDay(ZoneId.of("Asia/Seoul"))
                            .toInstant()
                    : null;
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
