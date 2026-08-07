package com.hackathon.MBN.web;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hackathon.MBN.domain.Event;
import com.hackathon.MBN.domain.EventEntity;
import com.hackathon.MBN.domain.Short;
import com.hackathon.MBN.domain.type.Confidence;
import com.hackathon.MBN.domain.type.EventStatus;
import com.hackathon.MBN.domain.type.PinType;
import com.hackathon.MBN.domain.type.ShortStyle;
import com.hackathon.MBN.repository.EventEntityRepository;
import com.hackathon.MBN.repository.EventRepository;
import com.hackathon.MBN.repository.ShortRepository;

class FeedControllerTest {

    @Test
    void returnsFeedAndDetailResponses() {
        EventRepository eventRepository = mock(EventRepository.class);
        ShortRepository shortRepository = mock(ShortRepository.class);
        EventEntityRepository eventEntityRepository = mock(EventEntityRepository.class);

        Event event = Event.builder()
                .id(1L)
                .title("Sample event")
                .category("news")
                .pinType(PinType.ORIGIN)
                .confidence(Confidence.UNVERIFIED)
                .status(EventStatus.COLLECTED)
                .build();
        Short shortItem = Short.builder()
                .id(10L)
                .event(event)
                .title("Sample short")
                .lang("en")
                .style(ShortStyle.STANDARD)
                .build();
        EventEntity entity = EventEntity.builder().id(100L).event(event).factText("Fact text").verified(true).build();

        when(eventRepository.findAll()).thenReturn(List.of(event));
        when(eventRepository.findById(1L)).thenReturn(java.util.Optional.of(event));
        when(shortRepository.findAll()).thenReturn(List.of(shortItem));
        when(shortRepository.findById(10L)).thenReturn(java.util.Optional.of(shortItem));
        when(eventEntityRepository.findAll()).thenReturn(List.of(entity));

        FeedController controller = new FeedController(eventRepository, shortRepository, eventEntityRepository);

        List<Map<String, Object>> feed = controller.getFeed();
        Map<String, Object> detail = controller.getEventDetail(1L);
        List<Map<String, Object>> variants = controller.getEventVariants(1L);
        Map<String, Object> shortResponse = controller.getShort(10L);

        assertFalse(feed.isEmpty());
        assertFalse(variants.isEmpty());
        assertNotNull(detail.get("title"));
        assertNotNull(shortResponse.get("title"));
    }
}
