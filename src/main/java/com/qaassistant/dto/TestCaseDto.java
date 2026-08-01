package com.qaassistant.dto;

import com.qaassistant.entity.TestCategory;
import com.qaassistant.entity.TestCaseStatus;
import com.qaassistant.entity.TestPriority;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TestCaseDto {
    private Long id;
    private String testId;
    private String title;
    private TestCategory category;
    private String preconditions;
    private String steps;
    private String expectedResult;
    private TestPriority priority;
    private String reason;
    private TestCaseStatus status;
    private Boolean approved;
    private List<Integer> mappedCriteriaIndices;
}
