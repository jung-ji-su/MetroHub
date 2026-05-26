package com.metrohub.api.domain.complaint;

import lombok.*;

import java.time.LocalDateTime;

public class ComplaintDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String category;
        private String stationName;
        private String content;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String category;
        private String stationName;
        private String content;
        private String status;
        private LocalDateTime createdAt;
    }
}
