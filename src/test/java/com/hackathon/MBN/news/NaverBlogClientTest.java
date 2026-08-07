package com.hackathon.MBN.news;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class NaverBlogClientTest {

    @Test
    void usesNaverOpenApiEndpointAndHeaders() {
        assertThat(NaverBlogClient.API_BASE_URL).isEqualTo("https://openapi.naver.com");
        assertThat(NaverBlogClient.BLOG_PATH).isEqualTo("/v1/search/blog.json");
        assertThat(NaverBlogClient.HEADER_CLIENT_ID).isEqualTo("X-Naver-Client-Id");
        assertThat(NaverBlogClient.HEADER_CLIENT_SECRET).isEqualTo("X-Naver-Client-Secret");
    }

    @Test
    void parseItemsMapsTitleUrlDescriptionAndPostDate() {
        String json = """
                {
                  "items": [
                    {
                      "title": "<b>아이유</b> 콘서트 후기",
                      "link": "https://blog.naver.com/example/1",
                      "description": "어제 다녀온 후기 &amp; 사진",
                      "postdate": "20260807"
                    }
                  ]
                }
                """;

        var items = NaverBlogClient.parseItems(json);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).title()).isEqualTo("아이유 콘서트 후기");
        assertThat(items.get(0).url()).isEqualTo("https://blog.naver.com/example/1");
        assertThat(items.get(0).description()).isEqualTo("어제 다녀온 후기 & 사진");
        assertThat(items.get(0).publishedAt()).isEqualTo(Instant.parse("2026-08-06T15:00:00Z"));
    }

    @Test
    void parseItemsSkipsEntriesMissingTitleOrUrl() {
        String json = """
                {
                  "items": [
                    { "title": "", "link": "https://blog.naver.com/example/2", "description": "d", "postdate": "20260807" }
                  ]
                }
                """;

        assertThat(NaverBlogClient.parseItems(json)).isEmpty();
    }
}
