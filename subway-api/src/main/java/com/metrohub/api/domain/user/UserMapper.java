package com.metrohub.api.domain.user;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserMapper {
    void insert(User user);
    Optional<User> findByNickname(@Param("nickname") String nickname);
    Optional<User> findById(@Param("id") Long id);
    boolean existsByNickname(@Param("nickname") String nickname);
}
