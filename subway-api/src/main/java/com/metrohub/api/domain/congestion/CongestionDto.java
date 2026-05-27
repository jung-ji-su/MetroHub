package com.metrohub.api.domain.congestion;

import lombok.*;

import java.time.LocalDateTime;

public class CongestionDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String lineNumber;
        private String stationName;
        private Integer congestionLevel;
        private String trainNo;
        private String arrivalMessage;
        private LocalDateTime updatedAt;
    }
}
