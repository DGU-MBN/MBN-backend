package com.hackathon.MBN.web;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hackathon.MBN.domain.Event;
import com.hackathon.MBN.domain.EventLocation;
import com.hackathon.MBN.domain.Interaction;
import com.hackathon.MBN.domain.Short;
import com.hackathon.MBN.domain.type.InteractionType;
import com.hackathon.MBN.domain.type.LocationPrecision;
import com.hackathon.MBN.domain.type.PinType;
import com.hackathon.MBN.repository.EventLocationRepository;
import com.hackathon.MBN.repository.InteractionRepository;

class MapControllerTest {

    @Test
    void returnsSinglePinWhenOnlyOneInCell() {
        EventLocationRepository locationRepository = mock(EventLocationRepository.class);
        InteractionRepository interactionRepository = mock(InteractionRepository.class);

        Event event = Event.builder().id(1L).title("Test event").category("KPOP").pinType(PinType.ORIGIN).popularity(10).build();
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

        MapController controller = new MapController(locationRepository, interactionRepository);

        List<Map<String, Object>> pins = controller.getPins(40.0, 30.0, 130.0, 120.0, 15, null, null, null, null, null);

        assertEquals(1, pins.size());
        assertEquals("single", pins.get(0).get("type"));
        assertEquals(1L, pins.get(0).get("eventId"));
    }

    @Test
    void clustersPinsInSameGridCellAtLowZoom() {
        EventLocationRepository locationRepository = mock(EventLocationRepository.class);
        InteractionRepository interactionRepository = mock(InteractionRepository.class);

        Event eventA = Event.builder().id(1L).title("A").category("KPOP").pinType(PinType.ORIGIN).build();
        Event eventB = Event.builder().id(2L).title("B").category("KPOP").pinType(PinType.ORIGIN).build();
        EventLocation locationA = EventLocation.builder().id(1L).event(eventA).lat(37.51).lng(126.91).precision(LocationPrecision.CITY).locationName("Seoul").build();
        EventLocation locationB = EventLocation.builder().id(2L).event(eventB).lat(37.52).lng(126.92).precision(LocationPrecision.CITY).locationName("Seoul").build();

        when(locationRepository.findByLatBetweenAndLngBetween(30.0, 40.0, 120.0, 130.0))
                .thenReturn(List.of(locationA, locationB));

        MapController controller = new MapController(locationRepository, interactionRepository);

        // zoom=3 -> precision 0자리, 두 좌표가 같은 격자로 뭉쳐야 함
        List<Map<String, Object>> pins = controller.getPins(40.0, 30.0, 130.0, 120.0, 3, null, null, null, null, null);

        assertEquals(1, pins.size());
        assertEquals("cluster", pins.get(0).get("type"));
        assertEquals(2, pins.get(0).get("count"));
    }

    @Test
    void aggregatesHeatmapByCountry() {
        EventLocationRepository locationRepository = mock(EventLocationRepository.class);
        InteractionRepository interactionRepository = mock(InteractionRepository.class);

        Short shortVideo = Short.builder().id(7L).title("Short").lang("en").build();
        Interaction viewJp = Interaction.builder().id(1L).shortVideo(shortVideo).type(InteractionType.VIEW).countryCode("JP").build();
        Interaction likeJp = Interaction.builder().id(2L).shortVideo(shortVideo).type(InteractionType.LIKE).countryCode("JP").build();
        Interaction viewUs = Interaction.builder().id(3L).shortVideo(shortVideo).type(InteractionType.VIEW).countryCode("US").build();

        when(interactionRepository.findAll()).thenReturn(List.of(viewJp, likeJp, viewUs));

        MapController controller = new MapController(locationRepository, interactionRepository);
        Map<String, Object> heatmap = controller.getHeatmap(null);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cells = (List<Map<String, Object>>) heatmap.get("cells");
        assertFalse(cells.isEmpty());
        // JP: view(1) + like(1*2) = 3, US: view(1) -> JP가 먼저 와야 함
        assertEquals("JP", cells.get(0).get("country"));
        assertEquals(3L, cells.get(0).get("reactionScore"));
    }
}
