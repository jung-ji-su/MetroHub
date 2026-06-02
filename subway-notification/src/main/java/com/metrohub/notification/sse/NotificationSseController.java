package com.metrohub.notification.sse;

import com.metrohub.notification.user.UserLookupMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationSseController {

    private final NotificationSseService sseService;
    private final StreamTokenStore streamTokenStore;
    private final UserLookupMapper userLookupMapper;

    /** JWT Bearer 인증으로 단회용 SSE 스트림 토큰 발급 (60초 TTL) */
    @PostMapping("/stream-token")
    public ResponseEntity<Map<String, String>> issueStreamToken(Authentication auth) {
        String streamToken = streamTokenStore.issue(auth.getName());
        return ResponseEntity.ok(Map.of("streamToken", streamToken));
    }

    /** SSE 연결은 단회용 streamToken으로만 인증 (JWT를 URL에 노출하지 않음) */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String streamToken) {
        String email = streamTokenStore.consume(streamToken).orElse(null);
        if (email == null) {
            log.warn("SSE 연결 거부 — 유효하지 않거나 만료된 streamToken");
            SseEmitter emitter = new SseEmitter(0L);
            emitter.complete();
            return emitter;
        }
        Long userId = userLookupMapper.findIdByEmail(email);
        if (userId == null) {
            log.warn("알림 SSE 연결 실패 - 사용자 없음: {}", email);
            SseEmitter emitter = new SseEmitter(0L);
            emitter.complete();
            return emitter;
        }
        return sseService.subscribe(userId);
    }
}
