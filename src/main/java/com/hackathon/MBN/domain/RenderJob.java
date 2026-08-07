package com.hackathon.MBN.domain;

import com.hackathon.MBN.domain.type.RenderJobStage;
import com.hackathon.MBN.domain.type.RenderJobStatus;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 파이프라인 단계 큐. @Scheduled 폴링이 status=QUEUED 건을 집어서 처리한다. */
@Entity
@Table(name = "render_jobs", indexes = @Index(name = "idx_render_jobs_status", columnList = "status"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RenderJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RenderJobStage stage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RenderJobStatus status = RenderJobStatus.QUEUED;

    @Lob
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
