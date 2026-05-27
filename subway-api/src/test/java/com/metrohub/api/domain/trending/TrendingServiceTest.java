package com.metrohub.api.domain.trending;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class TrendingServiceTest {

    private final TrendingService trendingService = new TrendingService();

    @Test
    @DisplayName("초기 상태는 빈 목록")
    void getCurrent_initiallyEmpty() {
        TrendingDto result = trendingService.getCurrent();
        assertThat(result.getStations()).isEmpty();
    }

    private static TrendingDto.StationEntry entry(String name) {
        return TrendingDto.StationEntry.builder().stationName(name).score(0).build();
    }

    @Test
    @DisplayName("업데이트 후 최신 데이터 반환")
    void update_and_get() {
        TrendingDto dto = TrendingDto.builder()
                .stations(List.of(entry("강남"), entry("홍대입구"), entry("신촌")))
                .build();

        trendingService.update(dto);

        TrendingDto result = trendingService.getCurrent();
        assertThat(result.getStations())
                .extracting(TrendingDto.StationEntry::getStationName)
                .containsExactly("강남", "홍대입구", "신촌");
    }

    @Test
    @DisplayName("여러 번 업데이트 시 마지막 값 반환")
    void update_multiple_returnsLatest() {
        trendingService.update(TrendingDto.builder().stations(List.of(entry("강남"))).build());
        trendingService.update(TrendingDto.builder().stations(List.of(entry("홍대입구"), entry("신도림"))).build());

        TrendingDto result = trendingService.getCurrent();
        assertThat(result.getStations())
                .extracting(TrendingDto.StationEntry::getStationName)
                .containsExactly("홍대입구", "신도림");
    }
}
