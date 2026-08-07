package com.hackathon.MBN.repository;

import com.hackathon.MBN.domain.Event;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    /** PENDING_REVIEW(에디터 승인 대기 중인 블로그/카페/유튜브발 이벤트)는 공개 목록에서 제외한다. */
    List<Event> findByStatusNotOrderByIdDesc(com.hackathon.MBN.domain.type.EventStatus excludedStatus);

    /** 저신뢰 소스(블로그/카페/유튜브) 이벤트가 이미 검증된 이벤트와 같은 사건인지 판단하는 보조 신호.
     * 아티스트 클러스터링 도입 전까지는 카테고리+장소명 정확 일치라는 단순 휴리스틱으로 대체한다.
     * ponytail: 문자열 정확 일치라 표기가 다르면 놓친다 — 클러스터링(아티스트+72시간) 도입되면 교체 */
    @Query("SELECT e FROM Event e WHERE "
            + "e.status <> com.hackathon.MBN.domain.type.EventStatus.PENDING_REVIEW AND "
            + "e.category = :category AND "
            + "e.locationName = :locationName AND "
            + "e.publishedAt BETWEEN :from AND :to")
    List<Event> findTrustedCorroboration(
            @Param("category") String category,
            @Param("locationName") String locationName,
            @Param("from") Instant from,
            @Param("to") Instant to);

    /** 기능명세서 10 AI 숏폼 Feed의 cursor/category/artistId/country/language 필터.
     * PENDING_REVIEW는 에디터 승인 전까지 노출되면 안 되므로 항상 제외한다. */
    @Query("SELECT e FROM Event e WHERE "
            + "e.status <> com.hackathon.MBN.domain.type.EventStatus.PENDING_REVIEW AND "
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
