package com.hackathon.MBN.news;

import com.hackathon.MBN.domain.type.EventStatus;
import com.hackathon.MBN.repository.EventRepository;
import com.hackathon.MBN.repository.ShortRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/news")
public class NewsController {

    private final EventRepository events;
    private final ShortRepository shorts;

    public NewsController(EventRepository events, ShortRepository shorts) {
        this.events = events;
        this.shorts = shorts;
    }

    // AI가 원문에서 추출한 한글 사실관계(Event). 에디터 승인 대기 중(PENDING_REVIEW)인 항목은 제외.
    @GetMapping("/korean")
    public List<KoreanNewsResponse> getKoreanNews() {
        return events.findByStatusNotOrderByIdDesc(EventStatus.PENDING_REVIEW).stream()
                .map(KoreanNewsResponse::from)
                .toList();
    }

    // GPT가 언어별로 재작성한 외국어 뉴스(Short). lang: en | zh | ja
    @GetMapping("/{lang}")
    public List<LocalizedNewsResponse> getLocalizedNews(@PathVariable String lang) {
        return shorts.findByLangOrderByIdDesc(lang).stream()
                .map(LocalizedNewsResponse::from)
                .toList();
    }
}
