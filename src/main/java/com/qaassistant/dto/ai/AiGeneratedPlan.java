package com.qaassistant.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiGeneratedPlan {

    private List<String> userFlows;
    private List<AiTestCase> testCases;
    private List<String> edgeCases;
    private List<String> permissionCases;
    private List<String> failureStates;
    private List<String> regressionAreas;
    private List<String> assumptions;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AiTestCase {
        private String testId;
        private String title;
        private String category;
        private String preconditions;
        private String steps;
        private String expectedResult;
        private String priority;
        private String reason;
        private List<Integer> mappedAcceptanceCriteria;
    }
}
