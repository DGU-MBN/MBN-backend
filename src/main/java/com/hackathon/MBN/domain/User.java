package com.hackathon.MBN.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String deviceToken;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String preferredLang = "en";

    /** 지도에서 보고 싶은 핀 타입, 쉼표 구분 (예: origin,broadcast). 비어있으면 전체 */
    private String pinTypeFilter;

    /*
     * open-in-view: false 환경이라 컨트롤러가 실행되는 시점엔 이미 영속성 컨텍스트가 닫혀 있다.
     * @CurrentUser 리졸버가 User를 조회하는 시점도 트랜잭션 밖이라, LAZY로 두면
     * 컨트롤러에서 이 컬렉션에 접근하는 즉시 LazyInitializationException이 난다.
     * User당 개수가 적은 컬렉션이라 EAGER 비용은 무시할 만하다.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_interested_artists", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "artist_id")
    @Builder.Default
    private Set<Long> interestedArtistIds = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_interested_categories", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "category")
    @Builder.Default
    private Set<String> interestedCategories = new HashSet<>();

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
