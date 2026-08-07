package com.hackathon.MBN.repository;

import com.hackathon.MBN.domain.EventLocation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventLocationRepository extends JpaRepository<EventLocation, Long> {
    List<EventLocation> findByLatBetweenAndLngBetween(double minLat, double maxLat, double minLng, double maxLng);

    Optional<EventLocation> findFirstByEventIdAndPrimaryTrue(Long eventId);
}
