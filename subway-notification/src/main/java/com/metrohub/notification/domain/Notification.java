package com.metrohub.notification.domain;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private Long id;
    private String type;
    private String title;
    private String body;
    private Long referenceId;
    private boolean isRead;
    private LocalDateTime createdAt;
}
