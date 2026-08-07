package com.hackathon.MBN.repository;

import com.hackathon.MBN.domain.Short;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortRepository extends JpaRepository<Short, Long> {
    List<Short> findByEventId(Long eventId);

    Optional<Short> findFirstByEventIdAndLang(Long eventId, String lang);

    Optional<Short> findFirstByEventId(Long eventId);

    List<Short> findByLangOrderByIdDesc(String lang);
}
