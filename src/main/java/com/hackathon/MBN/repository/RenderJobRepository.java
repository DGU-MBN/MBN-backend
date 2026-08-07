package com.hackathon.MBN.repository;

import com.hackathon.MBN.domain.RenderJob;
import com.hackathon.MBN.domain.type.RenderJobStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RenderJobRepository extends JpaRepository<RenderJob, Long> {
    List<RenderJob> findByStatus(RenderJobStatus status);
}
