package com.hackathon.MBN.news;

import java.time.Instant;

public record NaverCafeItem(String title, String url, String description, Instant publishedAt)
        implements SearchableContentItem {}
