package com.qaassistant.repository;

import com.qaassistant.entity.QaPlanVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QaPlanVersionRepository extends JpaRepository<QaPlanVersion, Long> {
    List<QaPlanVersion> findByPlanIdOrderByVersionNumberDesc(Long planId);
    Optional<QaPlanVersion> findByPlanIdAndVersionNumber(Long planId, Integer versionNumber);
}
