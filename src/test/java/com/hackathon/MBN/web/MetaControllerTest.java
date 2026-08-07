package com.hackathon.MBN.web;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hackathon.MBN.domain.Artist;
import com.hackathon.MBN.repository.ArtistRepository;

class MetaControllerTest {

    @Test
    void returnsArtistsAndCategories() {
        ArtistRepository artistRepository = mock(ArtistRepository.class);
        when(artistRepository.findAll()).thenReturn(List.of(
                Artist.builder().id(1L).name("BTS").aliases("방탄소년단").category("music").build(),
                Artist.builder().id(2L).name("IU").aliases("아이유").category("music").build(),
                Artist.builder().id(3L).name("Lee Min-ho").aliases("이민호").category("entertainment").build()
        ));

        MetaController controller = new MetaController(artistRepository);

        List<Map<String, Object>> artists = controller.getArtists();
        List<Map<String, Object>> categories = controller.getCategories();

        assertFalse(artists.isEmpty());
        assertFalse(categories.isEmpty());
    }
}
