package com.metrohub.api.domain.congestion;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CongestionHourlyDto {
    private int hourOfDay;
    private double avgCongestion;
    private int sampleCount;
}
