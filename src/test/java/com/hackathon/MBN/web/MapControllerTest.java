package com.hackathon.MBN.web;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hackathon.MBN.domain.Event;
import com.hackathon.MBN.domain.EventLocation;
import com.hackathon.MBN.domain.type.LocationPrecision;
import com.hackathon.MBN.domain.type.PinType;
import com.hackathon.MBN.repository.EventLocationRepository;
import com.hackathon.MBN.repository.EventRepository;

class MapControllerTest {

    @Test
    void returnsPinsAndHeatmap() {
        EventLocationRepository locationRepository = mock(EventLocationRepository.class);
        EventRepository eventRepository = mock(EventRepository.class);

        Event event = Event.builder().id(1L).title("Test event").category("news").pinType(PinType.ORIGIN).build();
        EventLocation location = EventLocation.builder()
                .id(1L)
                .event(event)
                .lat(37.5665)
                .lng(126.9780)
                .precision(LocationPrecision.CITY)
                .locationName("Seoul")
                .build();

        when(locationRepository.findByLatBetweenAndLngBetween(30.0, 40.0, 120.0, 130.0))
                .thenReturn(List.of(location));
        when(locationRepository.findAll()).thenReturn(List.of(location));

        MapController controller = new MapController(locationRepository, eventRepository);

        List<Map<String, Object>> pins = controller.getPins(30.0, 40.0, 120.0, 130.0);
        List<Map<String, Object>> heatmap = controller.getHeatmap(30.0, 40.0, 120.0, 130.0);

        assertFalse(pins.isEmpty());
        assertFalse(heatmap.isEmpty());
    }
}
