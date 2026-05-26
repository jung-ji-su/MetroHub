package com.metrohub.notification.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class NotificationDto {

    @Getter
    @Builder
    public static class Response {
        private Long id;
        private String type;
        private String title;
        private String body;
        private Long referenceId;
        private boolean isRead;
        private LocalDateTime createdAt;
    }
}
