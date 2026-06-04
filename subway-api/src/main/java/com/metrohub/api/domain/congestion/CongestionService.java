package com.metrohub.api.domain.congestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CongestionService {

    private final CongestionMapper congestionMapper;
    private final SeoulSubwayApiClient seoulSubwayApiClient;

    @Transactional
    public List<CongestionDto.Response> getCongestionByStation(String stationName) {
        log.info("on-demand 서울 API 호출: station={}", stationName);
        List<Congestion> fresh = seoulSubwayApiClient.fetchArrivals(stationName);
        if (!fresh.isEmpty()) {
            fresh.forEach(congestionMapper::upsert);
            return congestionMapper.findByStationName(stationName).stream()
                    .map(this::toResponse).collect(Collectors.toList());
        }
        return congestionMapper.findByStationName(stationName).stream()
                .map(this::toResponse).collect(Collectors.toList());
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
