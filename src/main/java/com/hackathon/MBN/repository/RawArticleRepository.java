package com.hackathon.MBN.repository;

import com.hackathon.MBN.domain.RawArticle;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawArticleRepository extends JpaRepository<RawArticle, Long> {
    Optional<RawArticle> findByContentHash(String contentHash);
}
