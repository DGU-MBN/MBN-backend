package com.hackathon.MBN.news;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class NaverNewsClientTest {

    @Test
    void usesNaverApiHubEndpointAndHeaders() {
        assertThat(NaverNewsClient.API_BASE_URL).isEqualTo("https://naverapihub.apigw.ntruss.com");
        assertThat(NaverNewsClient.NEWS_PATH).isEqualTo("/search/v1/news");
        assertThat(NaverNewsClient.HEADER_CLIENT_ID).isEqualTo("X-NCP-APIGW-API-KEY-ID");
        assertThat(NaverNewsClient.HEADER_CLIENT_SECRET).isEqualTo("X-NCP-APIGW-API-KEY");
    }

    @Test
    void parseResponseMapsNaverNewsItems() {
        String json = """
                {
                  "items": [
                    {
                      "title": "<b>MBN</b> title",
                      "originallink": "https://example.com/original",
                      "link": "https://n.news.naver.com/article/001/0000000001",
                      "description": "desc &amp; more",
                      "pubDate": "Fri, 07 Aug 2026 10:15:00 +0900"
                    }
                  ]
                }
                """;

        var items = NaverNewsClient.parseItems(json);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).title()).isEqualTo("MBN title");
        assertThat(items.get(0).url()).isEqualTo("https://example.com/original");
        assertThat(items.get(0).description()).isEqualTo("desc & more");
        assertThat(items.get(0).publishedAt()).isEqualTo(Instant.parse("2026-08-07T01:15:00Z"));
    }
}
