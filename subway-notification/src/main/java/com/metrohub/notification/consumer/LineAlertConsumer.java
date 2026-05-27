package com.metrohub.notification.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metrohub.notification.service.NotificationService;
import com.metrohub.notification.sse.NotificationSseService;
import com.metrohub.notification.subscription.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class LineAlertConsumer {

    private final NotificationService notificationService;
    private final SubscriptionService subscriptionService;
    private final NotificationSseService sseService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "subway.line.alert", groupId = "notification-group")
    public void consume(String message) {
        log.info("노선 알림 이벤트 수신: {}", message);
        try {
            Map<String, Object> data = objectMapper.readValue(message, new TypeReference<>() {});

            String lineNumber = (String) data.get("lineNumber");
            String alertType  = (String) data.getOrDefault("alertType", "ALERT");
            String alertMsg   = (String) data.getOrDefault("message", "노선 이상 감지");

            String title = lineNumber + "호선 알림";
            String body  = alertMsg;

            // 해당 노선 구독자들에게 개인 알림 저장 + SSE 푸시
            List<Long> subscribedUserIds = subscriptionService.getUsersByLine(lineNumber);
            log.info("노선 {} 구독자 {} 명에게 알림 전송", lineNumber, subscribedUserIds.size());

            for (Long userId : subscribedUserIds) {
                notificationService.saveForUser(userId, alertType, title, body, null);
                sseService.sendToUser(userId, "notification", Map.of(
                        "title", title,
                        "body", body,
                        "type", alertType,
                        "lineNumber", lineNumber
                ));
            }

        } catch (Exception e) {
            log.error("노선 알림 이벤트 처리 실패: {}", e.getMessage(), e);
        }
    }
}
