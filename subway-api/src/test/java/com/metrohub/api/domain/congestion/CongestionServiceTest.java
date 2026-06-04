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
    }

    @Test
    @DisplayName("서울 API 응답 있으면 upsert 후 DB 데이터 반환")
    void getCongestionByStation_apiReturnsData_upsertAndReturn() {
        given(seoulSubwayApiClient.fetchArrivals("강남")).willReturn(List.of(freshCongestion));
        given(congestionMapper.findByStationName("강남")).willReturn(List.of(freshCongestion));
        willDoNothing().given(congestionMapper).upsert(any());

        List<CongestionDto.Response> result = congestionService.getCongestionByStation("강남");

        then(seoulSubwayApiClient).should().fetchArrivals("강남");
        then(congestionMapper).should().upsert(freshCongestion);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStationName()).isEqualTo("강남");
    }

    @Test
    @DisplayName("서울 API 응답 없으면 upsert 없이 DB 데이터 반환")
    void getCongestionByStation_apiEmpty_returnsDb() {
        given(seoulSubwayApiClient.fetchArrivals("강남")).willReturn(List.of());
        given(congestionMapper.findByStationName("강남")).willReturn(List.of(freshCongestion));

        List<CongestionDto.Response> result = congestionService.getCongestionByStation("강남");

        then(seoulSubwayApiClient).should().fetchArrivals("강남");
        then(congestionMapper).should(never()).upsert(any());
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStationName()).isEqualTo("강남");
    }

    @Test
    @DisplayName("DB에 데이터 없어도 서울 API 호출 후 결과 반환")
    void getCongestionByStation_dbEmpty_callsApi() {
        Congestion newData = Congestion.builder()
                .id(2L).lineNumber("2").stationName("역삼")
                .congestionLevel(30).trainNo("2002").arrivalMessage("3분")
                .updatedAt(LocalDateTime.now()).build();
        given(seoulSubwayApiClient.fetchArrivals("역삼")).willReturn(List.of(newData));
        given(congestionMapper.findByStationName("역삼")).willReturn(List.of(newData));
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
