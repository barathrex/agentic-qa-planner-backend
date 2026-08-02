package com.qaassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaassistant.dto.ai.AiGeneratedPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class QaAiService {

    private static final String SYSTEM_PROMPT = """
            You are a QA Planning Assistant. Your role is to PROPOSE test cases for developer review.
            
            CRITICAL RULES:
            - NEVER state that a feature has passed QA, is approved, or is ready for release.
            - NEVER make final quality judgments. Only propose test cases.
            - Generate userFlows, testCases, edgeCases, permissionCases, failureStates, regressionAreas, and assumptions.
            - Provide clear section headers and titles for all scenario categories (Edge Cases, Permission Cases, Failure States, Regression Areas).
            - Every test case MUST include: testId, title, category, preconditions, steps, expectedResult, priority (HIGH/MEDIUM/LOW), reason, mappedAcceptanceCriteria (1-based indices).
            - Map each test case to one or more acceptance criteria by index (AC1 = index 1, AC2 = index 2, etc.).
            - Identify main user flows as a list of flow step descriptions.
            - Generate edgeCases, permissionCases, failureStates, regressionAreas as descriptive test scenario strings.
            - If information is missing, list assumptions explicitly.
            
            Valid categories: UNIT_TESTS, API_TESTS, INTEGRATION_TESTS, END_TO_END_TESTS, PLAYWRIGHT_TESTS, MANUAL_TESTS, EDGE_CASES, PERMISSION_CASES, FAILURE_STATES, REGRESSION_AREAS
            
            Respond ONLY with valid JSON matching this structure:
            {
              "userFlows": ["step1", "step2"],
              "testCases": [{
                "testId": "TC-001",
                "title": "...",
                "category": "API_TESTS",
                "preconditions": "...",
                "steps": "...",
                "expectedResult": "...",
                "priority": "HIGH",
                "reason": "...",
                "mappedAcceptanceCriteria": [1, 2]
              }],
              "edgeCases": ["..."],
              "permissionCases": ["..."],
              "failureStates": ["..."],
              "regressionAreas": ["..."],
              "assumptions": ["..."]
            }
            """;

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    public AiGeneratedPlan generatePlan(
            String requirement,
            List<String> acceptanceCriteria,
            String implementationSummary,
            String retrievedGuidance
    ) {
        String acFormatted = IntStream.range(0, acceptanceCriteria.size())
                .mapToObj(i -> "AC" + (i + 1) + ": " + acceptanceCriteria.get(i))
                .collect(Collectors.joining("\n"));

        String userPrompt = String.format("""
                ## Requirement
                %s
                
                ## Acceptance Criteria
                %s
                
                ## Implementation Summary
                %s
                
                ## Retrieved QA Guidelines
                %s
                
                Generate a comprehensive proposed QA plan as JSON.
                """, requirement, acFormatted, implementationSummary, retrievedGuidance);

        ChatClient chatClient = chatClientBuilder.build();

        String response = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(userPrompt)
                .call()
                .content();

        return parseAiResponse(response);
    }

    private AiGeneratedPlan parseAiResponse(String response) {
        try {
            String json = extractJson(response);
            return objectMapper.readValue(json, AiGeneratedPlan.class);
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", response, e);
            throw new RuntimeException("Failed to parse AI-generated QA plan. Please try again.", e);
        }
    }

    private String extractJson(String response) {
        int start = response.indexOf('{');
        int end = response.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return response;
    }
}
