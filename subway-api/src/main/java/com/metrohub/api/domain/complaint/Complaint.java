package com.metrohub.api.domain.complaint;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Complaint {
    private Long id;
    private Long userId;
    private String category;
    private String stationName;
    private String content;
    private String status;
    private LocalDateTime createdAt;
}
