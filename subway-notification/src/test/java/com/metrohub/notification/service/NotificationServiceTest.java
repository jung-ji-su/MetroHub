package com.metrohub.notification.service;

import com.metrohub.notification.domain.Notification;
import com.metrohub.notification.domain.NotificationDto;
import com.metrohub.notification.mapper.NotificationMapper;
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
class NotificationServiceTest {

    @Mock NotificationMapper notificationMapper;

    @InjectMocks NotificationService notificationService;

    private Notification buildNotification(Long userId) {
        return Notification.builder()
                .id(1L)
                .userId(userId)
                .type("LINE_ALERT")
                .title("2호선 지연")
                .body("신호 장애로 지연 운행 중입니다.")
                .referenceId(null)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("전체 알림 저장 (userId=null)")
    void save_broadcast() {
        willDoNothing().given(notificationMapper).insert(any());

        notificationService.save("LINE_ALERT", "2호선 지연", "신호 장애", null);

        then(notificationMapper).should().insert(argThat(n -> n.getUserId() == null));
    }

    @Test
    @DisplayName("사용자 알림 저장")
    void saveForUser_success() {
        willDoNothing().given(notificationMapper).insert(any());

        notificationService.saveForUser(10L, "COMPLAINT", "처리 완료", "민원이 처리되었습니다.", 5L);

        then(notificationMapper).should().insert(argThat(n ->
                n.getUserId().equals(10L) && "COMPLAINT".equals(n.getType())));
    }

    @Test
    @DisplayName("타입 없이 전체 알림 목록 조회")
    void getNotifications_noType() {
        Notification n = buildNotification(null);
        given(notificationMapper.findAll(0, 10)).willReturn(List.of(n));

        List<NotificationDto.Response> result = notificationService.getNotifications(null, 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("2호선 지연");
    }

    @Test
    @DisplayName("타입 필터로 알림 목록 조회")
    void getNotifications_withType() {
        Notification n = buildNotification(null);
        given(notificationMapper.findByType("LINE_ALERT", 0, 10)).willReturn(List.of(n));

        List<NotificationDto.Response> result = notificationService.getNotifications("LINE_ALERT", 0, 10);

        assertThat(result).hasSize(1);
        then(notificationMapper).should().findByType("LINE_ALERT", 0, 10);
    }

    @Test
    @DisplayName("사용자별 알림 조회")
    void getByUser_success() {
        Notification n = buildNotification(10L);
        given(notificationMapper.findByUserId(10L, 0, 10)).willReturn(List.of(n));

        List<NotificationDto.Response> result = notificationService.getByUser(10L, 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("알림 카운트 (타입 없음)")
    void countNotifications_noType() {
        given(notificationMapper.countAll()).willReturn(42L);

        assertThat(notificationService.countNotifications(null)).isEqualTo(42L);
    }

    @Test
    @DisplayName("알림 카운트 (타입 있음)")
    void countNotifications_withType() {
        given(notificationMapper.countByType("LINE_ALERT")).willReturn(5L);

        assertThat(notificationService.countNotifications("LINE_ALERT")).isEqualTo(5L);
    }
}
