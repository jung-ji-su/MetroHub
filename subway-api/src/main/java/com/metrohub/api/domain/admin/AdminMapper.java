package com.metrohub.api.domain.admin;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface AdminMapper {
    long countTotalUsers();
    long countTodayUsers();
    long countTotalPosts();
    long countTodayPosts();
    long countTotalComments();
    long countTotalComplaints();
    List<Map<String, Object>> countComplaintsByStatus();
    List<AdminDto.LinePostCount> countPostsByLine();
    List<AdminDto.RecentUser> findRecentUsers();
    List<AdminDto.RecentComplaint> findRecentComplaints();
}
