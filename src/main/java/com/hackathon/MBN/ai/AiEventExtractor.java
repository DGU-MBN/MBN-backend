package com.hackathon.MBN.ai;

import com.hackathon.MBN.domain.RawArticle;
import com.hackathon.MBN.domain.type.AiConfidence;
import com.hackathon.MBN.domain.type.LocationPrecision;
import com.hackathon.MBN.domain.type.NewsCategory;
import com.hackathon.MBN.web.ApiException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class AiEventExtractor {

    static final String API_BASE_URL = "https://api.openai.com";
    static final String
            CHAT_COMPLETIONS_PATH = "/v1/chat/completions";

    /** 카테고리는 NewsCategory enum이 유일한 출처. 여기에 넣으면 프롬프트에 자동 반영된다. */
    private static final String CATEGORY_LIST = Arrays.stream(NewsCategory.values())
            .map(Enum::name)
            .collect(Collectors.joining(", "));

    static final String SYSTEM_PROMPT = """
            너는 뉴스 기사에서 지도 표시용 사건 정보를 추출하는 엔진이다.
            기사 제목과 본문을 읽고 아래 JSON 스키마로만 응답한다. 설명, 코드블록, 다른 텍스트는 절대 포함하지 않는다.

            카테고리는 반드시 다음 중 하나여야 한다: %s

            응답 JSON 형식:""".formatted(CATEGORY_LIST) + """

            {
              "event_title": "사건을 한 문장으로 요약한 제목",
              "location_name": "사건이 발생한 구체적 장소명 (예: 서울 강남구 역삼동). 특정할 수 없으면 null",
              "admin_area": "행정구역명 (예: 서울특별시 강남구). 특정할 수 없으면 null",
              "lat": "location_name의 위도 (숫자). 특정할 수 없으면 null",
              "lng": "location_name의 경도 (숫자). 특정할 수 없으면 null",
              "location_precision": "VENUE | CITY | COUNTRY 중 하나 (장소를 특정할 수 없으면 null)",
              "category": "위 카테고리 중 하나",
              "confidence": "HIGH | MEDIUM | LOW",
              "evidence": "이렇게 판단한 근거가 되는 기사 속 문장 또는 표현"
            }
            """;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public AiEventExtractor(
            WebClient.Builder webClientBuilder,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gpt-4o-mini}") String model) {
        this.webClient = webClientBuilder.baseUrl(API_BASE_URL).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public ArticleExtraction extract(RawArticle article) {
        if (!StringUtils.hasText(apiKey)) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "OPENAI_CONFIG_MISSING", "OpenAI API key is missing");
        }
        String userContent = buildUserContent(article);
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userContent)));
        String response;
        try {
            response = webClient.post()
                    .uri(CHAT_COMPLETIONS_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(20));
        } catch (WebClientResponseException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "OPENAI_API_ERROR", "OpenAI API request failed");
        }
        return parseExtraction(extractContent(response));
    }

    static String extractContent(String responseJson) {
        try {
            JsonNode content = OBJECT_MAPPER.readTree(responseJson)
                    .path("choices").path(0).path("message").path("content");
            if (!content.isString()) {
                throw new IllegalArgumentException("OpenAI response has no message content");
            }
            return content.asText();
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid OpenAI response", ex);
        }
    }

    static ArticleExtraction parseExtraction(String json) {
        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid AI extraction JSON", ex);
        }
        String eventTitle = textOrNull(root, "event_title");
        String category = textOrNull(root, "category");
        String confidenceRaw = textOrNull(root, "confidence");
        if (!StringUtils.hasText(eventTitle) || !StringUtils.hasText(category) || !StringUtils.hasText(confidenceRaw)) {
            throw new IllegalArgumentException("AI extraction response missing required fields");
        }
        AiConfidence confidence;
        try {
            confidence = AiConfidence.valueOf(confidenceRaw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown confidence value: " + confidenceRaw, ex);
        }
        return new ArticleExtraction(
                eventTitle,
                textOrNull(root, "location_name"),
                textOrNull(root, "admin_area"),
                doubleOrNull(root, "lat"),
                doubleOrNull(root, "lng"),
                locationPrecisionOrNull(root),
                category,
                confidence,
                textOrNull(root, "evidence"));
    }

    static String buildUserContent(RawArticle article) {
        return "제목: " + article.getTitle() + "\n본문: " + articleText(article);
    }

    private static String textOrNull(JsonNode root, String field) {
        JsonNode node = root.path(field);
        return node.isString() ? node.asText() : null;
    }

    private static Double doubleOrNull(JsonNode root, String field) {
        JsonNode node = root.path(field);
        return node.isNumber() ? node.asDouble() : null;
    }

    private static LocationPrecision locationPrecisionOrNull(JsonNode root) {
        String raw = textOrNull(root, "location_precision");
        if (raw == null) {
            return null;
        }
        try {
            return LocationPrecision.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private static String articleText(RawArticle article) {
        return StringUtils.hasText(article.getContent()) ? article.getContent() : emptyIfNull(article.getDescription());
    }
}
