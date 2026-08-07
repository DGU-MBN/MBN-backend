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
    @Column(nullable = false)
    private LocationPrecision precision;

    @Column(nullable = false)
    private String locationName;

    @Builder.Default
    private boolean primary = true;
}
