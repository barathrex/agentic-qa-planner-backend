package com.qaassistant.controller;

import com.qaassistant.dto.*;
import com.qaassistant.service.PdfExportService;
import com.qaassistant.service.QaPlanService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
public class QaPlanController {

    private final QaPlanService qaPlanService;
    private final PdfExportService pdfExportService;

    @PostMapping("/generate")
    public ResponseEntity<QaPlanResponse> generate(
            @Valid @RequestBody GenerateQaPlanRequest request,
            HttpServletRequest servletRequest
    ) {
        String developerName = (String) servletRequest.getAttribute("developerName");
        return ResponseEntity.status(HttpStatus.CREATED).body(qaPlanService.generate(request, developerName));
    }

    @GetMapping
    public ResponseEntity<List<QaPlanResponse>> getPlans(
            @RequestParam(required = false) String search,
            HttpServletRequest servletRequest
    ) {
        String developerName = (String) servletRequest.getAttribute("developerName");
        return ResponseEntity.ok(qaPlanService.getPlansByDeveloper(developerName, search));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(
            @PathVariable Long id,
            HttpServletRequest servletRequest
    ) {
        String developerName = (String) servletRequest.getAttribute("developerName");
        qaPlanService.deletePlan(id, developerName);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<InputStreamResource> downloadPdf(@PathVariable Long id) {
        QaPlanResponse plan = qaPlanService.getById(id);
        ByteArrayInputStream pdfStream = pdfExportService.generatePdf(plan);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=QA_Plan_" + id + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdfStream));
    }

    @PostMapping("/save")
    public ResponseEntity<QaPlanResponse> save(@Valid @RequestBody SaveQaPlanRequest request) {
        return ResponseEntity.ok(qaPlanService.save(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QaPlanResponse> getPlan(@PathVariable Long id) {
        return ResponseEntity.ok(qaPlanService.getById(id));
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
