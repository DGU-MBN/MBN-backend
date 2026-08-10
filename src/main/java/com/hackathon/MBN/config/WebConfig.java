package com.hackathon.MBN.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.hackathon.MBN.web.CurrentUserArgumentResolver;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 휴대폰 등 같은 Wi-Fi의 다른 기기에서 열면 Origin이 localhost가 아니라
        // 맥의 사설 IP(192.168.x.x/10.x.x.x/172.16~31.x.x)가 된다. 하드코딩된
        // localhost:5173만 허용하면 그 요청들이 다 막힌다.
        // allowedOriginPatterns는 * 와일드카드만 지원한다(정규식 문자클래스 불가).
        registry.addMapping("/**")
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://192.168.*",
                        "http://10.*",
                        "http://172.*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
