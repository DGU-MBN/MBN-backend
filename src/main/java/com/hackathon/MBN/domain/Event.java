package com.hackathon.MBN.domain;

import com.hackathon.MBN.domain.type.AiConfidence;
import com.hackathon.MBN.domain.type.Confidence;
import com.hackathon.MBN.domain.type.EventStatus;
import com.hackathon.MBN.domain.type.PinType;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** AI 추출 흐름에서 어떤 RawArticle로부터 만들어졌는지. 클러스터링 도입 전까지는 기사 1개당 Event 1개 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_article_id", unique = true)
    private RawArticle sourceArticle;

    @Column(nullable = false)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PinType pinType;

    /** 기능명세서 07/10의 artistId 필터·검색을 위한 연관관계. 클러스터링 단계(A)에서 채워짐. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    private Artist artist;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Confidence confidence = Confidence.UNVERIFIED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EventStatus status = EventStatus.COLLECTED;

    @Column(nullable = false, length = 500)
    private String title;

    /** pending_review일 때만 채움: license_unreviewed / fact_conflict 등 */
    private String reviewReason;

    /** 출처 표시용 바이라인. NEWS_RSS(뉴스) 출신이면 "뉴스", 블로그/카페/유튜브(AI가 원문에서 재구성) 출신이면 "AI기자". */
    private String byline;

    // AI 지역/카테고리 추출 결과. 지오코딩 전이라 EventLocation 대신 여기 임시 보관
    private String locationName;
    private String adminArea;

    @Lob
    private String summary;

    @Lob
    private String evidence;

    @Enumerated(EnumType.STRING)
    private AiConfidence aiConfidence;

    // pinType == BROADCAST 일 때만 사용
    private String programName;
    private Instant airDate;

    private Instant publishedAt;

    @Builder.Default
    private Integer popularity = 0;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
