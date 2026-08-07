package com.hackathon.MBN.domain;

import com.hackathon.MBN.domain.type.LocationPrecision;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "event_locations", indexes = @Index(name = "idx_event_locations_lat_lng", columnList = "lat, lng"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lng;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_precision", nullable = false)
    private LocationPrecision precision;

    @Column(nullable = false)
    private String locationName;

    /** 기능명세서 08의 events/{id} 응답 location.country 용. 지오태깅 단계(A)에서 채워짐. */
    private String country;

    /** PRIMARY는 MySQL 예약어라 컬럼명을 is_primary로 지정한다. */
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean primary = true;
}
