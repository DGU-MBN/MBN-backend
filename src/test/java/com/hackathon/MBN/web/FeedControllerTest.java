package com.hackathon.MBN.web;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hackathon.MBN.domain.Event;
import com.hackathon.MBN.domain.EventEntity;
import com.hackathon.MBN.domain.EventLocation;
import com.hackathon.MBN.domain.RawArticle;
import com.hackathon.MBN.domain.Short;
import com.hackathon.MBN.domain.Source;
import com.hackathon.MBN.domain.type.Confidence;
import com.hackathon.MBN.domain.type.EventStatus;
import com.hackathon.MBN.domain.type.LocationPrecision;
import com.hackathon.MBN.domain.type.PinType;
import com.hackathon.MBN.domain.type.ShortStyle;
import com.hackathon.MBN.domain.type.SourceType;
import com.hackathon.MBN.repository.EventEntityRepository;
import com.hackathon.MBN.repository.EventLocationRepository;
import com.hackathon.MBN.repository.EventRepository;
import com.hackathon.MBN.repository.ShortRepository;

class FeedControllerTest {

    @Test
    void returnsFeedItemsWithNextCursor() {
        EventRepository eventRepository = mock(EventRepository.class);
        ShortRepository shortRepository = mock(ShortRepository.class);
        EventEntityRepository eventEntityRepository = mock(EventEntityRepository.class);
        EventLocationRepository eventLocationRepository = mock(EventLocationRepository.class);

        Event event = Event.builder().id(1L).title("Sample event").category("KPOP").pinType(PinType.ORIGIN).build();
        Short shortItem = Short.builder().id(10L).event(event).title("Sample short").lang("en").videoUrl("https://cdn/1.mp4").build();
        EventLocation location = EventLocation.builder().id(1L).event(event).lat(48.8).lng(2.3)
                .precision(LocationPrecision.CITY).locationName("Paris").country("France").primary(true).build();

        when(eventRepository.findFeedCandidates(isNull(), isNull(), isNull(), isNull(), isNull(), anyBoolean(), any()))
                .thenReturn(List.of(event));
        when(shortRepository.findFirstByEventId(1L)).thenReturn(Optional.of(shortItem));
        when(eventLocationRepository.findFirstByEventIdAndPrimaryTrue(1L)).thenReturn(Optional.of(location));

        FeedController controller = new FeedController(eventRepository, shortRepository, eventEntityRepository, eventLocationRepository);
        Map<String, Object> feed = controller.getFeed(null, null, null, null, null, null, null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) feed.get("items");
        assertEquals(1, items.size());
        assertEquals("Paris", items.get(0).get("location"));
        assertEquals("France", items.get(0).get("country"));
        assertEquals("https://cdn/1.mp4", items.get(0).get("videoUrl"));
    }

    @Test
    void returnsEventDetailWithLocationAndSources() {
        EventRepository eventRepository = mock(EventRepository.class);
        ShortRepository shortRepository = mock(ShortRepository.class);
        EventEntityRepository eventEntityRepository = mock(EventEntityRepository.class);
        EventLocationRepository eventLocationRepository = mock(EventLocationRepository.class);

        Event event = Event.builder()
                .id(1L).title("Sample event").category("KPOP").pinType(PinType.ORIGIN)
                .confidence(Confidence.UNVERIFIED).status(EventStatus.COLLECTED).build();
        EventLocation location = EventLocation.builder().id(1L).event(event).lat(48.8).lng(2.3)
                .precision(LocationPrecision.CITY).locationName("Paris").country("France").primary(true).build();
        Source source = Source.builder().id(1L).name("Yonhap").endpoint("https://yna.co.kr").sourceType(SourceType.NEWS_RSS).mapsToPinType(PinType.ORIGIN).build();
        RawArticle article = RawArticle.builder().id(1L).source(source).url("https://yna.co.kr/1").contentHash("h1").title("원문 제목").build();
        EventEntity entity = EventEntity.builder().id(100L).event(event).rawArticle(article).factText("Fact text").verified(true).build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventEntityRepository.findByEventId(1L)).thenReturn(List.of(entity));
        when(eventLocationRepository.findFirstByEventIdAndPrimaryTrue(1L)).thenReturn(Optional.of(location));
        when(shortRepository.findFirstByEventId(1L)).thenReturn(Optional.empty());

        FeedController controller = new FeedController(eventRepository, shortRepository, eventEntityRepository, eventLocationRepository);
        Map<String, Object> detail = controller.getEventDetail(1L);

        assertNotNull(detail.get("title"));
        @SuppressWarnings("unchecked")
        Map<String, Object> locationResponse = (Map<String, Object>) detail.get("location");
        assertEquals("Paris", locationResponse.get("name"));
        assertEquals("France", locationResponse.get("country"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sources = (List<Map<String, Object>>) detail.get("sources");
        assertEquals(1, sources.size());
        assertEquals("Yonhap", sources.get(0).get("publisher"));
    }

    @Test
    void popularSortRequestsPopularityOrdering() {
        EventRepository eventRepository = mock(EventRepository.class);
        ShortRepository shortRepository = mock(ShortRepository.class);
        EventEntityRepository eventEntityRepository = mock(EventEntityRepository.class);
        EventLocationRepository eventLocationRepository = mock(EventLocationRepository.class);

        when(eventRepository.findFeedCandidates(isNull(), isNull(), isNull(), isNull(), isNull(), org.mockito.ArgumentMatchers.eq(true), any()))
                .thenReturn(List.of());

        FeedController controller = new FeedController(eventRepository, shortRepository, eventEntityRepository, eventLocationRepository);
        Map<String, Object> feed = controller.getFeed(null, null, null, null, null, null, "popular");

        assertEquals(List.of(), feed.get("items"));
    }

    @Test
    void returnsVariantsAndShort() {
        EventRepository eventRepository = mock(EventRepository.class);
        ShortRepository shortRepository = mock(ShortRepository.class);
        EventEntityRepository eventEntityRepository = mock(EventEntityRepository.class);
        EventLocationRepository eventLocationRepository = mock(EventLocationRepository.class);

        Event event = Event.builder().id(1L).title("Sample event").category("KPOP").pinType(PinType.ORIGIN).build();
        Short shortItem = Short.builder().id(10L).event(event).title("Sample short").lang("en").style(ShortStyle.STANDARD).build();

        when(shortRepository.findByEventId(1L)).thenReturn(List.of(shortItem));
        when(shortRepository.findById(10L)).thenReturn(Optional.of(shortItem));

        FeedController controller = new FeedController(eventRepository, shortRepository, eventEntityRepository, eventLocationRepository);

        List<Map<String, Object>> variants = controller.getEventVariants(1L);
        Map<String, Object> shortResponse = controller.getShort(10L);

        assertFalse(variants.isEmpty());
        assertNotNull(shortResponse.get("title"));
    }
}
