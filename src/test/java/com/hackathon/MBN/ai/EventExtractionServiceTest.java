package com.hackathon.MBN.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hackathon.MBN.domain.Event;
import com.hackathon.MBN.domain.EventEntity;
import com.hackathon.MBN.domain.EventLocation;
import com.hackathon.MBN.domain.RawArticle;
import com.hackathon.MBN.domain.Short;
import com.hackathon.MBN.domain.Source;
import com.hackathon.MBN.domain.type.AiConfidence;
import com.hackathon.MBN.domain.type.Confidence;
import com.hackathon.MBN.domain.type.LocationPrecision;
import com.hackathon.MBN.domain.type.SourceType;
import com.hackathon.MBN.repository.EventEntityRepository;
import com.hackathon.MBN.repository.EventLocationRepository;
import com.hackathon.MBN.repository.EventRepository;
import com.hackathon.MBN.repository.RawArticleRepository;
import com.hackathon.MBN.repository.ShortRepository;
import java.time.Instant;
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
    ShortRepository shorts;

    @Mock
    EventEntityRepository eventEntities;

    @Mock
    AiEventExtractor extractor;

    @Mock
    AiLocalizer localizer;

    @Mock
    AiFactVerifier factVerifier;

    private EventExtractionService service() {
        return new EventExtractionService(
                rawArticles, events, eventLocations, shorts, eventEntities, extractor, localizer, factVerifier);
    }

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

    private RawArticle articleWithSourceType(long id, String title, SourceType sourceType) {
        Source source = Source.builder().sourceType(sourceType).name("Community Source")
                .endpoint("https://example.com").build();
        return RawArticle.builder()
                .id(id)
                .source(source)
                .url("https://example.com/" + id)
                .contentHash("hash-" + id)
                .title(title)
                .description("본문 " + id)
                .publishedAt(Instant.parse("2026-08-07T00:00:00Z"))
                .build();
    }

    @Test
    void forcesPendingReviewAndSkipsLocalizationForLowTrustSource() {
        RawArticle article = articleWithSourceType(10, "찌라시 의심 글", SourceType.NAVER_BLOG);
        stubSaveReturnsArgument();
        when(rawArticles.findUnprocessed(any(Pageable.class))).thenReturn(List.of(article));
        when(extractor.extract(article)).thenReturn(new ArticleExtraction(
                "미확인 열애설", "서울 강남구", "서울특별시 강남구", null, null, null,
                "스포츠연예", AiConfidence.LOW, "단독 제보"));
        when(events.findTrustedCorroboration(any(), any(), any(), any())).thenReturn(List.of());

        var result = service().extractEvents(10);

        assertThat(result.created()).isEqualTo(1);
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(events).save(captor.capture());
        Event saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(com.hackathon.MBN.domain.type.EventStatus.PENDING_REVIEW);
        assertThat(saved.getConfidence()).isEqualTo(com.hackathon.MBN.domain.type.Confidence.UNVERIFIED);
        assertThat(saved.getReviewReason()).isEqualTo("unverified_source_pending_review");
        assertThat(saved.getByline()).isEqualTo("AI기자");
        verify(shorts, never()).save(any());
    }

    @Test
    void forcesPendingReviewForYoutubeSourceToo() {
        RawArticle article = articleWithSourceType(12, "유튜브 영상 기반 소식", SourceType.YOUTUBE_OFFICIAL);
        stubSaveReturnsArgument();
        when(rawArticles.findUnprocessed(any(Pageable.class))).thenReturn(List.of(article));
        when(extractor.extract(article)).thenReturn(new ArticleExtraction(
                "콘서트 깜짝 등장", "서울 송파구", "서울특별시 송파구", null, null, null,
                "스포츠연예", AiConfidence.MEDIUM, "영상 캡션 근거"));
        when(events.findTrustedCorroboration(any(), any(), any(), any())).thenReturn(List.of());

        var result = service().extractEvents(10);

        assertThat(result.created()).isEqualTo(1);
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(events).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(com.hackathon.MBN.domain.type.EventStatus.PENDING_REVIEW);
        assertThat(captor.getValue().getByline()).isEqualTo("AI기자");
        verify(shorts, never()).save(any());
    }

    @Test
    void appendsCorroborationNoteWhenTrustedEventAlreadyExists() {
        RawArticle article = articleWithSourceType(11, "찌라시 의심 글 2", SourceType.NAVER_CAFE);
        stubSaveReturnsArgument();
        when(rawArticles.findUnprocessed(any(Pageable.class))).thenReturn(List.of(article));
        when(extractor.extract(article)).thenReturn(new ArticleExtraction(
                "컴백 소식", "서울 강남구", "서울특별시 강남구", null, null, null,
                "연예", AiConfidence.MEDIUM, "카페 글 근거"));
        Event trustedEvent = Event.builder().id(99L).build();
        when(events.findTrustedCorroboration(eq("연예"), eq("서울 강남구"), any(), any()))
                .thenReturn(List.of(trustedEvent));

        service().extractEvents(10);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(events).save(captor.capture());
        assertThat(captor.getValue().getEvidence()).contains("교차검증됨").contains("#99");
    }

    @Test
    void skipsArticleWhenLocationNameIsNull() {
        RawArticle article = article(1, "제목");
        when(rawArticles.findUnprocessed(any(Pageable.class))).thenReturn(List.of(article));
        when(extractor.extract(article)).thenReturn(new ArticleExtraction(
                "제목", null, null, null, null, null, "사회", AiConfidence.LOW, "근거"));

        var result = service().extractEvents(10);

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

        var result = service().extractEvents(10);

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
        assertThat(saved.getByline()).isEqualTo("뉴스");
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

        service().extractEvents(10);

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

        service().extractEvents(10);

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

        service().extractEvents(10);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(events).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo("사회");
    }

    @Test
    void localizesIntoThreeLanguagesAfterEventCreated() {
        RawArticle article = article(8, "부산 축제");
        stubSaveReturnsArgument();
        when(rawArticles.findUnprocessed(any(Pageable.class))).thenReturn(List.of(article));
        when(extractor.extract(article)).thenReturn(new ArticleExtraction(
                "부산 축제 개막", "부산", "부산광역시", null, null, null, "문화행사", AiConfidence.MEDIUM, "근거"));
        when(localizer.localize(any(Event.class), any(String.class)))
                .thenAnswer(invocation -> new LocalizedContent("Title in " + invocation.getArgument(1), "Summary"));

        service().extractEvents(10);

        ArgumentCaptor<Short> captor = ArgumentCaptor.forClass(Short.class);
        verify(shorts, times(3)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Short::getLang).containsExactlyInAnyOrder("en", "zh", "ja");
    }

    @Test
    void skipsOnlyFailingLanguageLocalization() {
        RawArticle article = article(9, "부산 축제");
        stubSaveReturnsArgument();
        when(rawArticles.findUnprocessed(any(Pageable.class))).thenReturn(List.of(article));
        when(extractor.extract(article)).thenReturn(new ArticleExtraction(
                "부산 축제 개막", "부산", "부산광역시", null, null, null, "문화행사", AiConfidence.MEDIUM, "근거"));
        when(localizer.localize(any(Event.class), eq("English"))).thenThrow(new IllegalArgumentException("실패"));
        when(localizer.localize(any(Event.class), eq("Chinese"))).thenReturn(new LocalizedContent("中文标题", "中文摘要"));
        when(localizer.localize(any(Event.class), eq("Japanese"))).thenReturn(new LocalizedContent("日本語タイトル", "日本語要約"));

        var result = service().extractEvents(10);

        assertThat(result.created()).isEqualTo(1);
        verify(shorts, times(2)).save(any());
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

        var result = service().extractEvents(10);

        assertThat(result.processed()).isEqualTo(2);
        assertThat(result.created()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
    }

    @Test
    void allFactsVerifiedSetsConfidenceToVerified() {
        RawArticle article = article(13, "부산 축제");
        stubSaveReturnsArgument();
        when(rawArticles.findUnprocessed(any(Pageable.class))).thenReturn(List.of(article));
        when(extractor.extract(article)).thenReturn(new ArticleExtraction(
                "부산 축제 개막", "부산", "부산광역시", null, null, null, "문화행사", AiConfidence.HIGH, "근거"));
        when(factVerifier.verify(eq(article), any(Event.class))).thenReturn(List.of(
                new VerifiedFact("부산에서 축제가 열렸다", true),
                new VerifiedFact("10만명이 방문했다", true)));

        service().extractEvents(10);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(events, times(2)).save(captor.capture());
        assertThat(captor.getValue().getConfidence()).isEqualTo(Confidence.VERIFIED);
        verify(eventEntities, times(2)).save(any(EventEntity.class));
    }

    @Test
    void anyUnverifiedFactSetsConfidenceToDisputed() {
        RawArticle article = article(14, "부산 축제");
        stubSaveReturnsArgument();
        when(rawArticles.findUnprocessed(any(Pageable.class))).thenReturn(List.of(article));
        when(extractor.extract(article)).thenReturn(new ArticleExtraction(
                "부산 축제 개막", "부산", "부산광역시", null, null, null, "문화행사", AiConfidence.HIGH, "근거"));
        when(factVerifier.verify(eq(article), any(Event.class))).thenReturn(List.of(
                new VerifiedFact("부산에서 축제가 열렸다", true),
                new VerifiedFact("100만명이 방문했다", false)));

        service().extractEvents(10);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(events, times(2)).save(captor.capture());
        assertThat(captor.getValue().getConfidence()).isEqualTo(Confidence.DISPUTED);
    }

    @Test
    void verificationFailureDoesNotPreventEventCreation() {
        RawArticle article = article(15, "부산 축제");
        stubSaveReturnsArgument();
        when(rawArticles.findUnprocessed(any(Pageable.class))).thenReturn(List.of(article));
        when(extractor.extract(article)).thenReturn(new ArticleExtraction(
                "부산 축제 개막", "부산", "부산광역시", null, null, null, "문화행사", AiConfidence.HIGH, "근거"));
        when(factVerifier.verify(eq(article), any(Event.class))).thenThrow(new IllegalArgumentException("검증 API 실패"));

        var result = service().extractEvents(10);

        assertThat(result.created()).isEqualTo(1);
        verify(eventEntities, never()).save(any());
    }
}
