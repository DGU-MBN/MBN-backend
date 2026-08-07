package com.hackathon.MBN.domain;

import com.hackathon.MBN.domain.type.LicenseStatus;
import com.hackathon.MBN.domain.type.PinType;
import com.hackathon.MBN.domain.type.SourceStatus;
import com.hackathon.MBN.domain.type.SourceType;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceType sourceType;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 1000)
    private String endpoint;

    private Integer pollIntervalMin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PinType mapsToPinType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LicenseStatus licenseStatus = LicenseStatus.UNREVIEWED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SourceStatus status = SourceStatus.ACTIVE;

    private Instant lastPolledAt;

    private Instant nextPollAt;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
