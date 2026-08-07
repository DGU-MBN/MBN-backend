package com.hackathon.MBN.news;

import com.hackathon.MBN.web.ApiException;
import java.time.Duration;
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
public class NaverCafeClient {

    static final String SOURCE_NAME = "Naver Cafe";
    static final String API_BASE_URL = "https://openapi.naver.com";
    static final String CAFE_PATH = "/v1/search/cafearticle.json";
    static final String CAFE_ENDPOINT = API_BASE_URL + CAFE_PATH;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;

    public NaverCafeClient(
            WebClient.Builder webClientBuilder,
            @Value("${naver.open-client-id:}") String clientId,
            @Value("${naver.open-client-secret:}") String clientSecret) {
        this.webClient = webClientBuilder.baseUrl(API_BASE_URL).build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public List<NaverCafeItem> search(String query, int display) {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "NAVER_OPEN_CONFIG_MISSING", "Naver Open API keys are missing");
        }
        try {
            String body = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(CAFE_PATH)
                            .queryParam("query", query)
                            .queryParam("display", display)
                            .queryParam("start", 1)
                            .build())
                    .header(NaverBlogClient.HEADER_CLIENT_ID, clientId)
                    .header(NaverBlogClient.HEADER_CLIENT_SECRET, clientSecret)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(10));
            return parseItems(body);
        } catch (WebClientResponseException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "NAVER_OPEN_API_ERROR", "Naver Cafe search failed");
        }
    }

    static List<NaverCafeItem> parseItems(String json) {
        try {
            JsonNode items = OBJECT_MAPPER.readTree(json).path("items");
            List<NaverCafeItem> result = new ArrayList<>();
            if (!items.isArray()) {
                return result;
            }
            for (JsonNode item : items) {
                String title = clean(item.path("title").asText());
                String url = item.path("link").asText();
                if (!StringUtils.hasText(title) || !StringUtils.hasText(url)) {
                    continue;
                }
                result.add(new NaverCafeItem(title, url, clean(item.path("description").asText()), null));
            }
            return result;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid Naver Cafe response", ex);
        }
    }

    private static String clean(String value) {
        return HtmlUtils.htmlUnescape(value.replaceAll("<[^>]*>", "")).trim();
    }
}
