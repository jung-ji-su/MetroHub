package com.metrohub.api.domain.congestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CongestionService {

    private static final int CACHE_MINUTES = 10;

    private final CongestionMapper congestionMapper;
    private final SeoulSubwayApiClient seoulSubwayApiClient;

    @Transactional
    public List<CongestionDto.Response> getCongestionByStation(String stationName) {
        List<Congestion> cached = congestionMapper.findByStationName(stationName);

        if (isStale(cached)) {
            log.info("on-demand 서울 API 호출: station={}", stationName);
            List<Congestion> fresh = seoulSubwayApiClient.fetchArrivals(stationName);
            if (!fresh.isEmpty()) {
                fresh.forEach(congestionMapper::upsert);
                cached = congestionMapper.findByStationName(stationName);
            } else if (!cached.isEmpty()) {
                // 서울 API 장애 시 stale 데이터 fallback (빈 응답 대신 이전 데이터 반환)
                log.warn("서울 API 응답 없음, stale 캐시 데이터 반환: station={}", stationName);
            }
        }

        return cached.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CongestionDto.Response> getCongestionByLine(String lineNumber) {
        return congestionMapper.findByLineNumber(lineNumber).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void upsertCongestion(Congestion congestion) {
        congestionMapper.upsert(congestion);
    }

    private boolean isStale(List<Congestion> data) {
        if (data.isEmpty()) return true;
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(CACHE_MINUTES);
        return data.stream().noneMatch(c -> c.getUpdatedAt() != null && c.getUpdatedAt().isAfter(cutoff));
    }

    private CongestionDto.Response toResponse(Congestion c) {
        return CongestionDto.Response.builder()
                .id(c.getId())
                .lineNumber(c.getLineNumber())
                .stationName(c.getStationName())
                .congestionLevel(c.getCongestionLevel())
                .trainNo(c.getTrainNo())
                .arrivalMessage(c.getArrivalMessage())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
