package com.qaassistant.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UncoveredCriteriaDto {
    private Integer criteriaIndex;
    private String description;
}
