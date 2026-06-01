package com.metrohub.notification.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.metrohub.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class CommunityEventConsumerTest {

    @Mock NotificationService notificationService;
    @Mock ObjectMapper objectMapper;

    @InjectMocks CommunityEventConsumer communityEventConsumer;

    @Test
    @DisplayName("커뮤니티 이벤트 → 전체 알림 저장 (postId 포함)")
    void consume_savesNotification() throws Exception {
        String message = "{\"lineNumber\":\"2\",\"title\":\"2호선 공지\",\"id\":5}";
        Map<String, Object> data = new HashMap<>();
        data.put("lineNumber", "2");
        data.put("title", "2호선 공지");
        data.put("id", 5);
        given(objectMapper.readValue(eq(message), any(TypeReference.class))).willReturn(data);

        communityEventConsumer.consume(message, 0, 0L);

        then(notificationService).should()
                .save(eq("COMMUNITY_ALERT"), contains("2호선"), anyString(), eq(5L));
    }

    @Test
    @DisplayName("lineNumber/title 없어도 알림 저장됨")
    void consume_missingFields_savesWithDefaults() throws Exception {
        String message = "{}";
        Map<String, Object> data = new HashMap<>();
        given(objectMapper.readValue(eq(message), any(TypeReference.class))).willReturn(data);

        communityEventConsumer.consume(message, 0, 0L);

        then(notificationService).should()
                .save(eq("COMMUNITY_ALERT"), anyString(), anyString(), isNull());
    }

    @Test
    @DisplayName("JSON 파싱 실패 시 예외 삼켜서 Consumer 중단 없음")
    void consume_parseError_doesNotThrow() throws Exception {
        given(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .willThrow(new RuntimeException("parse error"));

        assertThatCode(() -> communityEventConsumer.consume("invalid-json", 0, 0L))
                .doesNotThrowAnyException();

        then(notificationService).shouldHaveNoInteractions();
    }
}
