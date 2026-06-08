package com.metrohub.api.domain.congestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class CongestionAlertProducer {

    static final String TOPIC = "congestion-alerts";
    private static final int THRESHOLD_CROWDED      = 80;
    private static final int THRESHOLD_VERY_CROWDED = 90;
    private static final long COOLDOWN_MS = 5 * 60 * 1000L; // 역당 5분 쿨다운

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // 마지막 알림 발행 시각 추적 (역명:노선 → timestamp)
    private final ConcurrentHashMap<String, Long> lastAlertTime = new ConcurrentHashMap<>();

    /**
     * 혼잡도 임계값 초과 시 congestion-alerts 토픽으로 이벤트 발행.
     * 동일 역+노선에 대해 5분 쿨다운 적용 — 반복 알림 방지.
     */
    public void publishIfNeeded(String stationName, String lineNumber, int congestionLevel) {
        if (congestionLevel < THRESHOLD_CROWDED) return;

        String key = stationName + ":" + lineNumber;
        long now = System.currentTimeMillis();
        Long last = lastAlertTime.get(key);
        if (last != null && (now - last) < COOLDOWN_MS) return;

        lastAlertTime.put(key, now);

        String severity = congestionLevel >= THRESHOLD_VERY_CROWDED ? "VERY_CROWDED" : "CROWDED";
        String label    = congestionLevel >= THRESHOLD_VERY_CROWDED ? "매우혼잡" : "혼잡";

        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "stationName",     stationName,
                    "lineNumber",      lineNumber,
                    "congestionLevel", congestionLevel,
                    "severity",        severity,
                    "message",         stationName + " " + label + " (혼잡도 " + congestionLevel + ")",
                    "timestamp",       Instant.now().toEpochMilli()
            ));
            // lineNumber를 파티션 키로 사용 — 같은 노선 이벤트는 같은 파티션에 순서 보장
            kafkaTemplate.send(TOPIC, lineNumber, payload);
            log.info("혼잡 알림 발행: station={}, line={}, level={}, severity={}",
                    stationName, lineNumber, congestionLevel, severity);
        } catch (Exception e) {
            log.error("혼잡 알림 발행 실패: {}", e.getMessage());
        }
    }
}
