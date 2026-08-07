package com.hackathon.MBN.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hackathon.MBN.domain.Event;
import com.hackathon.MBN.domain.RawArticle;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiFactVerifierTest {

    @Test
    void parseVerificationMapsAllFacts() {
        String json = """
                {
                  "facts": [
                    { "fact_text": "부산에서 축제가 열렸다", "verified": true },
                    { "fact_text": "100만명이 방문했다", "verified": false }
                  ]
                }
                """;

        List<VerifiedFact> facts = AiFactVerifier.parseVerification(json);

        assertThat(facts).hasSize(2);
        assertThat(facts.get(0).factText()).isEqualTo("부산에서 축제가 열렸다");
        assertThat(facts.get(0).verified()).isTrue();
        assertThat(facts.get(1).factText()).isEqualTo("100만명이 방문했다");
        assertThat(facts.get(1).verified()).isFalse();
    }

    @Test
    void parseVerificationSkipsFactsWithoutText() {
        String json = """
                {
                  "facts": [
                    { "verified": true },
                    { "fact_text": "부산에서 축제가 열렸다", "verified": true }
                  ]
                }
                """;

        List<VerifiedFact> facts = AiFactVerifier.parseVerification(json);

        assertThat(facts).hasSize(1);
        assertThat(facts.get(0).factText()).isEqualTo("부산에서 축제가 열렸다");
    }

    @Test
    void parseVerificationThrowsWhenFactsArrayMissing() {
        assertThatThrownBy(() -> AiFactVerifier.parseVerification("{}"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseVerificationThrowsWhenNotJson() {
        assertThatThrownBy(() -> AiFactVerifier.parseVerification("not a json"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buildUserContentIncludesArticleAndExtractedSummary() {
        RawArticle article = RawArticle.builder()
                .title("부산 축제 개막")
                .description("검색 스니펫")
                .content("부산에서 대규모 축제가 열렸다")
                .build();
        Event event = Event.builder()
                .title("부산 축제 개막")
                .summary("부산에서 축제가 열려 10만명이 방문했다")
                .build();

        String userContent = AiFactVerifier.buildUserContent(article, event);

        assertThat(userContent).contains("원문 본문: 부산에서 대규모 축제가 열렸다");
        assertThat(userContent).contains("내용: 부산에서 축제가 열려 10만명이 방문했다");
        assertThat(userContent).doesNotContain("검색 스니펫");
    }

    @Test
    void extractContentReadsChatCompletionMessageContent() {
        String responseJson = """
                {
                  "choices": [
                    { "message": { "content": "{\\"facts\\":[]}" } }
                  ]
                }
                """;

        String content = AiFactVerifier.extractContent(responseJson);

        assertThat(content).isEqualTo("{\"facts\":[]}");
    }

    @Test
    void extractContentThrowsWhenMessageMissing() {
        assertThatThrownBy(() -> AiFactVerifier.extractContent("{\"choices\": []}"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
