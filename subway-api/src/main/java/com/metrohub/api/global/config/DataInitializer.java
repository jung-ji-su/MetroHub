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
        if (!waitForConnection()) {
            log.error("DB 연결 실패 — 스키마 초기화 건너뜀");
            return;
        }
        applySchemaUpdates();
        seedAdminAccount();
    }

    private boolean waitForConnection() {
        for (int i = 1; i <= 12; i++) {
            try {
                jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                log.info("DB 연결 확인 완료");
                return true;
            } catch (Exception e) {
                log.warn("DB 연결 대기 ({}/12)... {}", i, e.getMessage());
                try { Thread.sleep(3_000); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    private void applySchemaUpdates() {
        // V2: refresh_tokens 테이블 (없으면 생성)
        exec("CREATE TABLE IF NOT EXISTS refresh_tokens (" +
             "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
             "user_id BIGINT NOT NULL," +
             "token VARCHAR(255) NOT NULL UNIQUE," +
             "expires_at DATETIME NOT NULL," +
             "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
             "INDEX idx_token (token)," +
             "INDEX idx_user_id (user_id)" +
             ")", "refresh_tokens 테이블 생성");

        // V2: community_posts 인덱스
        exec("ALTER TABLE community_posts ADD INDEX idx_created_at (created_at)",
             "community_posts idx_created_at 추가");

        // V3: users.email nullable
        exec("ALTER TABLE users MODIFY COLUMN email VARCHAR(255) NULL DEFAULT NULL",
             "users.email nullable 적용");

        // V3: nickname 유니크 인덱스
        exec("ALTER TABLE users ADD UNIQUE INDEX idx_nickname_unique (nickname)",
             "idx_nickname_unique 생성");
    }

    private void exec(String sql, String label) {
        try {
            jdbcTemplate.execute(sql);
            log.info("스키마 업데이트 완료: {}", label);
        } catch (Exception e) {
            log.debug("스키마 업데이트 skip (이미 적용됨): {} — {}", label, e.getMessage());
        }
    }

    private void seedAdminAccount() {
        if (userMapper.findByNickname("dev").isEmpty()) {
            userMapper.insert(User.builder()
                    .nickname("dev")
                    .password(passwordEncoder.encode("1234"))
                    .role("ADMIN")
                    .build());
            log.info("dev 관리자 계정 생성 완료 (초기값: dev / 1234)");
        } else {
            // 이미 존재하면 role만 ADMIN으로 보장, 비밀번호는 DB에 저장된 값 유지
            jdbcTemplate.update("UPDATE users SET role = 'ADMIN' WHERE nickname = 'dev' AND role != 'ADMIN'");
        }
    }
}
