package com.metrohub.api.domain.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final AdminMapper adminMapper;

    @Transactional(readOnly = true)
    public AdminDto.DashboardResponse getDashboard() {
        Map<String, Long> byStatus = adminMapper.countComplaintsByStatus()
                .stream()
                .collect(Collectors.toMap(
                        m -> (String) m.get("status"),
                        m -> ((Number) m.get("cnt")).longValue()
                ));

        return AdminDto.DashboardResponse.builder()
                .totalUsers(adminMapper.countTotalUsers())
                .todayNewUsers(adminMapper.countTodayUsers())
                .totalPosts(adminMapper.countTotalPosts())
                .todayNewPosts(adminMapper.countTodayPosts())
                .totalComments(adminMapper.countTotalComments())
                .totalComplaints(adminMapper.countTotalComplaints())
                .complaintsByStatus(byStatus)
                .postsByLine(adminMapper.countPostsByLine())
                .recentUsers(adminMapper.findRecentUsers())
                .recentComplaints(adminMapper.findRecentComplaints())
                .build();
    }
}
