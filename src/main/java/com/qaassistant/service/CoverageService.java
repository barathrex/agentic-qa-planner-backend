package com.qaassistant.service;

import com.qaassistant.dto.UncoveredCriteriaDto;
import com.qaassistant.entity.AcceptanceCriteria;
import com.qaassistant.entity.GeneratedTestCase;
import com.qaassistant.entity.TestCaseMapping;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CoverageService {

    public double calculateCoverage(List<AcceptanceCriteria> criteria, List<GeneratedTestCase> testCases) {
        if (criteria.isEmpty()) {
            return 0.0;
        }

        Set<Long> coveredIds = testCases.stream()
                .flatMap(tc -> tc.getMappings().stream())
                .map(m -> m.getAcceptanceCriteria().getId())
                .collect(Collectors.toSet());

        long coveredCount = criteria.stream()
                .filter(ac -> coveredIds.contains(ac.getId()))
                .count();

        return Math.round((coveredCount * 100.0 / criteria.size()) * 100.0) / 100.0;
    }

    public List<UncoveredCriteriaDto> findUncoveredCriteria(
            List<AcceptanceCriteria> criteria,
            List<GeneratedTestCase> testCases
    ) {
        Set<Long> coveredIds = testCases.stream()
                .flatMap(tc -> tc.getMappings().stream())
                .map(m -> m.getAcceptanceCriteria().getId())
                .collect(Collectors.toSet());

        return criteria.stream()
                .filter(ac -> !coveredIds.contains(ac.getId()))
                .map(ac -> UncoveredCriteriaDto.builder()
                        .criteriaIndex(ac.getCriteriaIndex())
                        .description(ac.getDescription())
                        .build())
                .toList();
    }

    public Set<Long> getCoveredCriteriaIds(List<GeneratedTestCase> testCases) {
        return testCases.stream()
                .flatMap(tc -> tc.getMappings().stream())
                .map(m -> m.getAcceptanceCriteria().getId())
                .collect(Collectors.toSet());
    }
}
