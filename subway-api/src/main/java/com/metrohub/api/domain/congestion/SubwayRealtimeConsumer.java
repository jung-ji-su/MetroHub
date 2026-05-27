package com.metrohub.api.domain.congestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubwayRealtimeConsumer {

    private final CongestionService congestionService;
    private final CongestionHourlyMapper congestionHourlyMapper;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "subway-realtime", groupId = "subway-api-group")
    public void consume(String message) {
        try {
            Map<String, Object> data = objectMapper.readValue(message, Map.class);

            String stationName    = (String) data.get("station_name");
            String lineNumber     = (String) data.get("line_number");
            String trainNo        = (String) data.get("train_no");
            String arrivalMessage = (String) data.get("arrival_message");

            Object rawLevel = data.get("congestion_level");
            Integer congestionLevel = null;
            if (rawLevel != null && !rawLevel.toString().isBlank()) {
                try {
                    congestionLevel = Integer.parseInt(rawLevel.toString().trim().split("\\s+")[0]);
                } catch (NumberFormatException ignored) {}
            }

            if (stationName == null || lineNumber == null) {
                log.warn("subway-realtime 필수 필드 누락 - message={}", message);
                return;
            }

            Congestion congestion = Congestion.builder()
                    .stationName(stationName)
                    .lineNumber(lineNumber)
                    .trainNo(trainNo)
                    .arrivalMessage(arrivalMessage)
                    .congestionLevel(congestionLevel)
                    .build();

            congestionService.upsertCongestion(congestion);

            // 시간대별 혼잡도 누적 (데이터 있을 때만)
            if (congestionLevel != null) {
                int hour = LocalTime.now().getHour();
                congestionHourlyMapper.upsert(stationName, hour, congestionLevel);
            }

            log.debug("혼잡도 업데이트 완료: station={}, line={}, level={}", stationName, lineNumber, congestionLevel);

        } catch (Exception e) {
            log.error("subway-realtime 메시지 처리 실패: {}", e.getMessage(), e);
        }
    }
}
