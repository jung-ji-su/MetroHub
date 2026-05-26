package com.metrohub.api.domain.community;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommunityPost {
    private Long id;
    private String lineNumber;
    private Long userId;
    private String title;
    private String content;
    private Boolean alert;
    private LocalDateTime createdAt;
    private String authorNickname;
}
