package com.metrohub.api.domain.admin;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AdminDto {

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DashboardResponse {
        private long totalUsers;
        private long todayNewUsers;
        private long totalPosts;
        private long todayNewPosts;
        private long totalComments;
        private long totalComplaints;
        private Map<String, Long> complaintsByStatus;
        private List<LinePostCount> postsByLine;
        private List<RecentUser> recentUsers;
        private List<RecentComplaint> recentComplaints;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class LinePostCount {
        private String lineNumber;
        private long count;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RecentUser {
        private Long id;
        private String nickname;
        private LocalDateTime createdAt;
    }

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RecentComplaint {
        private Long id;
        private String category;
        private String stationName;
        private String status;
        private LocalDateTime createdAt;
    }
}
