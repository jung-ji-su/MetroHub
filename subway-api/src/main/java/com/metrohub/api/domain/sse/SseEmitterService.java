package com.metrohub.api.domain.sse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class SseEmitterService {

    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    public SseEmitter subscribe() {
        String clientId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);

        emitters.put(clientId, emitter);
        emitter.onCompletion(() -> emitters.remove(clientId));
        emitter.onTimeout(()    -> emitters.remove(clientId));
        emitter.onError(e       -> emitters.remove(clientId));

        try {
            emitter.send(SseEmitter.event().name("connected").data("{\"clientId\":\"" + clientId + "\"}"));
        } catch (IOException e) {
            emitters.remove(clientId);
        }
        log.debug("SSE 연결: {} (활성 {}명)", clientId, emitters.size());
        return emitter;
    }

    public void broadcast(String eventName, Object data) {
        if (emitters.isEmpty()) return;
        String json;
        try {
            json = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("SSE 직렬화 실패: {}", e.getMessage());
            return;
        }
        List<String> dead = new CopyOnWriteArrayList<>();
        emitters.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(json));
            } catch (IOException ex) {
                dead.add(id);
            }
        });
        dead.forEach(emitters::remove);
    }
}
