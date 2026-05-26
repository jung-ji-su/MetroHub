package com.metrohub.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metrohub.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "community-events", groupId = "notification-group")
    public void consume(@Payload String message,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("커뮤니티 이벤트 수신 [partition={}, offset={}]", partition, offset);
        try {
            Map<String, Object> data = objectMapper.readValue(message, Map.class);

            String lineNumber = (String) data.get("lineNumber");
            String title      = (String) data.get("title");
            Long postId = data.get("id") != null
                    ? Long.parseLong(data.get("id").toString()) : null;

            String notificationTitle = (lineNumber != null ? lineNumber + "호선" : "") + " 커뮤니티 알림";
            String notificationBody  = title != null ? title : "새로운 게시글 알림이 있습니다.";

            notificationService.save("COMMUNITY_ALERT", notificationTitle, notificationBody, postId);

        } catch (Exception e) {
            log.error("커뮤니티 이벤트 처리 실패: {}", e.getMessage(), e);
        }
    }
}
