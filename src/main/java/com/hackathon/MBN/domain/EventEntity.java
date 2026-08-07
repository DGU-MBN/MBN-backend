package com.hackathon.MBN.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 사건의 팩트/근거 한 건. RawArticle에서 추출된 사실관계를 Event에 묶는다. */
@Entity
@Table(name = "event_entities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_article_id")
    private RawArticle rawArticle;

    @Lob
    @Column(nullable = false)
    private String factText;

    @Builder.Default
    private boolean verified = false;
}
