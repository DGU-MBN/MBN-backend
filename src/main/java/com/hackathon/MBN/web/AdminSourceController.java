package com.hackathon.MBN.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.MBN.domain.Source;
import com.hackathon.MBN.domain.type.LicenseStatus;
import com.hackathon.MBN.domain.type.PinType;
import com.hackathon.MBN.domain.type.SourceStatus;
import com.hackathon.MBN.domain.type.SourceType;
import com.hackathon.MBN.repository.SourceRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/sources")
@RequiredArgsConstructor
public class AdminSourceController {

    private final SourceRepository sourceRepository;

    @GetMapping
    public List<Map<String, Object>> listSources() {
        return sourceRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @PostMapping
    public Map<String, Object> createSource(@RequestBody SourceRequest request) {
        Source source = Source.builder()
                .sourceType(SourceType.valueOf(request.sourceType().toUpperCase()))
                .name(request.name())
                .endpoint(request.endpoint())
                .pollIntervalMin(request.pollIntervalMin())
                .mapsToPinType(PinType.valueOf(request.mapsToPinType().toUpperCase()))
                .licenseStatus(request.licenseStatus() != null
                        ? LicenseStatus.valueOf(request.licenseStatus().toUpperCase())
                        : LicenseStatus.UNREVIEWED)
                .status(request.status() != null
                        ? SourceStatus.valueOf(request.status().toUpperCase())
                        : SourceStatus.ACTIVE)
                .build();

        return toResponse(sourceRepository.save(source));
    }

    @PutMapping("/{sourceId}")
    public Map<String, Object> updateSource(@PathVariable Long sourceId, @RequestBody SourceRequest request) {
        Source source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SOURCE_NOT_FOUND", "Source not found: " + sourceId));

        if (request.sourceType() != null) {
            source.setSourceType(SourceType.valueOf(request.sourceType().toUpperCase()));
        }
        if (request.name() != null) {
            source.setName(request.name());
        }
        if (request.endpoint() != null) {
            source.setEndpoint(request.endpoint());
        }
        if (request.pollIntervalMin() != null) {
            source.setPollIntervalMin(request.pollIntervalMin());
        }
        if (request.mapsToPinType() != null) {
            source.setMapsToPinType(PinType.valueOf(request.mapsToPinType().toUpperCase()));
        }
        if (request.licenseStatus() != null) {
            source.setLicenseStatus(LicenseStatus.valueOf(request.licenseStatus().toUpperCase()));
        }
        if (request.status() != null) {
            source.setStatus(SourceStatus.valueOf(request.status().toUpperCase()));
        }

        return toResponse(sourceRepository.save(source));
    }

    private Map<String, Object> toResponse(Source source) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", source.getId());
        response.put("sourceType", source.getSourceType() != null ? source.getSourceType().name() : null);
        response.put("name", source.getName());
        response.put("endpoint", source.getEndpoint());
        response.put("pollIntervalMin", source.getPollIntervalMin());
        response.put("mapsToPinType", source.getMapsToPinType() != null ? source.getMapsToPinType().name() : null);
        response.put("licenseStatus", source.getLicenseStatus() != null ? source.getLicenseStatus().name() : null);
        response.put("status", source.getStatus() != null ? source.getStatus().name() : null);
        return response;
    }

    public record SourceRequest(
            String sourceType,
            String name,
            String endpoint,
            Integer pollIntervalMin,
            String mapsToPinType,
            String licenseStatus,
            String status) {
    }
}
