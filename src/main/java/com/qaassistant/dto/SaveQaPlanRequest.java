package com.qaassistant.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaveQaPlanRequest {
    @NotNull
    private Long planId;
}
