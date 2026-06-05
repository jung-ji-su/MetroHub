package com.metrohub.api.domain.complaint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

public class ComplaintDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank
        private String category;
        @NotBlank @Size(max = 100)
        private String stationName;
        private String trainNo;
        private String lineCode;
        private String lineName;
        private String direction;
        private String destination;
        @NotBlank @Size(max = 2000)
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
        private String trainNo;
        private String lineCode;
        private String lineName;
        private String direction;
        private String destination;
        private String content;
        private String status;
        private LocalDateTime createdAt;
    }
}
