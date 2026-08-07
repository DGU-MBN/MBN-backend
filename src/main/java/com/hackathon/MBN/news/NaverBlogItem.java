package com.hackathon.MBN.news;

import java.time.Instant;

public record NaverBlogItem(String title, String url, String description, Instant publishedAt)
        implements SearchableContentItem {}
