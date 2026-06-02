package com.metrohub.api.domain.congestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubwayLineService {

    private final SeoulSubwayApiClient seoulClient;

    // 노선별 조회 대상 핵심 역 — 노선 전체를 커버하도록 고르게 분포
    private static final Map<String, List<String>> KEY_STATIONS;

    static {
        Map<String, List<String>> m = new HashMap<>();
        m.put("1001", List.of("서울역", "청량리", "의정부", "영등포", "구로"));
        m.put("1002", List.of("시청", "왕십리", "잠실", "강남", "신도림", "홍대입구"));
        m.put("1003", List.of("종로3가", "을지로3가", "교대", "수서", "대화"));
        m.put("1004", List.of("서울역", "동대문", "사당", "과천", "금정"));
        m.put("1005", List.of("광화문", "여의도", "영등포구청", "마곡", "방화"));
        m.put("1006", List.of("삼각지", "합정", "응암", "상월곡"));
        m.put("1007", List.of("건대입구", "고속터미널", "학동", "온수"));
        m.put("1008", List.of("잠실", "복정", "모란"));
        m.put("1009", List.of("김포공항", "여의도", "고속터미널", "봉은사"));
        m.put("1075", List.of("강남", "판교", "광교중앙"));
        m.put("1077", List.of("서울숲", "강남구청", "수원"));
        m.put("1063", List.of("용산", "왕십리", "청량리"));
        m.put("1065", List.of("서울역", "공덕", "인천공항1터미널"));
        KEY_STATIONS = Collections.unmodifiableMap(m);
    }

    // 5분 인메모리 캐시 (API 일일 한도 절약)
    private static final long CACHE_MILLIS = 5 * 60 * 1000L;

    private record CachedData(long timestamp, List<TrainPositionDto> trains) {}
    private final Map<String, CachedData> cache = new ConcurrentHashMap<>();

    public List<TrainPositionDto> getTrains(String lineCode) {
        CachedData cached = cache.get(lineCode);
        if (cached != null && System.currentTimeMillis() - cached.timestamp() < CACHE_MILLIS) {
            log.debug("노선도 캐시 반환 (lineCode={})", lineCode);
            return cached.trains();
        }

        List<String> stations = KEY_STATIONS.getOrDefault(lineCode, Collections.emptyList());
        if (stations.isEmpty()) {
            log.warn("지원하지 않는 노선 코드: {}", lineCode);
            return Collections.emptyList();
        }

        // 핵심 역들 조회 후 열차별로 집계
        // trainNo 기준으로 중복 제거: ETA가 가장 작은(= 가장 최신) 레코드 유지
        Map<String, SeoulSubwayApiClient.ArrivalDetail> trainMap = new LinkedHashMap<>();
        for (String station : stations) {
            List<SeoulSubwayApiClient.ArrivalDetail> details = seoulClient.fetchArrivalDetails(station);
            for (SeoulSubwayApiClient.ArrivalDetail d : details) {
                if (!lineCode.equals(d.getLineCode())) continue;
                trainMap.merge(d.getTrainNo(), d, (existing, newer) ->
                    newer.getEtaSeconds() < existing.getEtaSeconds() ? newer : existing);
            }
        }

        List<TrainPositionDto> trains = trainMap.values().stream()
            .map(d -> TrainPositionDto.builder()
                .trainNo(d.getTrainNo())
                .lineCode(d.getLineCode())
                .currentStation(d.getCurrentStation())
                .direction(d.getDirection())
                .destination(d.getDestination())
                .arrivalMessage(d.getArrivalMessage())
                .etaSeconds(d.getEtaSeconds())
                .express(d.getTrainStatus() != null && d.getTrainStatus().contains("급행"))
                .build())
            .collect(Collectors.toList());

        log.info("노선도 데이터 갱신 (lineCode={}, trains={})", lineCode, trains.size());
        cache.put(lineCode, new CachedData(System.currentTimeMillis(), trains));
        return trains;
    }
}
