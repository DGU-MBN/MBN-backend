package com.hackathon.MBN.news;

import java.util.List;

@FunctionalInterface
public interface ContentSearcher<T extends SearchableContentItem> {
    List<T> search(String query, int display);
}
