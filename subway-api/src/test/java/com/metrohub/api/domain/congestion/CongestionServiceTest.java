package com.metrohub.api.domain.congestion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CongestionServiceTest {

    @Mock CongestionMapper congestionMapper;
    @Mock SeoulSubwayApiClient seoulSubwayApiClient;

    @InjectMocks CongestionService congestionService;

    private Congestion freshCongestion;
    private Congestion staleCongestion;

    @BeforeEach
    void setUp() {
        freshCongestion = Congestion.builder()
                .id(1L)
                .lineNumber("2")
                .stationName("강남")
                .congestionLevel(50)
                .trainNo("2001")
                .arrivalMessage("곧 도착")
                .updatedAt(LocalDateTime.now())
                .build();

        staleCongestion = Congestion.builder()
                .id(2L)
                .lineNumber("2")
                .stationName("강남")
                .congestionLevel(80)
                .trainNo("2002")
                .arrivalMessage("2분")
                .updatedAt(LocalDateTime.now().minusMinutes(15))
                .build();
    }

    @Test
    @DisplayName("캐시가 신선하면 서울 API 호출 없이 DB 데이터 반환")
    void getCongestionByStation_fresh_noApiCall() {
        given(congestionMapper.findByStationName("강남")).willReturn(List.of(freshCongestion));

        List<CongestionDto.Response> result = congestionService.getCongestionByStation("강남");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStationName()).isEqualTo("강남");
        then(seoulSubwayApiClient).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("캐시가 만료되면 서울 API 재호출")
    void getCongestionByStation_stale_callsApi() {
        given(congestionMapper.findByStationName("강남"))
                .willReturn(List.of(staleCongestion))
                .willReturn(List.of(freshCongestion));
        given(seoulSubwayApiClient.fetchArrivals("강남")).willReturn(List.of(freshCongestion));
        willDoNothing().given(congestionMapper).upsert(any());

        List<CongestionDto.Response> result = congestionService.getCongestionByStation("강남");

        then(seoulSubwayApiClient).should().fetchArrivals("강남");
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("DB에 데이터 없으면 서울 API 호출")
    void getCongestionByStation_empty_callsApi() {
        given(congestionMapper.findByStationName("역삼"))
                .willReturn(List.of())
                .willReturn(List.of(freshCongestion));
        given(seoulSubwayApiClient.fetchArrivals("역삼")).willReturn(List.of(freshCongestion));
        willDoNothing().given(congestionMapper).upsert(any());

        List<CongestionDto.Response> result = congestionService.getCongestionByStation("역삼");

        then(seoulSubwayApiClient).should().fetchArrivals("역삼");
        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("호선별 혼잡도 조회")
    void getCongestionByLine_success() {
        given(congestionMapper.findByLineNumber("2")).willReturn(List.of(freshCongestion));

        List<CongestionDto.Response> result = congestionService.getCongestionByLine("2");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLineNumber()).isEqualTo("2");
    }

    @Test
    @DisplayName("혼잡도 upsert")
    void upsertCongestion_success() {
        willDoNothing().given(congestionMapper).upsert(freshCongestion);

        assertThatCode(() -> congestionService.upsertCongestion(freshCongestion))
                .doesNotThrowAnyException();

        then(congestionMapper).should().upsert(freshCongestion);
    }
}
