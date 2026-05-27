package com.metrohub.api.domain.community;

import lombok.*;

import java.time.LocalDateTime;

public class CommunityDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostCreateRequest {
        private String lineNumber;
        private String title;
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
        private String content;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommentResponse {
        private Long id;
        private Long postId;
        private String content;
        private String authorNickname;
        private LocalDateTime createdAt;
    }
}
