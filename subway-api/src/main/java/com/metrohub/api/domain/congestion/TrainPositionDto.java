package com.metrohub.api.domain.congestion;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrainPositionDto {
    private String trainNo;
    private String lineCode;
    private String currentStation;  // 현재 열차 위치역 (arvlMsg3)
    private String direction;       // 상행/하행/외선순환 등
    private String destination;     // 행선지 (성수행, 외선순환 등)
    private String arrivalMessage;  // 원문 도착 메시지
    private int    etaSeconds;      // 다음역 도착 예정 초
}
