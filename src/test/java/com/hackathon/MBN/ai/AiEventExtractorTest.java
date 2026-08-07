package com.hackathon.MBN.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hackathon.MBN.domain.type.AiConfidence;
import com.hackathon.MBN.domain.type.LocationPrecision;
import org.junit.jupiter.api.Test;

class AiEventExtractorTest {

    @Test
    void parseExtractionMapsAllFields() {
        String json = """
                {
                  "event_title": "강남 오피스텔 화재",
                  "location_name": "서울 강남구 역삼동",
                  "admin_area": "서울특별시 강남구",
                  "lat": 37.5006,
                  "lng": 127.0365,
                  "location_precision": "VENUE",
                  "category": "사고재난",
                  "confidence": "HIGH",
                  "evidence": "역삼동의 한 오피스텔 3층에서 화재가 발생했다"
                }
                """;

        ArticleExtraction extraction = AiEventExtractor.parseExtraction(json);

        assertThat(extraction.eventTitle()).isEqualTo("강남 오피스텔 화재");
        assertThat(extraction.locationName()).isEqualTo("서울 강남구 역삼동");
        assertThat(extraction.adminArea()).isEqualTo("서울특별시 강남구");
        assertThat(extraction.lat()).isEqualTo(37.5006);
        assertThat(extraction.lng()).isEqualTo(127.0365);
        assertThat(extraction.locationPrecision()).isEqualTo(LocationPrecision.VENUE);
        assertThat(extraction.category()).isEqualTo("사고재난");
        assertThat(extraction.confidence()).isEqualTo(AiConfidence.HIGH);
        assertThat(extraction.evidence()).isEqualTo("역삼동의 한 오피스텔 3층에서 화재가 발생했다");
    }

    @Test
    void parseExtractionAllowsNullLocationName() {
        String json = """
                {
                  "event_title": "전국 소비자물가 상승",
                  "location_name": null,
                  "admin_area": null,
                  "lat": null,
                  "lng": null,
                  "location_precision": null,
                  "category": "경제산업",
                  "confidence": "MEDIUM",
                  "evidence": "통계청 발표에 따르면"
                }
                """;

        ArticleExtraction extraction = AiEventExtractor.parseExtraction(json);

        assertThat(extraction.locationName()).isNull();
        assertThat(extraction.adminArea()).isNull();
        assertThat(extraction.lat()).isNull();
        assertThat(extraction.lng()).isNull();
        assertThat(extraction.locationPrecision()).isNull();
    }

    @Test
    void parseExtractionThrowsWhenRequiredFieldMissing() {
        String json = """
                {
                  "summary": "제목이 없는 응답",
                  "location_name": "서울",
                  "category": "사회일반",
                  "confidence": "LOW"
                }
                """;

        assertThatThrownBy(() -> AiEventExtractor.parseExtraction(json))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseExtractionThrowsWhenNotJson() {
        assertThatThrownBy(() -> AiEventExtractor.parseExtraction("not a json"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseExtractionThrowsWhenConfidenceUnknown() {
        String json = """
                {
                  "event_title": "제목",
                  "category": "기타",
                  "confidence": "SUPER_HIGH"
                }
                """;

        assertThatThrownBy(() -> AiEventExtractor.parseExtraction(json))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void extractContentReadsChatCompletionMessageContent() {
        String responseJson = """
                {
                  "choices": [
                    { "message": { "content": "{\\"event_title\\":\\"제목\\"}" } }
                  ]
                }
                """;

        String content = AiEventExtractor.extractContent(responseJson);

        assertThat(content).isEqualTo("{\"event_title\":\"제목\"}");
    }

    @Test
    void extractContentThrowsWhenMessageMissing() {
        assertThatThrownBy(() -> AiEventExtractor.extractContent("{\"choices\": []}"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
