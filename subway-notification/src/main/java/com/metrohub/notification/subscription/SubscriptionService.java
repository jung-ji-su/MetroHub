package com.metrohub.notification.subscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionMapper subscriptionMapper;

    @Transactional
    public Subscription subscribe(Long userId, String subType, String subValue) {
        Subscription sub = Subscription.builder()
                .userId(userId)
                .subType(subType.toUpperCase())
                .subValue(subValue)
                .build();
        subscriptionMapper.insert(sub);
        return sub;
    }

    @Transactional
    public void unsubscribe(Long userId, Long subscriptionId) {
        subscriptionMapper.delete(subscriptionId, userId);
    }

    @Transactional(readOnly = true)
    public List<Subscription> getByUser(Long userId) {
        return subscriptionMapper.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Long> getUsersByLine(String lineValue) {
        return subscriptionMapper.findUserIdsByLine(lineValue);
    }
}
