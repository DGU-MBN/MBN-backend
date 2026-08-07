package com.hackathon.MBN.repository;

import com.hackathon.MBN.domain.RawArticle;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RawArticleRepository extends JpaRepository<RawArticle, Long> {
    Optional<RawArticle> findByContentHash(String contentHash);

    boolean existsByContentHash(String contentHash);

    @Query("SELECT r FROM RawArticle r WHERE NOT EXISTS (SELECT 1 FROM Event e WHERE e.sourceArticle = r) ORDER BY r.id ASC")
    List<RawArticle> findUnprocessed(Pageable pageable);
}
