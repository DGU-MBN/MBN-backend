package com.hackathon.MBN.news;

import static org.assertj.core.api.Assertions.assertThat;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class ArticleBodyFetcherTest {

    @Test
    void extractBodyJoinsParagraphText() {
        var doc = Jsoup.parse("<html><body><p>첫 문단입니다.</p><script>ignore()</script><p>둘째 문단입니다.</p></body></html>");

        assertThat(ArticleBodyFetcher.extractBody(doc)).isEqualTo("첫 문단입니다. 둘째 문단입니다.");
    }

    @Test
    void extractBodyReturnsNullWhenNoParagraphs() {
        var doc = Jsoup.parse("<html><body><div>본문 없음</div></body></html>");

        assertThat(ArticleBodyFetcher.extractBody(doc)).isNull();
    }
}
