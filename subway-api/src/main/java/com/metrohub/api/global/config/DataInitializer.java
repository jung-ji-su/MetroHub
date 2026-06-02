package com.metrohub.api.global.config;

import com.metrohub.api.domain.user.User;
import com.metrohub.api.domain.user.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        applySchemaUpdates();
        seedAdminAccount();
    }

    private void applySchemaUpdates() {
        // V3: email nullable (TiDB-safe, idempotent)
        try {
            jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN email VARCHAR(255) NULL DEFAULT NULL");
            log.info("users.email nullable 적용 완료");
        } catch (Exception e) {
            log.debug("users.email 컬럼 수정 skip: {}", e.getMessage());
        }

        // V3: nickname unique index (skip if already exists)
        try {
            jdbcTemplate.execute("ALTER TABLE users ADD UNIQUE INDEX idx_nickname_unique (nickname)");
            log.info("idx_nickname_unique 인덱스 생성 완료");
        } catch (Exception e) {
            log.debug("idx_nickname_unique 인덱스 skip: {}", e.getMessage());
        }
    }

    private void seedAdminAccount() {
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
