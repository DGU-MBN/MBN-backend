package com.hackathon.MBN.web;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hackathon.MBN.domain.Artist;
import com.hackathon.MBN.domain.Event;
import com.hackathon.MBN.domain.type.PinType;
import com.hackathon.MBN.repository.ArtistRepository;
import com.hackathon.MBN.repository.EventRepository;

class MetaControllerTest {

    @Test
    void returnsArtistsFilteredByQuery() {
        ArtistRepository artistRepository = mock(ArtistRepository.class);
        EventRepository eventRepository = mock(EventRepository.class);
        when(artistRepository.findByNameContainingIgnoreCase("bl"))
                .thenReturn(List.of(Artist.builder().id(1L).name("BLACKPINK").aliases("블랙핑크").category("music").build()));

        MetaController controller = new MetaController(artistRepository, eventRepository);
        List<Map<String, Object>> artists = controller.getArtists("bl");

        assertEquals(1, artists.size());
        assertEquals("BLACKPINK", artists.get(0).get("name"));
    }

    @Test
    void returnsFixedCategoryList() {
        ArtistRepository artistRepository = mock(ArtistRepository.class);
        EventRepository eventRepository = mock(EventRepository.class);

        MetaController controller = new MetaController(artistRepository, eventRepository);
        List<String> categories = controller.getCategories();

        assertTrue(categories.contains("KPOP"));
        assertTrue(categories.contains("BEAUTY"));
        assertEquals(8, categories.size());
    }

    @Test
    void searchCombinesArtistsAndEvents() {
        ArtistRepository artistRepository = mock(ArtistRepository.class);
        EventRepository eventRepository = mock(EventRepository.class);
        when(artistRepository.findByNameContainingIgnoreCase("black"))
                .thenReturn(List.of(Artist.builder().id(1L).name("BLACKPINK").build()));
        when(eventRepository.findAll()).thenReturn(List.of(
                Event.builder().id(10L).title("BLACKPINK Paris Event").category("KPOP").pinType(PinType.ORIGIN).build(),
                Event.builder().id(11L).title("Unrelated news").category("DRAMA").pinType(PinType.ORIGIN).build()));

        MetaController controller = new MetaController(artistRepository, eventRepository);
        List<Map<String, Object>> results = controller.search("black");

        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(r -> "ARTIST".equals(r.get("type"))));
        assertTrue(results.stream().anyMatch(r -> "EVENT".equals(r.get("type"))));
    }
}
