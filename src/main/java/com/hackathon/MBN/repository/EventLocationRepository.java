package com.hackathon.MBN.repository;

import com.hackathon.MBN.domain.EventLocation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventLocationRepository extends JpaRepository<EventLocation, Long> {
    List<EventLocation> findByLatBetweenAndLngBetween(double minLat, double maxLat, double minLng, double maxLng);
}
