package com.metrohub.notification.controller;

import com.metrohub.notification.domain.NotificationDto;
import com.metrohub.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotifications(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<NotificationDto.Response> items = notificationService.getNotifications(type, page, size);
        long total = notificationService.countNotifications(type);

        return ResponseEntity.ok(Map.of(
                "items", items,
                "total", total,
                "page", page,
                "size", size
        ));
    }
}
