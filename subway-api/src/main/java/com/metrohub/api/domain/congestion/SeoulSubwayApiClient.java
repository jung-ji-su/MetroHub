package com.metrohub.api.domain.congestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeoulSubwayApiClient {

    @Value("${seoul.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String BASE_URL = "http://swopenAPI.seoul.go.kr/api/subway";

    public List<Congestion> fetchArrivals(String stationName) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("SEOUL_API_KEY가 설정되지 않아 on-demand 조회를 건너뜁니다.");
            return Collections.emptyList();
        }
        try {
            String encoded = URLEncoder.encode(stationName, StandardCharsets.UTF_8);
            String url = BASE_URL + "/" + apiKey + "/json/realtimeStationArrival/0/50/" + encoded;
            String response = restTemplate.getForObject(url, String.class);
            return parseResponse(response);
        } catch (Exception e) {
            log.warn("Seoul API 호출 실패 (station={}): {}", stationName, e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── 노선도용 상세 도착 정보 ────────────────────────────────────────

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ArrivalDetail {
        private String trainNo;
        private String lineCode;        // subwayId (e.g. "1002")
        private String queriedStation;  // 조회한 역
        private String currentStation;  // arvlMsg3: 열차 현재 위치역
        private String direction;       // updnLine: "상행" / "하행" / "외선순환" 등
        private String destination;     // trainLineNm: "성수행", "외선순환" 등
        private String arrivalMessage;  // arvlMsg2
        private int    etaSeconds;      // barvlDt (초)
    }

    public List<ArrivalDetail> fetchArrivalDetails(String stationName) {
        if (apiKey == null || apiKey.isBlank()) return Collections.emptyList();
        try {
            String encoded = URLEncoder.encode(stationName, StandardCharsets.UTF_8);
            String url = BASE_URL + "/" + apiKey + "/json/realtimeStationArrival/0/50/" + encoded;
            String response = restTemplate.getForObject(url, String.class);
            return parseDetails(response, stationName);
        } catch (Exception e) {
            log.warn("Seoul API 호출 실패 (station={}): {}", stationName, e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ArrivalDetail> parseDetails(String responseStr, String queriedStation) {
        try {
            Map<String, Object> raw = objectMapper.readValue(responseStr, Map.class);

            if (raw.containsKey("code") && !raw.containsKey("realtimeArrivalList")) {
                log.warn("Seoul API 오류: [{}] {}", raw.get("code"), raw.get("message"));
                return Collections.emptyList();
            }
            Map<String, Object> errorMsg = (Map<String, Object>) raw.getOrDefault("errorMessage", Map.of());
            Object status = errorMsg.get("status");
            if (status != null && !"200".equals(status.toString())) return Collections.emptyList();

            List<Map<String, Object>> arrivals =
                (List<Map<String, Object>>) raw.getOrDefault("realtimeArrivalList", Collections.emptyList());

            return arrivals.stream()
                .map(item -> {
                    int eta = 0;
                    try { eta = Integer.parseInt(String.valueOf(item.getOrDefault("barvlDt", "0"))); }
                    catch (NumberFormatException ignored) {}
                    return ArrivalDetail.builder()
                        .trainNo((String) item.get("btrainNo"))
                        .lineCode((String) item.get("subwayId"))
                        .queriedStation(queriedStation)
                        .currentStation((String) item.get("arvlMsg3"))
                        .direction((String) item.get("updnLine"))
                        .destination((String) item.get("trainLineNm"))
                        .arrivalMessage((String) item.get("arvlMsg2"))
                        .etaSeconds(eta)
                        .build();
                })
                .filter(d -> d.getTrainNo() != null && d.getLineCode() != null)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Seoul API 파싱 실패 (details): {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── 기존 혼잡도 파싱 ────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<Congestion> parseResponse(String responseStr) {
        try {
            Map<String, Object> raw = objectMapper.readValue(responseStr, Map.class);

            // 최상위 에러 형식 ({"status":500, "code":"...", "message":"..."})
            if (raw.containsKey("code") && !raw.containsKey("realtimeArrivalList")) {
                log.warn("Seoul API 오류: [{}] {}", raw.get("code"), raw.get("message"));
                return Collections.emptyList();
            }

            Map<String, Object> errorMsg = (Map<String, Object>) raw.getOrDefault("errorMessage", Map.of());
            Object status = errorMsg.get("status");
            if (status != null && !"200".equals(status.toString())) {
                log.warn("Seoul API 오류: [{}] {}", errorMsg.get("code"), errorMsg.get("message"));
                return Collections.emptyList();
            }

            List<Map<String, Object>> arrivals =
                    (List<Map<String, Object>>) raw.getOrDefault("realtimeArrivalList", Collections.emptyList());

            return arrivals.stream()
                    .map(item -> {
                        String rawLevel = (String) item.get("congestionTrain");
                        Integer congestionLevel = null;
                        if (rawLevel != null && !rawLevel.isBlank()) {
                            try {
                                congestionLevel = Integer.parseInt(rawLevel.trim().split("\\s+")[0]);
                            } catch (NumberFormatException ignored) {}
                        }
                        return Congestion.builder()
                                .stationName((String) item.get("statnNm"))
                                .lineNumber((String) item.get("subwayId"))
                                .trainNo((String) item.get("btrainNo"))
                                .arrivalMessage((String) item.get("arvlMsg2"))
                                .congestionLevel(congestionLevel)
                                .build();
                    })
                    .filter(c -> c.getStationName() != null && c.getLineNumber() != null)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Seoul API 응답 파싱 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
