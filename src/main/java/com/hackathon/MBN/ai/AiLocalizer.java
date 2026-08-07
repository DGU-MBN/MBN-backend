package com.hackathon.MBN.ai;

import com.hackathon.MBN.domain.Event;
import com.hackathon.MBN.web.ApiException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
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

/** 사건 팩트를 언어별로 "번역"이 아니라 그 언어 기자가 새로 쓴 것처럼 재작성한다 */
@Component
public class AiLocalizer {

    static final String API_BASE_URL = "https://api.openai.com";
    static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";

    static final String SYSTEM_PROMPT_TEMPLATE = """
            너는 번역가가 아니라 %s 현지 뉴스 매체의 기자다.
            아래로 주어지는 한국어 기사 정보(제목/본문 전문/근거)를 참고해서, 처음부터 %s로
            취재해서 쓴 것처럼 자연스러운 현지 뉴스 기사 전문을 새로 작성한다.
            문장을 그대로 옮기지 말고(직역이나 번역체 금지), 요약하지 말고 원문의 정보량을
            살려서 기사 전체를 재작성하라.
            설명, 코드블록, 다른 텍스트 없이 아래 JSON 스키마로만 응답한다.

            응답 JSON 형식:
            {
              "title": "%s로 자연스럽게 쓴 제목",
              "body": "%s로 자연스럽게 다시 쓴 기사 전문"
            }
            """;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public AiLocalizer(
            WebClient.Builder webClientBuilder,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gpt-4o-mini}") String model) {
        this.webClient = webClientBuilder.baseUrl(API_BASE_URL).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public LocalizedContent localize(Event event, String languageName) {
        if (!StringUtils.hasText(apiKey)) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "OPENAI_CONFIG_MISSING", "OpenAI API key is missing");
        }
        String systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(languageName, languageName, languageName, languageName);
        String userContent = "사실관계 정보:\n"
                + "제목: " + event.getTitle() + "\n"
                + "카테고리: " + event.getCategory() + "\n"
                + "장소: " + emptyIfNull(event.getLocationName()) + "\n"
                + "내용: " + emptyIfNull(event.getSummary()) + "\n"
                + "근거: " + emptyIfNull(event.getEvidence());
        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.4,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
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
        return parseLocalizedContent(extractContent(response));
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

    static LocalizedContent parseLocalizedContent(String json) {
        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid localization JSON", ex);
        }
        String title = textOrNull(root, "title");
        String body = textOrNull(root, "body");
        if (!StringUtils.hasText(title)) {
            throw new IllegalArgumentException("Localization response missing title");
        }
        return new LocalizedContent(title, body);
    }

    private static String textOrNull(JsonNode root, String field) {
        JsonNode node = root.path(field);
        return node.isString() ? node.asText() : null;
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
