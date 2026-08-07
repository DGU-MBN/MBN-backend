package com.hackathon.MBN.web;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.MBN.domain.Event;
import com.hackathon.MBN.domain.RawArticle;
import com.hackathon.MBN.domain.RenderJob;
import com.hackathon.MBN.domain.Source;
import com.hackathon.MBN.domain.type.PinType;
import com.hackathon.MBN.domain.type.RenderJobStage;
import com.hackathon.MBN.domain.type.RenderJobStatus;
import com.hackathon.MBN.domain.type.SourceType;
import com.hackathon.MBN.repository.EventRepository;
import com.hackathon.MBN.repository.RawArticleRepository;
import com.hackathon.MBN.repository.RenderJobRepository;
import com.hackathon.MBN.repository.SourceRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminPipelineController {

    private final RawArticleRepository rawArticleRepository;
    private final EventRepository eventRepository;
    private final RenderJobRepository renderJobRepository;
    private final SourceRepository sourceRepository;

    @PostMapping("/ingest")
    public Map<String, Object> ingest(@RequestBody IngestRequest request) {
        if ((request.keyword() == null || request.keyword().isBlank()) && (request.url() == null || request.url().isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "keyword or url is required");
        }

        Source source = sourceRepository.findAll().stream().findFirst().orElseGet(() -> {
            Source defaultSource = Source.builder()
                    .name("Manual Ingest")
                    .endpoint("manual")
                    .sourceType(SourceType.NEWS_RSS)
                    .mapsToPinType(PinType.ORIGIN)
                    .build();
            return sourceRepository.save(defaultSource);
        });

        RawArticle article = RawArticle.builder()
                .source(source)
                .url(request.url() != null ? request.url() : "keyword:" + request.keyword())
                .contentHash("manual-" + Instant.now().toEpochMilli())
                .title(request.keyword() != null ? "Manual ingest: " + request.keyword() : "Manual ingest")
                .description(request.keyword())
                .publishedAt(Instant.now())
                .lastSeenAt(Instant.now())
                .build();
        article = rawArticleRepository.save(article);

        Event event = eventRepository.save(Event.builder()
                .category("manual")
                .pinType(PinType.ORIGIN)
                .title(request.keyword() != null ? "Manual ingest: " + request.keyword() : "Manual ingest")
                .build());

        RenderJob renderJob = RenderJob.builder()
                .event(event)
                .stage(RenderJobStage.INGEST)
                .status(RenderJobStatus.QUEUED)
                .build();
        renderJobRepository.save(renderJob);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("articleId", article.getId());
        response.put("eventId", event.getId());
        response.put("jobId", renderJob.getId());
        response.put("status", "queued");
        return response;
    }

    @GetMapping("/jobs")
    public List<Map<String, Object>> listJobs() {
        return renderJobRepository.findAll().stream()
                .map(this::toJobResponse)
                .collect(Collectors.toList());
    }

    @PostMapping("/jobs/{jobId}/retry")
    public Map<String, Object> retryJob(@PathVariable Long jobId) {
        RenderJob job = renderJobRepository.findById(jobId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", "Job not found: " + jobId));

        job.setStatus(RenderJobStatus.QUEUED);
        job.setErrorMessage(null);
        job = renderJobRepository.save(job);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jobId", job.getId());
        response.put("status", job.getStatus().name());
        response.put("message", "retry queued");
        return response;
    }

    private Map<String, Object> toJobResponse(RenderJob job) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jobId", job.getId());
        response.put("eventId", job.getEvent() != null ? job.getEvent().getId() : null);
        response.put("stage", job.getStage() != null ? job.getStage().name() : null);
        response.put("status", job.getStatus() != null ? job.getStatus().name() : null);
        response.put("errorMessage", job.getErrorMessage());
        return response;
    }

    public record IngestRequest(String keyword, String url) {
    }
}
