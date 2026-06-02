package com.metrohub.api.domain.complaint;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ComplaintMapper {
    void insert(Complaint complaint);
    Optional<Complaint> findById(@Param("id") Long id);
    List<Complaint> findByUserId(@Param("userId") Long userId);
    List<Complaint> findAll(@Param("status") String status, @Param("offset") int offset, @Param("size") int size);
    long countAll(@Param("status") String status);
    void updateStatus(@Param("id") Long id, @Param("status") String status);
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
