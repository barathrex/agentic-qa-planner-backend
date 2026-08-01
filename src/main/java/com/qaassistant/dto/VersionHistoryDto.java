package com.qaassistant.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VersionHistoryDto {
    private Long id;
    private Integer versionNumber;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
