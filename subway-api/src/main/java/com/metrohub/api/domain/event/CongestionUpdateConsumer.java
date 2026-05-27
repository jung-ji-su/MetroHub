package com.metrohub.api.domain.event;

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
public class CongestionUpdateConsumer {

    private final SseEmitterService sseEmitterService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "subway.congestion.updated", groupId = "subway-api-group")
    public void consume(String message) {
        try {
            Map<?, ?> payload = objectMapper.readValue(message, Map.class);
            sseEmitterService.broadcast("congestion.updated", payload);
        } catch (Exception e) {
            log.error("congestion.updated 처리 실패: {}", e.getMessage());
        }
    }
}
