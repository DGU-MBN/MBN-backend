package com.hackathon.MBN.news;

import com.hackathon.MBN.ai.EventExtractionResult;

public record IngestAndExtractResult(NewsIngestResult ingest, EventExtractionResult extraction) {}
