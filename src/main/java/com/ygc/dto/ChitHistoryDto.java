package com.ygc.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChitHistoryDto {
    private Long id;
    private String chitName;
    private String finalStatus;
    private String closingReason;
    private LocalDateTime closedAt;
    private String analysisPdfPath;
}
