package com.metrohub.notification.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
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
public class ComplaintEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "complaint-events", groupId = "notification-group")
    public void consume(@Payload String message,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("민원 이벤트 수신 [partition={}, offset={}]", partition, offset);
        try {
            Map<String, Object> data = objectMapper.readValue(message, new TypeReference<>() {});

            String category    = (String) data.get("category");
            String stationName = (String) data.get("stationName");
            Long complaintId = data.get("id") != null
                    ? Long.parseLong(data.get("id").toString()) : null;
            Long userId = data.get("userId") != null
                    ? Long.parseLong(data.get("userId").toString()) : null;

            String notificationTitle = "민원 접수 완료";
            String notificationBody  = String.format("[%s] %s 역 민원이 접수되었습니다.",
                    category != null ? category : "기타",
                    stationName != null ? stationName : "");

            notificationService.saveForUser(userId, "COMPLAINT_RECEIVED", notificationTitle, notificationBody, complaintId);

        } catch (Exception e) {
            log.error("민원 이벤트 처리 실패: {}", e.getMessage(), e);
        }
    }
}
