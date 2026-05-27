package com.metrohub.notification.subscription;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SubscriptionMapper {

    void insert(Subscription subscription);

    void delete(@Param("id") Long id, @Param("userId") Long userId);

    List<Subscription> findByUserId(@Param("userId") Long userId);

    List<Long> findUserIdsByLine(@Param("lineValue") String lineValue);

    boolean existsByUserAndLine(@Param("userId") Long userId, @Param("lineValue") String lineValue);
}
