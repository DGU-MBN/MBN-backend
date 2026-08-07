package com.hackathon.MBN.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 디바이스 토큰 인증은 컨트롤러 계층에서 직접 처리하므로(구현계획 B-1, JWT 미사용)
 * Spring Security는 길만 열어준다.
 */
import org.springframework.security.web.SecurityFilterChain;

/** 디바이스 토큰/관리자 인증이 아직 없어서 임시로 전체 개방. 인증 붙으면 이 클래스부터 교체 */
@Configuration
public class SecurityConfig {

    @Bean
    // ponytail: 전 경로 permitAll. 인증이 필요한 API는 토큰 리졸버가 개별로 막는다.
    //           공개 배포한다면 그때 필터 체인으로 승격할 것.
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
      
        return http.build();
    }
}
