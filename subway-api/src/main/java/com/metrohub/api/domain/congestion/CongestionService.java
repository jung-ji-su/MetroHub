package com.metrohub.api.domain.congestion;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CongestionService {

    private final CongestionMapper congestionMapper;

    @Transactional(readOnly = true)
    public List<CongestionDto.Response> getCongestionByStation(String stationName) {
        return congestionMapper.findByStationName(stationName).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
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
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
