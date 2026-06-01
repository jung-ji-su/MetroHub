package com.metrohub.notification.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metrohub.notification.service.NotificationService;
import com.metrohub.notification.sse.NotificationSseService;
import com.metrohub.notification.subscription.SubscriptionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class LineAlertConsumerTest {

    @Mock NotificationService notificationService;
    @Mock SubscriptionService subscriptionService;
    @Mock NotificationSseService sseService;
    @Mock ObjectMapper objectMapper;

    @InjectMocks LineAlertConsumer lineAlertConsumer;

    @Test
    @DisplayName("구독자들에게 알림 저장 + SSE 푸시")
    void consume_notifiesSubscribers() throws Exception {
        String message = "{\"lineNumber\":\"2\",\"alertType\":\"CONGESTION_SPIKE\",\"message\":\"혼잡도 급등\"}";
        Map<String, Object> data = Map.of(
                "lineNumber", "2",
                "alertType", "CONGESTION_SPIKE",
                "message", "혼잡도 급등"
        );
        given(objectMapper.readValue(eq(message), any(TypeReference.class))).willReturn(data);
        given(subscriptionService.getUsersByLine("2")).willReturn(List.of(10L, 20L));

        lineAlertConsumer.consume(message);

        then(notificationService).should(times(2))
                .saveForUser(anyLong(), eq("CONGESTION_SPIKE"), anyString(), anyString(), isNull());
        then(sseService).should(times(2))
                .sendToUser(anyLong(), eq("notification"), anyMap());
    }

    @Test
    @DisplayName("구독자 없는 노선 → 알림/SSE 없음")
    void consume_noSubscribers_noNotification() throws Exception {
        String message = "{\"lineNumber\":\"9\",\"alertType\":\"ALERT\",\"message\":\"이상 감지\"}";
        Map<String, Object> data = Map.of("lineNumber", "9", "alertType", "ALERT", "message", "이상 감지");
        given(objectMapper.readValue(eq(message), any(TypeReference.class))).willReturn(data);
        given(subscriptionService.getUsersByLine("9")).willReturn(List.of());

        lineAlertConsumer.consume(message);

        then(notificationService).shouldHaveNoInteractions();
        then(sseService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("JSON 파싱 실패 시 예외 삼켜서 Consumer 중단 없음")
    void consume_parseError_doesNotThrow() throws Exception {
        given(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .willThrow(new RuntimeException("parse error"));

        assertThatCode(() -> lineAlertConsumer.consume("invalid-json"))
                .doesNotThrowAnyException();

        then(notificationService).shouldHaveNoInteractions();
    }
}
