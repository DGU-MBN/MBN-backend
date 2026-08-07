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
class CommunityIngestServiceTest {

    @Mock
    RawArticleRepository rawArticles;

    @Mock
    SourceRepository sources;

    @Mock
    ArticleBodyFetcher bodyFetcher;

    @Test
    void createsSourceOnFirstUseAndSavesNewItems() {
        var fresh = new NaverBlogItem("아이유 콘서트", "https://blog.naver.com/1", "설명", Instant.parse("2026-08-07T00:00:00Z"));
        when(sources.findByName("Naver Blog")).thenReturn(Optional.empty());
        when(sources.save(org.mockito.ArgumentMatchers.any(Source.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(rawArticles.existsByContentHash(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);

        var service = new CommunityIngestService(rawArticles, sources, bodyFetcher);
        var result = service.ingest(
                (query, display) -> List.of(fresh),
                SourceType.NAVER_BLOG, "Naver Blog", "https://openapi.naver.com/v1/search/blog.json",
                "아이유", 10);

        assertThat(result.fetched()).isEqualTo(1);
        assertThat(result.saved()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(0);

        ArgumentCaptor<Source> sourceCaptor = ArgumentCaptor.forClass(Source.class);
        verify(sources).save(sourceCaptor.capture());
        assertThat(sourceCaptor.getValue().getSourceType()).isEqualTo(SourceType.NAVER_BLOG);
        assertThat(sourceCaptor.getValue().getMapsToPinType()).isEqualTo(PinType.ORIGIN);

        ArgumentCaptor<RawArticle> articleCaptor = ArgumentCaptor.forClass(RawArticle.class);
        verify(rawArticles).save(articleCaptor.capture());
        assertThat(articleCaptor.getValue().getTitle()).isEqualTo("아이유 콘서트");
    }

    @Test
    void skipsDuplicateContentHash() {
        var duplicate = new NaverCafeItem("중복 글", "https://cafe.naver.com/1", "설명", null);
        Source existing = Source.builder().sourceType(SourceType.NAVER_CAFE).name("Naver Cafe")
                .endpoint("https://openapi.naver.com/v1/search/cafearticle.json").mapsToPinType(PinType.ORIGIN).build();
        when(sources.findByName("Naver Cafe")).thenReturn(Optional.of(existing));
        when(rawArticles.existsByContentHash(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);

        var service = new CommunityIngestService(rawArticles, sources, bodyFetcher);
        var result = service.ingest(
                (query, display) -> List.of(duplicate),
                SourceType.NAVER_CAFE, "Naver Cafe", "https://openapi.naver.com/v1/search/cafearticle.json",
                "뉴진스", 10);

        assertThat(result.saved()).isEqualTo(0);
        assertThat(result.skipped()).isEqualTo(1);
    }
}
