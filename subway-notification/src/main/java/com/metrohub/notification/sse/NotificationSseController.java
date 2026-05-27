package com.metrohub.notification.sse;

import com.metrohub.notification.global.config.JwtUtil;
import com.metrohub.notification.user.UserLookupMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationSseController {

    private final NotificationSseService sseService;
    private final UserLookupMapper userLookupMapper;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Authentication auth) {
        String email = auth.getName();
        Long userId = userLookupMapper.findIdByEmail(email);
        if (userId == null) {
            log.warn("알림 SSE 연결 실패 - 사용자 없음: {}", email);
            SseEmitter emitter = new SseEmitter();
            emitter.complete();
            return emitter;
        }
        return sseService.subscribe(userId);
    }
}
