package com.hackathon.MBN.repository;

import com.hackathon.MBN.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}
