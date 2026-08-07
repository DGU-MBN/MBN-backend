package com.hackathon.MBN.news;

import java.time.Instant;

public interface SearchableContentItem {
    String title();
    String url();
    String description();
    Instant publishedAt();
}
