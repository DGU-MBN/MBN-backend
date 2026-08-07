package com.hackathon.MBN.web;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hackathon.MBN.domain.Source;
import com.hackathon.MBN.domain.type.LicenseStatus;
import com.hackathon.MBN.domain.type.PinType;
import com.hackathon.MBN.domain.type.SourceStatus;
import com.hackathon.MBN.domain.type.SourceType;
import com.hackathon.MBN.repository.SourceRepository;

class AdminSourceControllerTest {

    @Test
    void managesSources() {
        SourceRepository sourceRepository = mock(SourceRepository.class);
        Source source = Source.builder()
                .id(1L)
                .sourceType(SourceType.NEWS_RSS)
                .name("Example")
                .endpoint("https://example.com")
                .pollIntervalMin(60)
                .mapsToPinType(PinType.ORIGIN)
                .licenseStatus(LicenseStatus.UNREVIEWED)
                .status(SourceStatus.ACTIVE)
                .build();

        when(sourceRepository.findAll()).thenReturn(List.of(source));
        when(sourceRepository.findById(1L)).thenReturn(Optional.of(source));
        when(sourceRepository.save(any(Source.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminSourceController controller = new AdminSourceController(sourceRepository);

        List<Map<String, Object>> sources = controller.listSources();
        Map<String, Object> created = controller.createSource(new AdminSourceController.SourceRequest(
                "NEWS_RSS", "Example", "https://example.com", 60, "ORIGIN", "UNREVIEWED", "ACTIVE"));
        Map<String, Object> updated = controller.updateSource(1L, new AdminSourceController.SourceRequest(
                null, "Updated", null, null, null, null, null));

        assertFalse(sources.isEmpty());
        assertFalse(created.isEmpty());
        assertFalse(updated.isEmpty());
    }
}
