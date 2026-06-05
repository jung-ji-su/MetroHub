package com.metrohub.api.domain.congestion;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrainPositionDto {
    private String  trainNo;
    private String  lineCode;
    private String  nextStation;     // 다음 도착예정역 (queriedStation — 위치 계산 기준)
    private String  currentStation;  // arvlMsg3 원문 (참고용, 위치 계산에 사용 금지)
    private String  direction;       // 상행/하행/외선순환 등
    private String  destination;     // 행선지 (성수행, 외선순환 등)
    private String  arrivalMessage;  // 원문 도착 메시지
    private int     etaSeconds;      // nextStation 도착 예정 초
    private boolean express;         // 급행 여부 (btrainSttus 포함 "급행")
}
