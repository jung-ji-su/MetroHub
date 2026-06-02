package com.metrohub.api.domain.community;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

public class CommunityDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostCreateRequest {
        @NotBlank
        private String lineNumber;
        @NotBlank @Size(max = 100)
        private String title;
        @NotBlank @Size(max = 2000)
        private String content;
        private boolean alert;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostResponse {
        private Long id;
        private String lineNumber;
        private String title;
        private String content;
        private boolean alert;
        private String authorNickname;
        private LocalDateTime createdAt;
        private int likeCount;
        private boolean likedByMe;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LikeResponse {
        private Long postId;
        private int likeCount;
        private boolean liked;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentCreateRequest {
        @NotBlank @Size(max = 1000)
        private String content;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentResponse {
        private Long id;
        private Long postId;
        private Long authorId;
        private String content;
        private String authorNickname;
        private LocalDateTime createdAt;
    }
}
