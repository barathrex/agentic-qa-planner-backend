package com.qaassistant.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AcceptanceCriteriaDto {
    private Long id;
    private Integer criteriaIndex;
    private String description;
    private Boolean covered;
}
