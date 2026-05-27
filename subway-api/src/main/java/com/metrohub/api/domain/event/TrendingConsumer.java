package com.metrohub.api.domain.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metrohub.api.domain.sse.SseEmitterService;
import com.metrohub.api.domain.trending.TrendingDto;
import com.metrohub.api.domain.trending.TrendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrendingConsumer {

    private final TrendingService trendingService;
    private final SseEmitterService sseEmitterService;
    private final ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    @KafkaListener(topics = "subway.trending.updated", groupId = "subway-api-group")
    public void consume(String message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message, Map.class);
            List<Map<String, Object>> rawList = (List<Map<String, Object>>) payload.get("stations");

            List<TrendingDto.StationEntry> entries = rawList.stream()
                    .map(m -> TrendingDto.StationEntry.builder()
                            .stationName((String) m.get("stationName"))
                            .score(((Number) m.get("score")).intValue())
                            .build())
                    .toList();

            TrendingDto dto = TrendingDto.builder().stations(entries).build();
            trendingService.update(dto);
            sseEmitterService.broadcast("trending.updated", dto);
        } catch (Exception e) {
            log.error("trending.updated 처리 실패: {}", e.getMessage());
        }
    }
}
