package com.qaassistant.service;

import com.qaassistant.dto.DuplicatePairDto;
import com.qaassistant.entity.GeneratedTestCase;
import com.qaassistant.entity.TestCaseStatus;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DuplicateDetectionService {

    private static final double SIMILARITY_THRESHOLD = 0.75;

    public List<DuplicatePairDto> detectDuplicates(List<GeneratedTestCase> testCases) {
        return findDuplicates(testCases, true);
    }

    public List<DuplicatePairDto> findDuplicatesWithoutMutation(List<GeneratedTestCase> testCases) {
        return findDuplicates(testCases, false);
    }

    private List<DuplicatePairDto> findDuplicates(List<GeneratedTestCase> testCases, boolean mutate) {
        List<DuplicatePairDto> duplicates = new ArrayList<>();

        for (int i = 0; i < testCases.size(); i++) {
            for (int j = i + 1; j < testCases.size(); j++) {
                GeneratedTestCase a = testCases.get(i);
                GeneratedTestCase b = testCases.get(j);
                double similarity = calculateSimilarity(a, b);

                if (similarity >= SIMILARITY_THRESHOLD) {
                    duplicates.add(DuplicatePairDto.builder()
                            .testId1(a.getTestId())
                            .testId2(b.getTestId())
                            .title1(a.getTitle())
                            .title2(b.getTitle())
                            .similarityScore(Math.round(similarity * 100.0) / 100.0)
                            .build());

                    if (mutate) {
                        if (a.getStatus() != TestCaseStatus.APPROVED && a.getStatus() != TestCaseStatus.REJECTED) {
                            a.setStatus(TestCaseStatus.POSSIBLE_DUPLICATE);
                        }
                        if (b.getStatus() != TestCaseStatus.APPROVED && b.getStatus() != TestCaseStatus.REJECTED) {
                            b.setStatus(TestCaseStatus.POSSIBLE_DUPLICATE);
                        }
                    }
                }
            }
        }

        return duplicates;
    }

    public double calculateSimilarity(GeneratedTestCase a, GeneratedTestCase b) {
        String textA = normalize(a.getTitle() + " " + nullSafe(a.getSteps()) + " " + nullSafe(a.getExpectedResult()));
        String textB = normalize(b.getTitle() + " " + nullSafe(b.getSteps()) + " " + nullSafe(b.getExpectedResult()));

        Set<String> tokensA = tokenize(textA);
        Set<String> tokensB = tokenize(textB);

        if (tokensA.isEmpty() && tokensB.isEmpty()) {
            return 1.0;
        }
        if (tokensA.isEmpty() || tokensB.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(tokensA);
        intersection.retainAll(tokensB);
        Set<String> union = new HashSet<>(tokensA);
        union.addAll(tokensB);

        return (double) intersection.size() / union.size();
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream(text.split("\\s+"))
                .filter(t -> t.length() > 2)
                .collect(Collectors.toSet());
    }

    private String normalize(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ").trim();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
