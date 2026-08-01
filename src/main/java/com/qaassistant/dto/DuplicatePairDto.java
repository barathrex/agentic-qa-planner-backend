package com.qaassistant.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DuplicatePairDto {
    private String testId1;
    private String testId2;
    private String title1;
    private String title2;
    private Double similarityScore;
}
