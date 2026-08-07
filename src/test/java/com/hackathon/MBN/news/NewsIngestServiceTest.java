package com.hackathon.MBN.news;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.MBN.domain.RawArticle;
import com.hackathon.MBN.domain.Source;
import com.hackathon.MBN.domain.type.PinType;
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
class NewsIngestServiceTest {

    @Mock
    NaverNewsClient client;

    @Mock
    RawArticleRepository rawArticles;

    @Mock
    SourceRepository sources;

    @Mock
    ArticleBodyFetcher bodyFetcher;

    @Test
    void savesNewItemsAndSkipsDuplicateHashes() {
        Source source = Source.builder()
                .sourceType(SourceType.NEWS_RSS)
                .name("Naver News")
                .endpoint("https://openapi.naver.com/v1/search/news.json")
                .mapsToPinType(PinType.ORIGIN)
                .build();
        var duplicate = new NaverNewsItem("old", "https://example.com/old", "old desc", Instant.parse("2026-08-07T00:00:00Z"));
        var fresh = new NaverNewsItem("fresh", "https://example.com/fresh", "fresh desc", Instant.parse("2026-08-07T01:00:00Z"));

        when(sources.findByName("Naver News")).thenReturn(Optional.of(source));
        when(client.search("MBN", 2)).thenReturn(List.of(duplicate, fresh));
        when(rawArticles.existsByContentHash(NewsIngestService.contentHash(duplicate))).thenReturn(true);
        when(rawArticles.existsByContentHash(NewsIngestService.contentHash(fresh))).thenReturn(false);

        var result = new NewsIngestService(client, rawArticles, sources, bodyFetcher).ingest("MBN", 2);

        assertThat(result.saved()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);

        ArgumentCaptor<RawArticle> captor = ArgumentCaptor.forClass(RawArticle.class);
        verify(rawArticles).save(captor.capture());
        assertThat(captor.getValue().getSource()).isSameAs(source);
        assertThat(captor.getValue().getTitle()).isEqualTo("fresh");
    }
}
