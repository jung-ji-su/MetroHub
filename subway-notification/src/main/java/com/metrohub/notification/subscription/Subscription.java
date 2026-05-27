package com.metrohub.notification.subscription;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {
    private Long id;
    private Long userId;
    private String subType;   // LINE | STATION
    private String subValue;  // 노선코드 or 역명
    private LocalDateTime createdAt;
}
