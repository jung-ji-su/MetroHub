package com.metrohub.notification.controller;

import com.metrohub.notification.domain.NotificationDto;
import com.metrohub.notification.service.NotificationService;
import com.metrohub.notification.user.UserLookupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserLookupMapper userLookupMapper;

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

    @PostMapping("/my/read-all")
    public ResponseEntity<Void> markAllRead(Authentication auth) {
        Long userId = userLookupMapper.findIdByEmail(auth.getName());
        if (userId == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        notificationService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> getMyNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {

        Long userId = userLookupMapper.findIdByEmail(auth.getName());
        if (userId == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        List<NotificationDto.Response> items = notificationService.getByUser(userId, page, size);
        long total = notificationService.countByUser(userId);

        return ResponseEntity.ok(Map.of(
                "items", items,
                "total", total,
                "page", page
        ));
    }
}
