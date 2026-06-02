package com.metrohub.api.global.config;

import com.metrohub.api.domain.user.User;
import com.metrohub.api.domain.user.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userMapper.findByNickname("dev").isEmpty()) {
            userMapper.insert(User.builder()
                    .nickname("dev")
                    .password(passwordEncoder.encode("1234"))
                    .role("ADMIN")
                    .build());
            log.info("dev 관리자 계정 생성 완료");
        }
    }
}
