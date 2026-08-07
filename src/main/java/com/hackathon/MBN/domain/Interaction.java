package com.hackathon.MBN.domain;

import com.hackathon.MBN.domain.type.InteractionType;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** /shorts/:id/interactions 원천 로그. heatmap 배치 집계의 원본 데이터. */
@Entity
@Table(name = "interactions", indexes = @Index(name = "idx_interactions_country_created", columnList = "country_code, created_at"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Interaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "short_id", nullable = false)
    private Short shortVideo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InteractionType type;

    /** view / complete 에서만 사용 */
    private Double watchRatio;

    @Column(length = 2)
    private String countryCode;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
