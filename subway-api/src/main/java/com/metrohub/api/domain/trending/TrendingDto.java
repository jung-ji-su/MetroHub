package com.metrohub.api.domain.trending;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TrendingDto {
    private List<StationEntry> stations;

    @Getter
    @Builder
    public static class StationEntry {
        private String stationName;
        private int    score;
    }
}
