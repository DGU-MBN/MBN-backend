package com.hackathon.MBN.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "raw_articles", indexes = @Index(name = "idx_raw_articles_content_hash", columnList = "content_hash", unique = true))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RawArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(nullable = false, unique = true, length = 64)
    private String contentHash;

    @Column(nullable = false, length = 500)
    private String title;

    @Lob
    private String description;

    /** 원문 URL에서 크롤링한 본문 전문. 크롤링 실패하면 null (description 스니펫으로 폴백) */
    @Lob
    private String content;

    private Instant publishedAt;

    private Instant lastSeenAt;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
