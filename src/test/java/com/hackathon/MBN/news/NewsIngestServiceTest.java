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

    @Test
    void ingestByCategorySearchesEveryKeywordForThatCategory() {
        Source source = Source.builder()
                .sourceType(SourceType.NEWS_RSS)
                .name("Naver News")
                .endpoint("https://openapi.naver.com/v1/search/news.json")
                .mapsToPinType(PinType.ORIGIN)
                .build();
        when(sources.findByName("Naver News")).thenReturn(Optional.of(source));

        // 식품 카테고리의 대표 검색어 3개("식품","먹거리","외식 트렌드")마다 신규 기사 1건씩
        var a = new NaverNewsItem("식품A", "https://example.com/a", "d", Instant.parse("2026-08-08T00:00:00Z"));
        var b = new NaverNewsItem("먹거리B", "https://example.com/b", "d", Instant.parse("2026-08-08T00:00:00Z"));
        var c = new NaverNewsItem("외식C", "https://example.com/c", "d", Instant.parse("2026-08-08T00:00:00Z"));
        when(client.search("식품", 5)).thenReturn(List.of(a));
        when(client.search("먹거리", 5)).thenReturn(List.of(b));
        when(client.search("외식 트렌드", 5)).thenReturn(List.of(c));
        when(rawArticles.existsByContentHash(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);

        var result = new NewsIngestService(client, rawArticles, sources)
                .ingestByCategory(com.hackathon.MBN.domain.type.NewsCategory.식품, 5);

        assertThat(result.fetched()).isEqualTo(3);
        assertThat(result.saved()).isEqualTo(3);
        verify(client).search("식품", 5);
        verify(client).search("먹거리", 5);
        verify(client).search("외식 트렌드", 5);
    }
}
