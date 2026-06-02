package com.metrohub.api.domain.user;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface RefreshTokenMapper {
    void insert(RefreshToken refreshToken);
    Optional<RefreshToken> findByToken(@Param("token") String token);
    void deleteByToken(@Param("token") String token);
    void deleteByUserId(@Param("userId") Long userId);
}
