package com.hackathon.MBN.repository;

import com.hackathon.MBN.domain.Event;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findAllByOrderByIdDesc();
}
