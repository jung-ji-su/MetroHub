package com.metrohub.notification.subscription;

import com.metrohub.notification.user.UserLookupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserLookupMapper userLookupMapper;

    @GetMapping
    public ResponseEntity<List<Subscription>> list(Authentication auth) {
        Long userId = resolveUserId(auth);
        return ResponseEntity.ok(subscriptionService.getByUser(userId));
    }

    @PostMapping
    public ResponseEntity<Subscription> subscribe(
            @RequestBody Map<String, String> body,
            Authentication auth) {
        Long userId  = resolveUserId(auth);
        String subType  = body.getOrDefault("subType", "LINE");
        String subValue = body.get("subValue");
        if (subValue == null || subValue.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(subscriptionService.subscribe(userId, subType, subValue));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> unsubscribe(@PathVariable Long id, Authentication auth) {
        Long userId = resolveUserId(auth);
        subscriptionService.unsubscribe(userId, id);
        return ResponseEntity.noContent().build();
    }

    private Long resolveUserId(Authentication auth) {
        return userLookupMapper.findIdByEmail(auth.getName());
    }
}
