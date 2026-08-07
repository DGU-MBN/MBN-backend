package com.hackathon.MBN.news;

import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

/**
 * 원문 URL에서 본문 전문을 가져온다. 언론사별 CSS 셀렉터 없이 <p> 태그를 전부 이어붙이는
 * 범용 휴리스틱이라 광고/기자 소개 문구가 섞여 들어올 수 있다.
 * ponytail: 추출 정확도가 문제되면 도메인별 셀렉터 매핑으로 교체
 */
@Component
public class ArticleBodyFetcher {

    public String fetch(String url) throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(10_000)
                .get();
        return extractBody(doc);
    }

    static String extractBody(Document doc) {
        String body = doc.select("p").text();
        return body.isBlank() ? null : body;
    }
}
