package com.qaassistant.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class QaPlanResponse {
    private Long id;
    private String developerName;
    private String title;
    private String description;
    private String requirement;
    private String implementationSummary;
    private List<String> userFlows;
    private String retrievedGuidance;
    private List<String> assumptions;
    private List<AcceptanceCriteriaDto> acceptanceCriteria;
    private List<TestCaseDto> testCases;
    private Double coveragePercentage;
    private List<UncoveredCriteriaDto> uncoveredCriteria;
    private List<DuplicatePairDto> duplicateTestCases;
    private List<TestCaseDto> incompleteTestCases;
    private Integer currentVersion;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private String disclaimer;
}
