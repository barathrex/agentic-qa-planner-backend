package com.qaassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class GenerateQaPlanRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotBlank(message = "Requirement is required")
    private String requirement;

    @NotEmpty(message = "At least one acceptance criterion is required")
    private List<@NotBlank String> acceptanceCriteria;

    @NotBlank(message = "Implementation summary is required")
    private String implementationSummary;

    private String developerName;
}
