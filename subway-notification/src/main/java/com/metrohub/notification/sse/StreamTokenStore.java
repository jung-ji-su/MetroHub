package com.metrohub.notification.sse;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StreamTokenStore {

    private static final long TTL_SECONDS = 60;

    private record Entry(String email, Instant expiresAt) {}

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    public String issue(String email) {
        String token = UUID.randomUUID().toString();
        store.put(token, new Entry(email, Instant.now().plusSeconds(TTL_SECONDS)));
        return token;
    }

    // 단회 사용 — 꺼내는 즉시 삭제
    public Optional<String> consume(String token) {
        Entry entry = store.remove(token);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) return Optional.empty();
        return Optional.of(entry.email());
    }
}
