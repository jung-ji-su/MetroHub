package com.metrohub.notification.subscription;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock SubscriptionMapper subscriptionMapper;

    @InjectMocks SubscriptionService subscriptionService;

    @Test
    @DisplayName("호선 구독 추가 - subType 대문자로 저장")
    void subscribe_uppercasesSubType() {
        willDoNothing().given(subscriptionMapper).insert(any());

        Subscription result = subscriptionService.subscribe(10L, "line", "2");

        assertThat(result.getSubType()).isEqualTo("LINE");
        assertThat(result.getSubValue()).isEqualTo("2");
        assertThat(result.getUserId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("구독 해제 성공")
    void unsubscribe_success() {
        willDoNothing().given(subscriptionMapper).delete(1L, 10L);

        assertThatCode(() -> subscriptionService.unsubscribe(10L, 1L))
                .doesNotThrowAnyException();

        then(subscriptionMapper).should().delete(1L, 10L);
    }

    @Test
    @DisplayName("사용자 구독 목록 조회")
    void getByUser_success() {
        Subscription sub = Subscription.builder()
                .id(1L).userId(10L).subType("LINE").subValue("2").build();
        given(subscriptionMapper.findByUserId(10L)).willReturn(List.of(sub));

        List<Subscription> result = subscriptionService.getByUser(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSubValue()).isEqualTo("2");
    }

    @Test
    @DisplayName("노선별 구독 유저 ID 목록 조회")
    void getUsersByLine_success() {
        given(subscriptionMapper.findUserIdsByLine("2")).willReturn(List.of(10L, 20L, 30L));

        List<Long> userIds = subscriptionService.getUsersByLine("2");

        assertThat(userIds).containsExactly(10L, 20L, 30L);
    }

    @Test
    @DisplayName("구독자 없는 노선은 빈 목록 반환")
    void getUsersByLine_empty() {
        given(subscriptionMapper.findUserIdsByLine("9")).willReturn(List.of());

        assertThat(subscriptionService.getUsersByLine("9")).isEmpty();
    }
}
