package com.metrohub.api.domain.congestion;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Congestion {
    private Long id;
    private String lineNumber;
    private String stationName;
    private Integer congestionLevel;
    private String trainNo;
    private LocalDateTime updatedAt;
}
