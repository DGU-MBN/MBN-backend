package com.hackathon.MBN.repository;

import com.hackathon.MBN.domain.Source;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceRepository extends JpaRepository<Source, Long> {

    Optional<Source> findByName(String name);
}
