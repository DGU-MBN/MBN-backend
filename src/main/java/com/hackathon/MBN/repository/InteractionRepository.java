package com.hackathon.MBN.repository;

import com.hackathon.MBN.domain.Interaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {
}
