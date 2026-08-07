package com.hackathon.MBN.repository;

import com.hackathon.MBN.domain.EventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventEntityRepository extends JpaRepository<EventEntity, Long> {
    List<EventEntity> findByEventId(Long eventId);
}
