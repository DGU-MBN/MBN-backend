package com.hackathon.MBN.repository;

import com.hackathon.MBN.domain.Event;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    /** 기능명세서 10 AI 숏폼 Feed의 cursor/category/artistId/country/language 필터. */
    @Query("SELECT e FROM Event e WHERE "
            + "(:cursorId IS NULL OR e.id > :cursorId) AND "
            + "(:category IS NULL OR e.category = :category) AND "
            + "(:artistId IS NULL OR e.artist.id = :artistId) AND "
            + "(:country IS NULL OR EXISTS (SELECT 1 FROM EventLocation el WHERE el.event = e AND el.country = :country)) AND "
            + "(:language IS NULL OR EXISTS (SELECT 1 FROM Short s WHERE s.event = e AND s.lang = :language)) "
            // ponytail: popular 정렬은 e.id 기준 커서를 그대로 재사용한다. 페이지가 깊어질수록
            // popularity 순서와 어긋날 수 있음 — Best News류 첫 페이지 노출 이상으로 쓰려면
            // (popularity, id) 복합 커서로 바꿀 것.
            + "ORDER BY CASE WHEN :popular = true THEN e.popularity END DESC, e.id ASC")
    List<Event> findFeedCandidates(
            @Param("cursorId") Long cursorId,
            @Param("category") String category,
            @Param("artistId") Long artistId,
            @Param("country") String country,
            @Param("language") String language,
            @Param("popular") boolean popular,
            Pageable pageable);
}
