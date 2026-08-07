package com.hackathon.MBN.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.MBN.domain.Event;
import com.hackathon.MBN.domain.EventLocation;
import com.hackathon.MBN.domain.RawArticle;
import com.hackathon.MBN.domain.Source;
import com.hackathon.MBN.domain.type.AiConfidence;
import com.hackathon.MBN.domain.type.LocationPrecision;
import com.hackathon.MBN.domain.type.SourceType;
import com.hackathon.MBN.repository.EventLocationRepository;
import com.hackathon.MBN.repository.EventRepository;
import com.hackathon.MBN.repository.RawArticleRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class EventExtractionServiceTest {

    @Mock
    RawArticleRepository rawArticles;

    @Mock
    EventRepository events;

    @Mock
    EventLocationRepository eventLocations;

    @Mock
    AiEventExtractor extractor;

    private RawArticle article(long id, String title) {
        Source source = Source.builder().sourceType(SourceType.NEWS_RSS).name("Naver News")
                .endpoint("https://example.com").build();
        return RawArticle.builder()
                .id(id)
                .source(source)
                .url("https://example.com/" + id)
                .contentHash("hash-" + id)
                .title(title)
                .description("본문 " + id)
                .build();
    }

    private void stubSaveReturnsArgument() {
        when(events.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void skipsArticleWhenLocationNameIsNull() {
        RawArticle article = article(1, "제목");
        when(rawArticles.findUnprocessed(any(Pageable.class))).thenReturn(List.of(article));
        when(extractor.extract(article)).thenReturn(new ArticleExtraction(
                "제목", null, null, null, null, null, "사회", AiConfidence.LOW, "근거"));

        var result = new EventExtractionService(rawArticles, events, eventLocations, extractor).extractEvents(10);

        assertThat(result.processed()).isEqualTo(1);
        assertThat(result.created()).isEqualTo(0);
        assertThat(result.skipped()).isEqualTo(1);
        verify(events, never()).save(any());
    }

    @Test
    void createsEventWithRawArticleTextAsSummary() {
        RawArticle article = article(2, "강남 화재");
        stubSaveReturnsArgument();
        when(rawArticles.findUnprocessed(any(Pageable.class))).thenReturn(List.of(article));
        when(extractor.extract(article)).thenReturn(new ArticleExtraction(
                "강남 오피스텔 화재", "서울 강남구 역삼동", "서울특별시 강남구", null, null, null,
                "사회", AiConfidence.HIGH, "근거 문장"));

        var result = new EventExtractionService(rawArticles, events, eventLocations, extractor).extractEvents(10);

        assertThat(result.processed()).isEqualTo(1);
        assertThat(result.created()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(0);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(events).save(captor.capture());
        Event saved = captor.getValue();
        assertThat(saved.getSourceArticle()).isSameAs(article);
        assertThat(saved.getTitle()).isEqualTo("강남 오피스텔 화재");
        assertThat(saved.getLocationName()).isEqualTo("서울 강남구 역삼동");
        assertThat(saved.getAdminArea()).isEqualTo("서울특별시 강남구");
        assertThat(saved.getSummary()).isEqualTo(article.getDescription());
        assertThat(saved.getCategory()).isEqualTo("사회");
        assertThat(saved.getAiConfidence()).isEqualTo(AiConfidence.HIGH);
        assertThat(saved.getEvidence()).isEqualTo("근거 문장");
        verify(eventLocations, never()).save(any());
    }

    @Test
    void savesEventLocationWhenLatLngPresent() {
        RawArticle article = article(6, "강남 화재");
        stubSaveReturnsArgument();
        when(rawArticles.findUnprocessed(any(Pageable.class))).thenReturn(List.of(article));
        when(extractor.extract(article)).thenReturn(new ArticleExtraction(
                "강남 오피스텔 화재", "서울 강남구 역삼동", "서울특별시 강남구", 37.5006, 127.0365, LocationPrecision.VENUE,
                "사회", AiConfidence.HIGH, "근거 문장"));

        new EventExtractionService(rawArticles, events, eventLocations, extractor).extractEvents(10);

        ArgumentCaptor<EventLocation> captor = ArgumentCaptor.forClass(EventLocation.class);
        verify(eventLocations).save(captor.capture());
        EventLocation saved = captor.getValue();
        assertThat(saved.getLat()).isEqualTo(37.5006);
        assertThat(saved.getLng()).isEqualTo(127.0365);
        assertThat(saved.getPrecision()).isEqualTo(LocationPrecision.VENUE);
        assertThat(saved.getLocationName()).isEqualTo("서울 강남구 역삼동");
    }

    @Test
    void defaultsToCityPrecisionWhenMissing() {
        RawArticle article = article(7, "부산 축제");
        stubSaveReturnsArgument();
        when(rawArticles.findUnprocessed(any(Pageable.class))).thenReturn(List.of(article));
        when(extractor.extract(article)).thenReturn(new ArticleExtraction(
                "부산 축제", "부산", "부산광역시", 35.1796, 129.0756, null,
                "문화", AiConfidence.MEDIUM, "근거"));

        new EventExtractionService(rawArticles, events, eventLocations, extractor).extractEvents(10);

        ArgumentCaptor<EventLocation> captor = ArgumentCaptor.forClass(EventLocation.class);
        verify(eventLocations).save(captor.capture());
        assertThat(captor.getValue().getPrecision()).isEqualTo(LocationPrecision.CITY);
    }

    @Test
    void fallsBackToDefaultCategoryWhenUnknown() {
        RawArticle article = article(3, "제목");
        stubSaveReturnsArgument();
        when(rawArticles.findUnprocessed(any(Pageable.class))).thenReturn(List.of(article));
        when(extractor.extract(article)).thenReturn(new ArticleExtraction(
                "제목", "서울", "서울", null, null, null, "존재하지않는카테고리", AiConfidence.LOW, "근거"));

        new EventExtractionService(rawArticles, events, eventLocations, extractor).extractEvents(10);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(events).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo("사회");
    }

    @Test
    void continuesProcessingWhenExtractorThrowsForOneArticle() {
        RawArticle failing = article(4, "실패");
        RawArticle succeeding = article(5, "성공");
        stubSaveReturnsArgument();
        when(rawArticles.findUnprocessed(any(Pageable.class))).thenReturn(List.of(failing, succeeding));
        when(extractor.extract(failing)).thenThrow(new IllegalArgumentException("AI 파싱 실패"));
        when(extractor.extract(succeeding)).thenReturn(new ArticleExtraction(
                "성공 이벤트", "부산", "부산광역시", null, null, null, "경제", AiConfidence.MEDIUM, "근거"));

        var result = new EventExtractionService(rawArticles, events, eventLocations, extractor).extractEvents(10);

        assertThat(result.processed()).isEqualTo(2);
        assertThat(result.created()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
    }
}
