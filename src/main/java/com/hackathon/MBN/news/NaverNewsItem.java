package com.hackathon.MBN.news;

import java.time.Instant;

public record NaverNewsItem(String title, String url, String description, Instant publishedAt) {}
