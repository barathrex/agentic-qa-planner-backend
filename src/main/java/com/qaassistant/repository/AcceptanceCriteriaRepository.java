package com.qaassistant.repository;

import com.qaassistant.entity.AcceptanceCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcceptanceCriteriaRepository extends JpaRepository<AcceptanceCriteria, Long> {
    List<AcceptanceCriteria> findByPlanIdOrderByCriteriaIndexAsc(Long planId);
}
