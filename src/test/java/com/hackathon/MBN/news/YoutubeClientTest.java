package com.hackathon.MBN.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class YoutubeClientTest {

    @Test
    void usesYoutubeDataApiV3BaseUrl() {
        assertThat(YoutubeClient.API_BASE_URL).isEqualTo("https://www.googleapis.com/youtube/v3");
    }

    @Test
    void parseUploadsPlaylistIdReadsRelatedPlaylists() {
        String json = """
                {
                  "items": [
                    { "id": "UCabc", "contentDetails": { "relatedPlaylists": { "uploads": "UUabc" } } }
                  ]
                }
                """;

        assertThat(YoutubeClient.parseUploadsPlaylistId(json)).isEqualTo("UUabc");
    }

    @Test
    void parseUploadsPlaylistIdThrowsWhenChannelNotFound() {
        assertThatThrownBy(() -> YoutubeClient.parseUploadsPlaylistId("{\"items\": []}"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseVideoItemsMapsSnippetFields() {
        String json = """
                {
                  "items": [
                    {
                      "snippet": {
                        "title": "NewJeans 'New Single' Official MV",
                        "description": "새 싱글 공식 뮤직비디오",
                        "publishedAt": "2026-08-07T01:15:00Z",
                        "resourceId": { "videoId": "abc123" }
                      }
                    }
                  ]
                }
                """;

        var items = YoutubeClient.parseVideoItems(json);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).videoId()).isEqualTo("abc123");
        assertThat(items.get(0).title()).isEqualTo("NewJeans 'New Single' Official MV");
        assertThat(items.get(0).description()).isEqualTo("새 싱글 공식 뮤직비디오");
        assertThat(items.get(0).url()).isEqualTo("https://youtu.be/abc123");
        assertThat(items.get(0).publishedAt()).isEqualTo(Instant.parse("2026-08-07T01:15:00Z"));
    }
}
