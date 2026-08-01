package com.qaassistant.repository;

import com.qaassistant.entity.GeneratedTestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneratedTestCaseRepository extends JpaRepository<GeneratedTestCase, Long> {
    List<GeneratedTestCase> findByPlanId(Long planId);
}
