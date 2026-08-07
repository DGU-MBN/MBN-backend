package com.hackathon.MBN.news;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NaverCafeClientTest {

    @Test
    void usesCafeArticleEndpoint() {
        assertThat(NaverCafeClient.API_BASE_URL).isEqualTo("https://openapi.naver.com");
        assertThat(NaverCafeClient.CAFE_PATH).isEqualTo("/v1/search/cafearticle.json");
    }

    @Test
    void parseItemsMapsTitleAndUrlWithNullPublishedAt() {
        String json = """
                {
                  "items": [
                    {
                      "title": "<b>뉴진스</b> 콘서트 후기 모음",
                      "link": "https://cafe.naver.com/example/1",
                      "description": "후기 &amp; 사진 공유"
                    }
                  ]
                }
                """;

        var items = NaverCafeClient.parseItems(json);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).title()).isEqualTo("뉴진스 콘서트 후기 모음");
        assertThat(items.get(0).url()).isEqualTo("https://cafe.naver.com/example/1");
        assertThat(items.get(0).description()).isEqualTo("후기 & 사진 공유");
        assertThat(items.get(0).publishedAt()).isNull();
    }
}
