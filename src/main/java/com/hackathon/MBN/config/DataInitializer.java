package com.hackathon.MBN.config;

import com.hackathon.MBN.domain.Artist;
import com.hackathon.MBN.repository.ArtistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final ArtistRepository artistRepository;

    @Bean
    public CommandLineRunner seedArtists() {
        return args -> {
            if (artistRepository.count() > 0) {
                return;
            }

            artistRepository.saveAll(java.util.List.of(
                    Artist.builder().name("BTS").aliases("방탄소년단").category("music").build(),
                    Artist.builder().name("BLACKPINK").aliases("블랙핑크").category("music").build(),
                    Artist.builder().name("Stray Kids").aliases("스트레이 키즈").category("music").build(),
                    Artist.builder().name("IU").aliases("아이유").category("music").build(),
                    Artist.builder().name("Lee Min-ho").aliases("이민호").category("entertainment").build(),
                    Artist.builder().name("Son Heung-min").aliases("손흥민").category("sports").build()
            ));
        };
    }
}
