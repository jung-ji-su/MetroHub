package com.metrohub.api.domain.complaint;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock ComplaintMapper complaintMapper;
    @Mock KafkaTemplate<String, String> kafkaTemplate;
    @Mock ObjectMapper objectMapper;

    @InjectMocks ComplaintService complaintService;

    private Complaint complaint;

    @BeforeEach
    void setUp() {
        complaint = Complaint.builder()
                .id(1L)
                .userId(10L)
                .category("시설")
                .stationName("강남")
                .content("에스컬레이터 고장")
                .status("RECEIVED")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("민원 접수 성공")
    void createComplaint_success() throws Exception {
        ComplaintDto.CreateRequest req = new ComplaintDto.CreateRequest("시설", "강남", "에스컬레이터 고장");
        willDoNothing().given(complaintMapper).insert(any());
        given(objectMapper.writeValueAsString(any())).willReturn("{}");

        ComplaintDto.Response result = complaintService.createComplaint(10L, req);

        assertThat(result.getStatus()).isEqualTo("RECEIVED");
        assertThat(result.getStationName()).isEqualTo("강남");
        then(kafkaTemplate).should().send(eq("complaint-events"), anyString());
    }

    @Test
    @DisplayName("Kafka 발행 실패해도 민원 접수는 성공")
    void createComplaint_kafkaFails_stillCreates() throws Exception {
        ComplaintDto.CreateRequest req = new ComplaintDto.CreateRequest("시설", "강남", "고장");
        willDoNothing().given(complaintMapper).insert(any());
        given(objectMapper.writeValueAsString(any())).willThrow(new RuntimeException("Kafka 오류"));

        ComplaintDto.Response result = complaintService.createComplaint(10L, req);

        assertThat(result.getStatus()).isEqualTo("RECEIVED");
    }

    @Test
    @DisplayName("내 민원 목록 조회")
    void getMyComplaints_success() {
        given(complaintMapper.findByUserId(10L)).willReturn(List.of(complaint));

        List<ComplaintDto.Response> result = complaintService.getMyComplaints(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("시설");
    }

    @Test
    @DisplayName("민원 상세 조회 성공")
    void getComplaint_success() {
        given(complaintMapper.findById(1L)).willReturn(Optional.of(complaint));

        ComplaintDto.Response result = complaintService.getComplaint(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getContent()).isEqualTo("에스컬레이터 고장");
    }

    @Test
    @DisplayName("존재하지 않는 민원 조회 실패")
    void getComplaint_notFound_throws() {
        given(complaintMapper.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> complaintService.getComplaint(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("민원을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("민원 삭제 성공")
    void deleteComplaint_success() {
        given(complaintMapper.deleteByIdAndUserId(1L, 10L)).willReturn(1);

        assertThatCode(() -> complaintService.deleteComplaint(10L, 1L))
                .doesNotThrowAnyException();

        then(complaintMapper).should().deleteByIdAndUserId(1L, 10L);
    }

    @Test
    @DisplayName("없는 민원 삭제 시 예외 발생")
    void deleteComplaint_notFound_throws() {
        given(complaintMapper.deleteByIdAndUserId(999L, 10L)).willReturn(0);

        assertThatThrownBy(() -> complaintService.deleteComplaint(10L, 999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("민원을 찾을 수 없거나");
    }
}
