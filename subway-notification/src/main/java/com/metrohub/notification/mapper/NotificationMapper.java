package com.metrohub.notification.mapper;

import com.metrohub.notification.domain.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {

    void insert(Notification notification);

    List<Notification> findAll(@Param("offset") int offset, @Param("size") int size);

    List<Notification> findByType(@Param("type") String type,
                                  @Param("offset") int offset,
                                  @Param("size") int size);

    List<Notification> findByUserId(@Param("userId") Long userId,
                                    @Param("offset") int offset,
                                    @Param("size") int size);

    int countAll();

    int countByType(@Param("type") String type);

    int countByUserId(@Param("userId") Long userId);

    void markAllReadByUserId(@Param("userId") Long userId);
}
