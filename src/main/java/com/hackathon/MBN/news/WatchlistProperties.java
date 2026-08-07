package com.hackathon.MBN.news;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 관리자가 channelId/query를 직접 안 넣었을 때 무작위로 골라 쓸 감시 대상 목록.
 * queriesByCategory는 NewsCategory별로 검색어를 나눠둬서, count>1 요청 시 카테고리를 고루 섞어 뽑을 수 있게 한다. */
@ConfigurationProperties(prefix = "watchlist")
public record WatchlistProperties(List<String> youtubeChannelIds, Map<String, List<String>> queriesByCategory) {

    public WatchlistProperties {
        youtubeChannelIds = youtubeChannelIds != null ? youtubeChannelIds : List.of();
        queriesByCategory = queriesByCategory != null ? queriesByCategory : Map.of();
    }
}
