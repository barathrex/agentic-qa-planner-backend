package com.qaassistant.controller;

import com.qaassistant.dto.*;
import com.qaassistant.service.QaPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
public class QaPlanController {

    private final QaPlanService qaPlanService;

    @PostMapping("/generate")
    public ResponseEntity<QaPlanResponse> generate(@Valid @RequestBody GenerateQaPlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(qaPlanService.generate(request));
    }

    @PostMapping("/save")
    public ResponseEntity<QaPlanResponse> save(@Valid @RequestBody SaveQaPlanRequest request) {
        return ResponseEntity.ok(qaPlanService.save(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QaPlanResponse> getPlan(@PathVariable Long id) {
        return ResponseEntity.ok(qaPlanService.getById(id));
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<List<VersionHistoryDto>> getVersions(@PathVariable Long id) {
        return ResponseEntity.ok(qaPlanService.getVersions(id));
    }

    @GetMapping("/{id}/versions/{versionNumber}")
    public ResponseEntity<QaPlanResponse> getVersion(
            @PathVariable Long id,
            @PathVariable Integer versionNumber
    ) {
        return ResponseEntity.ok(qaPlanService.getVersion(id, versionNumber));
    }

    @PutMapping("/testcase/{id}")
    public ResponseEntity<TestCaseDto> updateTestCase(
            @PathVariable Long id,
            @RequestBody UpdateTestCaseRequest request
    ) {
        return ResponseEntity.ok(qaPlanService.updateTestCase(id, request));
    }

    @PutMapping("/testcase/{id}/approve")
    public ResponseEntity<TestCaseDto> approveTestCase(@PathVariable Long id) {
        return ResponseEntity.ok(qaPlanService.approveTestCase(id));
    }

    @PutMapping("/testcase/{id}/reject")
    public ResponseEntity<TestCaseDto> rejectTestCase(@PathVariable Long id) {
        return ResponseEntity.ok(qaPlanService.rejectTestCase(id));
    }

    @PutMapping("/testcase/{id}/priority")
    public ResponseEntity<TestCaseDto> updatePriority(
            @PathVariable Long id,
            @Valid @RequestBody PriorityUpdateRequest request
    ) {
        return ResponseEntity.ok(qaPlanService.updatePriority(id, request.getPriority()));
    }
}
