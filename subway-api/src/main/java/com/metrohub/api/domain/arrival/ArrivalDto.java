package com.metrohub.api.domain.arrival;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ArrivalDto {
    private String lineCode;
    private String direction;
    private String destination;
    private String arrivalMessage;
    private int    etaSeconds;
    private String trainStatus;
}
