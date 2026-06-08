package com.metrohub.api.domain.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metrohub.api.domain.sse.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CongestionAlertConsumer {

    private final SseEmitterService sseEmitterService;
    private final ObjectMapper objectMapper;

    /**
     * congestion-alerts 토픽 소비.
     * SubwayRealtimeConsumer가 임계값 초과를 감지해 발행한 이벤트를
     * SSE로 모든 연결된 클라이언트에게 브로드캐스트.
     * subway-realtime → (스트림 처리) → congestion-alerts → SSE 파이프라인 완성.
     */
    @KafkaListener(topics = "congestion-alerts", groupId = "subway-api-group")
    public void consume(String message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message, new TypeReference<>() {});
            sseEmitterService.broadcast("congestion.alert", payload);
            log.info("혼잡 알림 SSE 브로드캐스트: station={}, level={}, severity={}",
                    payload.get("stationName"), payload.get("congestionLevel"), payload.get("severity"));
        } catch (Exception e) {
            log.error("congestion-alerts 처리 실패: {}", e.getMessage());
        }
    }
}
