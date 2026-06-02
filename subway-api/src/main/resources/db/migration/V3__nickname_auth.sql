-- 이메일 필드 nullable로 변경 (닉네임 기반 인증으로 전환)
ALTER TABLE users MODIFY COLUMN email VARCHAR(255) NULL DEFAULT NULL;

-- 닉네임 유니크 인덱스 추가
ALTER TABLE users ADD UNIQUE INDEX idx_nickname_unique (nickname);
