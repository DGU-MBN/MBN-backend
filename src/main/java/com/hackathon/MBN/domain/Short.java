package com.hackathon.MBN.domain;

import com.hackathon.MBN.domain.type.ShortStyle;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "shorts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Short {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 10)
    private String lang;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ShortStyle style = ShortStyle.STANDARD;

    @Column(length = 1000)
    private String videoUrl;

    @Column(length = 1000)
    private String hlsUrl;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 1000)
    private String thumbnailUrl;

    @Builder.Default
    private long views = 0;

    @Builder.Default
    private long likes = 0;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
