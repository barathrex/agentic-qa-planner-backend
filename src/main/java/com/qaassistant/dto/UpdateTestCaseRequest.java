package com.qaassistant.dto;

import com.qaassistant.entity.TestCaseStatus;
import com.qaassistant.entity.TestPriority;
import lombok.Data;

@Data
public class UpdateTestCaseRequest {
    private String title;
    private String preconditions;
    private String steps;
    private String expectedResult;
    private TestPriority priority;
    private String reason;
    private TestCaseStatus status;
}
