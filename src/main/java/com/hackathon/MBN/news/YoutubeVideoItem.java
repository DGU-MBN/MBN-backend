package com.hackathon.MBN.news;

import java.time.Instant;

public record YoutubeVideoItem(String videoId, String title, String description, String url, Instant publishedAt) {}
