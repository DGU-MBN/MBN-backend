package com.hackathon.MBN.domain;

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
