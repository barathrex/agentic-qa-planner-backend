package com.qaassistant.dto;

import com.qaassistant.entity.TestPriority;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PriorityUpdateRequest {
    @NotNull
    private TestPriority priority;
}
