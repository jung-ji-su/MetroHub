package com.metrohub.api.domain.user;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String email;
    private String password;
    private String nickname;
    private String role; // USER | ADMIN
    private LocalDateTime createdAt;
}
