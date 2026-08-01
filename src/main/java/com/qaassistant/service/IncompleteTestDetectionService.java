package com.qaassistant.service;

import com.qaassistant.entity.GeneratedTestCase;
import com.qaassistant.entity.TestCaseStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class IncompleteTestDetectionService {

    public boolean isIncomplete(GeneratedTestCase testCase) {
        return isBlank(testCase.getTitle())
                || isBlank(testCase.getSteps())
                || isBlank(testCase.getExpectedResult());
    }

    public List<GeneratedTestCase> detectAndMarkIncomplete(List<GeneratedTestCase> testCases) {
        return findIncomplete(testCases, true);
    }

    public List<GeneratedTestCase> findIncompleteWithoutMutation(List<GeneratedTestCase> testCases) {
        return findIncomplete(testCases, false);
    }

    private List<GeneratedTestCase> findIncomplete(List<GeneratedTestCase> testCases, boolean mutate) {
        List<GeneratedTestCase> incomplete = new ArrayList<>();

        for (GeneratedTestCase testCase : testCases) {
            if (isIncomplete(testCase)) {
                if (mutate
                        && testCase.getStatus() != TestCaseStatus.APPROVED
                        && testCase.getStatus() != TestCaseStatus.REJECTED
                        && testCase.getStatus() != TestCaseStatus.POSSIBLE_DUPLICATE) {
                    testCase.setStatus(TestCaseStatus.INCOMPLETE);
                }
                incomplete.add(testCase);
            }
        }

        return incomplete;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
