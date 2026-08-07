package com.hackathon.MBN.repository;

import com.hackathon.MBN.domain.Interaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {
    /** 기능명세서 09 Reaction Heatmap의 원천 조회. eventId 필터 시 사용. */
    List<Interaction> findByShortVideo_Event_Id(Long eventId);
}
