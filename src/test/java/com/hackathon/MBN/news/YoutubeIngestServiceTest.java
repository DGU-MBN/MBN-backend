package com.hackathon.MBN.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.MBN.domain.RawArticle;
import com.hackathon.MBN.domain.Source;
import com.hackathon.MBN.domain.type.SourceType;
import com.hackathon.MBN.repository.RawArticleRepository;
import com.hackathon.MBN.repository.SourceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class YoutubeIngestServiceTest {

    @Mock
    YoutubeClient client;

    @Mock
    RawArticleRepository rawArticles;

    @Mock
    SourceRepository sources;

    @Test
    void createsChannelSourceAndSavesNewVideos() {
        var video = new YoutubeVideoItem("abc123", "New MV", "설명", "https://youtu.be/abc123",
                Instant.parse("2026-08-07T01:15:00Z"));
        when(sources.findByName("YouTube:UCabc")).thenReturn(Optional.empty());
        when(sources.save(any(Source.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(client.resolveUploadsPlaylistId("UCabc")).thenReturn("UUabc");
        when(client.latestVideos("UUabc", 10)).thenReturn(List.of(video));
        when(rawArticles.existsByContentHash(anyString())).thenReturn(false);

        var result = new YoutubeIngestService(client, rawArticles, sources).ingest("UCabc", 10);

        assertThat(result.fetched()).isEqualTo(1);
        assertThat(result.saved()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(0);

        ArgumentCaptor<Source> sourceCaptor = ArgumentCaptor.forClass(Source.class);
        verify(sources).save(sourceCaptor.capture());
        assertThat(sourceCaptor.getValue().getSourceType()).isEqualTo(SourceType.YOUTUBE_OFFICIAL);

        ArgumentCaptor<RawArticle> articleCaptor = ArgumentCaptor.forClass(RawArticle.class);
        verify(rawArticles).save(articleCaptor.capture());
        assertThat(articleCaptor.getValue().getTitle()).isEqualTo("New MV");
        assertThat(articleCaptor.getValue().getContent()).isEqualTo("설명");
        assertThat(articleCaptor.getValue().getUrl()).isEqualTo("https://youtu.be/abc123");
    }

    @Test
    void skipsDuplicateVideos() {
        var video = new YoutubeVideoItem("abc123", "New MV", "설명", "https://youtu.be/abc123",
                Instant.parse("2026-08-07T01:15:00Z"));
        Source existing = Source.builder().sourceType(SourceType.YOUTUBE_OFFICIAL).name("YouTube:UCabc")
                .endpoint("https://www.youtube.com/channel/UCabc").build();
        when(sources.findByName("YouTube:UCabc")).thenReturn(Optional.of(existing));
        when(client.resolveUploadsPlaylistId("UCabc")).thenReturn("UUabc");
        when(client.latestVideos("UUabc", 10)).thenReturn(List.of(video));
        when(rawArticles.existsByContentHash(anyString())).thenReturn(true);

        var result = new YoutubeIngestService(client, rawArticles, sources).ingest("UCabc", 10);

        assertThat(result.saved()).isEqualTo(0);
        assertThat(result.skipped()).isEqualTo(1);
    }
}
