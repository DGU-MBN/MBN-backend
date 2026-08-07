package com.hackathon.MBN.ai;

import com.hackathon.MBN.domain.Event;
import com.hackathon.MBN.domain.RawArticle;
import com.hackathon.MBN.web.ApiException;
import java.time.Duration;
import java.util.ArrayList;
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

/** 1차 추출(AiEventExtractor) 결과가 원문에 실제로 근거하는지 별도 호출로 감사(audit)한다 */
@Component
public class AiFactVerifier {

    static final String API_BASE_URL = "https://api.openai.com";
    static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";

    static final String SYSTEM_PROMPT = """
            너는 재구성된 사건 정보가 원문 기사에 실제로 근거하는지 검증하는 팩트체커다.
            아래 원문 기사와, 그 기사에서 추출된 사건 요약을 비교해서, 요약에 담긴 핵심 주장을
            2~5개의 개별 사실(fact)로 쪼갠 뒤 각각이 원문에 직접적인 근거가 있는지 판정한다.
            원문에 없는 내용을 지어냈거나 과장했다면 verified를 false로 표시한다.
            설명, 코드블록, 다른 텍스트 없이 아래 JSON 스키마로만 응답한다.

            응답 JSON 형식:
            {
              "facts": [
                { "fact_text": "사실 문장", "verified": true }
              ]
            }
            """;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    public AiFactVerifier(
            WebClient.Builder webClientBuilder,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gpt-4o-mini}") String model) {
        this.webClient = webClientBuilder.baseUrl(API_BASE_URL).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    public List<VerifiedFact> verify(RawArticle article, Event event) {
        if (!StringUtils.hasText(apiKey)) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "OPENAI_CONFIG_MISSING", "OpenAI API key is missing");
        }
        String userContent = buildUserContent(article, event);
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
        return parseVerification(extractContent(response));
    }

    static String buildUserContent(RawArticle article, Event event) {
        return "원문 제목: " + article.getTitle() + "\n"
                + "원문 본문: " + articleText(article) + "\n\n"
                + "추출된 사건 요약:\n"
                + "제목: " + event.getTitle() + "\n"
                + "내용: " + emptyIfNull(event.getSummary());
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

    static List<VerifiedFact> parseVerification(String json) {
        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(json);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid verification JSON", ex);
        }
        JsonNode factsNode = root.path("facts");
        if (!factsNode.isArray()) {
            throw new IllegalArgumentException("Verification response missing facts array");
        }
        List<VerifiedFact> facts = new ArrayList<>();
        for (JsonNode factNode : factsNode) {
            JsonNode textNode = factNode.path("fact_text");
            if (!textNode.isString() || !StringUtils.hasText(textNode.asText())) {
                continue;
            }
            facts.add(new VerifiedFact(textNode.asText(), factNode.path("verified").asBoolean(false)));
        }
        return facts;
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private static String articleText(RawArticle article) {
        return StringUtils.hasText(article.getContent()) ? article.getContent() : emptyIfNull(article.getDescription());
    }
}
