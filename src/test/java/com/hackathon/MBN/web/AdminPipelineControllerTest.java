package com.hackathon.MBN.web;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

class AdminPipelineControllerTest {

    @Test
    void supportsManualIngestAndJobRetry() {
        RawArticleRepository rawArticleRepository = mock(RawArticleRepository.class);
        EventRepository eventRepository = mock(EventRepository.class);
        RenderJobRepository renderJobRepository = mock(RenderJobRepository.class);
        SourceRepository sourceRepository = mock(SourceRepository.class);

        Source source = Source.builder().id(1L).name("Manual").endpoint("manual").sourceType(SourceType.NEWS_RSS).mapsToPinType(PinType.ORIGIN).build();
        Event event = Event.builder().id(10L).title("Manual event").category("manual").pinType(PinType.ORIGIN).build();
        RenderJob job = RenderJob.builder().id(20L).event(event).stage(RenderJobStage.INGEST).status(RenderJobStatus.QUEUED).build();

        when(sourceRepository.findAll()).thenReturn(List.of(source));
        when(rawArticleRepository.save(any(RawArticle.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(renderJobRepository.save(any(RenderJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(renderJobRepository.findAll()).thenReturn(List.of(job));
        when(renderJobRepository.findById(20L)).thenReturn(Optional.of(job));

        AdminPipelineController controller = new AdminPipelineController(rawArticleRepository, eventRepository, renderJobRepository, sourceRepository);

        Map<String, Object> ingest = controller.ingest(new AdminPipelineController.IngestRequest("BLACKPINK", null));
        List<Map<String, Object>> jobs = controller.listJobs();
        Map<String, Object> retried = controller.retryJob(20L);

        assertFalse(ingest.isEmpty());
        assertFalse(jobs.isEmpty());
        assertFalse(retried.isEmpty());
    }
}
