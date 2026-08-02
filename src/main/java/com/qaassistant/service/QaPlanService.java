package com.qaassistant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qaassistant.dto.*;
import com.qaassistant.dto.ai.AiGeneratedPlan;
import com.qaassistant.entity.*;
import com.qaassistant.repository.GeneratedTestCaseRepository;
import com.qaassistant.repository.QaPlanRepository;
import com.qaassistant.repository.QaPlanVersionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class QaPlanService {

    public static final String DISCLAIMER =
            "This is a proposed QA plan for developer review only. " +
            "It does not indicate that the feature has passed QA, is approved, or is ready for release.";

    private final QaPlanRepository qaPlanRepository;
    private final QaPlanVersionRepository versionRepository;
    private final GeneratedTestCaseRepository testCaseRepository;
    private final KnowledgeBaseService knowledgeBaseService;
    private final QaAiService qaAiService;
    private final CoverageService coverageService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final IncompleteTestDetectionService incompleteTestDetectionService;
    private final ObjectMapper objectMapper;

    @Transactional
    public QaPlanResponse generate(GenerateQaPlanRequest request, String developerName) {
        String guidance = knowledgeBaseService.retrieveRelevantGuidance(
                request.getRequirement(),
                request.getImplementationSummary()
        );

        AiGeneratedPlan aiPlan = qaAiService.generatePlan(
                request.getRequirement(),
                request.getAcceptanceCriteria(),
                request.getImplementationSummary(),
                guidance
        );

        QaPlan plan = QaPlan.builder()
                .developerName(developerName != null ? developerName : request.getDeveloperName())
                .title(request.getTitle())
                .description(request.getDescription())
                .requirement(request.getRequirement())
                .implementationSummary(request.getImplementationSummary())
                .userFlows(serializeList(aiPlan.getUserFlows()))
                .retrievedGuidance(guidance)
                .assumptions(serializeList(aiPlan.getAssumptions()))
                .currentVersion(1)
                .build();

        List<AcceptanceCriteria> criteria = IntStream.range(0, request.getAcceptanceCriteria().size())
                .mapToObj(i -> AcceptanceCriteria.builder()
                        .plan(plan)
                        .description(request.getAcceptanceCriteria().get(i))
                        .criteriaIndex(i + 1)
                        .build())
                .toList();
        plan.getAcceptanceCriteria().addAll(criteria);

        Map<Integer, AcceptanceCriteria> criteriaByIndex = criteria.stream()
                .collect(Collectors.toMap(AcceptanceCriteria::getCriteriaIndex, ac -> ac));

        List<GeneratedTestCase> testCases = new ArrayList<>();
        int counter = 1;

        if (aiPlan.getTestCases() != null) {
            for (AiGeneratedPlan.AiTestCase aiTc : aiPlan.getTestCases()) {
                testCases.add(buildTestCase(plan, aiTc, criteriaByIndex, counter++));
            }
        }

        counter = addScenarioTests(testCases, plan, aiPlan.getEdgeCases(), TestCategory.EDGE_CASES, counter);
        counter = addScenarioTests(testCases, plan, aiPlan.getPermissionCases(), TestCategory.PERMISSION_CASES, counter);
        counter = addScenarioTests(testCases, plan, aiPlan.getFailureStates(), TestCategory.FAILURE_STATES, counter);
        addScenarioTests(testCases, plan, aiPlan.getRegressionAreas(), TestCategory.REGRESSION_AREAS, counter);

        plan.getTestCases().addAll(testCases);

        qaPlanRepository.save(plan);

        applyDeterministicAnalysis(plan);

        QaPlanVersion version = QaPlanVersion.builder()
                .plan(plan)
                .versionNumber(1)
                .snapshotJson(snapshotPlan(plan))
                .updatedDate(LocalDateTime.now())
                .build();
        plan.getVersions().add(version);
        qaPlanRepository.save(plan);

        return toResponse(plan);
    }

    @Transactional(readOnly = true)
    public List<QaPlanResponse> getPlansByDeveloper(String developerName, String searchQuery) {
        List<QaPlan> plans;
        if (searchQuery != null && !searchQuery.isBlank()) {
            plans = qaPlanRepository.searchDeveloperPlans(developerName, searchQuery.trim());
        } else {
            plans = qaPlanRepository.findByDeveloperNameOrderByCreatedDateDesc(developerName);
        }
        return plans.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void deletePlan(Long id, String developerName) {
        QaPlan plan = findPlan(id);
        if (developerName != null && !developerName.equalsIgnoreCase(plan.getDeveloperName())) {
            throw new IllegalArgumentException("Cannot delete plan owned by another developer");
        }
        qaPlanRepository.delete(plan);
    }

    @Transactional
    public QaPlanResponse save(SaveQaPlanRequest request) {
        QaPlan plan = findPlan(request.getPlanId());
        int newVersion = plan.getCurrentVersion() + 1;
        plan.setCurrentVersion(newVersion);

        QaPlanVersion version = QaPlanVersion.builder()
                .plan(plan)
                .versionNumber(newVersion)
                .snapshotJson(snapshotPlan(plan))
                .updatedDate(LocalDateTime.now())
                .build();
        plan.getVersions().add(version);

        applyDeterministicAnalysis(plan);
        qaPlanRepository.save(plan);

        return toResponse(plan);
    }

    @Transactional(readOnly = true)
    public QaPlanResponse getById(Long id) {
        return toResponse(findPlan(id));
    }

    @Transactional(readOnly = true)
    public List<VersionHistoryDto> getVersions(Long planId) {
        findPlan(planId);
        return versionRepository.findByPlanIdOrderByVersionNumberDesc(planId).stream()
                .map(v -> VersionHistoryDto.builder()
                        .id(v.getId())
                        .versionNumber(v.getVersionNumber())
                        .createdDate(v.getCreatedDate())
                        .updatedDate(v.getUpdatedDate())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public QaPlanResponse getVersion(Long planId, Integer versionNumber) {
        QaPlanVersion version = versionRepository.findByPlanIdAndVersionNumber(planId, versionNumber)
                .orElseThrow(() -> new EntityNotFoundException("Version not found"));

        try {
            return objectMapper.readValue(version.getSnapshotJson(), QaPlanResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to load version snapshot", e);
        }
    }

    @Transactional
    public TestCaseDto updateTestCase(Long testCaseId, UpdateTestCaseRequest request) {
        GeneratedTestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new EntityNotFoundException("Test case not found"));

        if (request.getTitle() != null) testCase.setTitle(request.getTitle());
        if (request.getPreconditions() != null) testCase.setPreconditions(request.getPreconditions());
        if (request.getSteps() != null) testCase.setSteps(request.getSteps());
        if (request.getExpectedResult() != null) testCase.setExpectedResult(request.getExpectedResult());
        if (request.getPriority() != null) testCase.setPriority(request.getPriority());
        if (request.getReason() != null) testCase.setReason(request.getReason());
        if (request.getStatus() != null) testCase.setStatus(request.getStatus());

        testCaseRepository.save(testCase);
        QaPlan plan = testCase.getPlan();
        applyDeterministicAnalysis(plan);
        qaPlanRepository.save(plan);

        return toTestCaseDto(testCase, coverageService.getCoveredCriteriaIds(plan.getTestCases()));
    }

    @Transactional
    public TestCaseDto approveTestCase(Long testCaseId) {
        GeneratedTestCase testCase = findTestCase(testCaseId);
        testCase.setApproved(true);
        testCase.setStatus(TestCaseStatus.APPROVED);
        testCaseRepository.save(testCase);
        return toTestCaseDto(testCase, Set.of());
    }

    @Transactional
    public TestCaseDto rejectTestCase(Long testCaseId) {
        GeneratedTestCase testCase = findTestCase(testCaseId);
        testCase.setApproved(false);
        testCase.setStatus(TestCaseStatus.REJECTED);
        testCaseRepository.save(testCase);
        return toTestCaseDto(testCase, Set.of());
    }

    @Transactional
    public TestCaseDto updatePriority(Long testCaseId, TestPriority priority) {
        GeneratedTestCase testCase = findTestCase(testCaseId);
        testCase.setPriority(priority);
        testCaseRepository.save(testCase);
        return toTestCaseDto(testCase, Set.of());
    }

    private void applyDeterministicAnalysis(QaPlan plan) {
        List<GeneratedTestCase> testCases = plan.getTestCases();
        incompleteTestDetectionService.detectAndMarkIncomplete(testCases);
        duplicateDetectionService.detectDuplicates(testCases);
        plan.setCoveragePercentage(coverageService.calculateCoverage(plan.getAcceptanceCriteria(), testCases));
    }

    private GeneratedTestCase buildTestCase(
            QaPlan plan,
            AiGeneratedPlan.AiTestCase aiTc,
            Map<Integer, AcceptanceCriteria> criteriaByIndex,
            int counter
    ) {
        GeneratedTestCase testCase = GeneratedTestCase.builder()
                .plan(plan)
                .testId(aiTc.getTestId() != null ? aiTc.getTestId() : "TC-" + String.format("%03d", counter))
                .title(aiTc.getTitle())
                .category(parseCategory(aiTc.getCategory()))
                .preconditions(aiTc.getPreconditions())
                .steps(aiTc.getSteps())
                .expectedResult(aiTc.getExpectedResult())
                .priority(parsePriority(aiTc.getPriority()))
                .reason(aiTc.getReason())
                .status(TestCaseStatus.PROPOSED)
                .approved(false)
                .build();

        if (aiTc.getMappedAcceptanceCriteria() != null) {
            for (Integer index : aiTc.getMappedAcceptanceCriteria()) {
                AcceptanceCriteria ac = criteriaByIndex.get(index);
                if (ac != null) {
                    TestCaseMapping mapping = TestCaseMapping.builder()
                            .testCase(testCase)
                            .acceptanceCriteria(ac)
                            .build();
                    testCase.getMappings().add(mapping);
                }
            }
        }

        return testCase;
    }

    private int addScenarioTests(
            List<GeneratedTestCase> testCases,
            QaPlan plan,
            List<String> scenarios,
            TestCategory category,
            int counter
    ) {
        if (scenarios == null) return counter;
        for (String scenario : scenarios) {
            testCases.add(GeneratedTestCase.builder()
                    .plan(plan)
                    .testId("TC-" + String.format("%03d", counter++))
                    .title(scenario)
                    .category(category)
                    .preconditions("As applicable for the scenario")
                    .steps("Execute scenario: " + scenario)
                    .expectedResult("System handles scenario appropriately")
                    .priority(TestPriority.MEDIUM)
                    .reason("Covers " + category.name().toLowerCase().replace('_', ' ') + " identified during QA planning")
                    .status(TestCaseStatus.PROPOSED)
                    .approved(false)
                    .build());
        }
        return counter;
    }

    private TestCategory parseCategory(String category) {
        if (category == null) return TestCategory.MANUAL_TESTS;
        try {
            return TestCategory.valueOf(category.toUpperCase().replace(" ", "_").replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return TestCategory.MANUAL_TESTS;
        }
    }

    private TestPriority parsePriority(String priority) {
        if (priority == null) return TestPriority.MEDIUM;
        try {
            return TestPriority.valueOf(priority.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TestPriority.MEDIUM;
        }
    }

    private QaPlan findPlan(Long id) {
        return qaPlanRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("QA plan not found: " + id));
    }

    private GeneratedTestCase findTestCase(Long id) {
        return testCaseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Test case not found: " + id));
    }

    private QaPlanResponse toResponse(QaPlan plan) {
        List<GeneratedTestCase> testCases = plan.getTestCases();
        Set<Long> coveredIds = coverageService.getCoveredCriteriaIds(testCases);
        List<DuplicatePairDto> duplicates = duplicateDetectionService.findDuplicatesWithoutMutation(testCases);
        List<GeneratedTestCase> incomplete = incompleteTestDetectionService.findIncompleteWithoutMutation(testCases);

        return QaPlanResponse.builder()
                .id(plan.getId())
                .developerName(plan.getDeveloperName())
                .title(plan.getTitle())
                .description(plan.getDescription())
                .requirement(plan.getRequirement())
                .implementationSummary(plan.getImplementationSummary())
                .userFlows(deserializeList(plan.getUserFlows()))
                .retrievedGuidance(plan.getRetrievedGuidance())
                .assumptions(deserializeList(plan.getAssumptions()))
                .acceptanceCriteria(plan.getAcceptanceCriteria().stream()
                        .map(ac -> AcceptanceCriteriaDto.builder()
                                .id(ac.getId())
                                .criteriaIndex(ac.getCriteriaIndex())
                                .description(ac.getDescription())
                                .covered(coveredIds.contains(ac.getId()))
                                .build())
                        .toList())
                .testCases(testCases.stream()
                        .map(tc -> toTestCaseDto(tc, coveredIds))
                        .toList())
                .coveragePercentage(plan.getCoveragePercentage())
                .uncoveredCriteria(coverageService.findUncoveredCriteria(plan.getAcceptanceCriteria(), testCases))
                .duplicateTestCases(duplicates)
                .incompleteTestCases(incomplete.stream()
                        .map(tc -> toTestCaseDto(tc, coveredIds))
                        .toList())
                .currentVersion(plan.getCurrentVersion())
                .createdDate(plan.getCreatedDate())
                .updatedDate(plan.getUpdatedDate())
                .disclaimer(DISCLAIMER)
                .build();
    }

    private TestCaseDto toTestCaseDto(GeneratedTestCase tc, Set<Long> coveredIds) {
        return TestCaseDto.builder()
                .id(tc.getId())
                .testId(tc.getTestId())
                .title(tc.getTitle())
                .category(tc.getCategory())
                .preconditions(tc.getPreconditions())
                .steps(tc.getSteps())
                .expectedResult(tc.getExpectedResult())
                .priority(tc.getPriority())
                .reason(tc.getReason())
                .status(tc.getStatus())
                .approved(tc.getApproved())
                .mappedCriteriaIndices(tc.getMappings().stream()
                        .map(m -> m.getAcceptanceCriteria().getCriteriaIndex())
                        .toList())
                .build();
    }

    private String snapshotPlan(QaPlan plan) {
        try {
            return objectMapper.writeValueAsString(toResponse(plan));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to snapshot plan", e);
        }
    }

    private String serializeList(List<String> items) {
        if (items == null) return "[]";
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> deserializeList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
