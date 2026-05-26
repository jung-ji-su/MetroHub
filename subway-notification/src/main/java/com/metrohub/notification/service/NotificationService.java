package com.metrohub.notification.service;

import com.metrohub.notification.domain.Notification;
import com.metrohub.notification.domain.NotificationDto;
import com.metrohub.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    @Transactional
    public void save(String type, String title, String body, Long referenceId) {
        Notification notification = Notification.builder()
                .type(type)
                .title(title)
                .body(body)
                .referenceId(referenceId)
                .build();
        notificationMapper.insert(notification);
        log.info("알림 저장: type={}, title={}, referenceId={}", type, title, referenceId);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto.Response> getNotifications(String type, int page, int size) {
        int offset = page * size;
        List<Notification> list = (type != null && !type.isBlank())
                ? notificationMapper.findByType(type, offset, size)
                : notificationMapper.findAll(offset, size);
        return list.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countNotifications(String type) {
        return (type != null && !type.isBlank())
                ? notificationMapper.countByType(type)
                : notificationMapper.countAll();
    }

    private NotificationDto.Response toResponse(Notification n) {
        return NotificationDto.Response.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .referenceId(n.getReferenceId())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
