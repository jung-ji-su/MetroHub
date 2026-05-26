CREATE DATABASE IF NOT EXISTS metrohub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE metrohub;

CREATE TABLE IF NOT EXISTS users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    nickname   VARCHAR(100) NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS congestion_info (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    line_number     VARCHAR(20)  NOT NULL,
    station_name    VARCHAR(100) NOT NULL,
    congestion_level INT,
    train_no        VARCHAR(50),
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_line_station (line_number, station_name)
);

CREATE TABLE IF NOT EXISTS community_posts (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    line_number VARCHAR(20)  NOT NULL,
    user_id     BIGINT       NOT NULL,
    title       VARCHAR(255) NOT NULL,
    content     TEXT         NOT NULL,
    is_alert    TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_line_number (line_number),
    INDEX idx_user_id (user_id)
);

CREATE TABLE IF NOT EXISTS community_comments (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id    BIGINT   NOT NULL,
    user_id    BIGINT   NOT NULL,
    content    TEXT     NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_post_id (post_id)
);

CREATE TABLE IF NOT EXISTS complaints (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    category     VARCHAR(100) NOT NULL,
    station_name VARCHAR(100) NOT NULL,
    content      TEXT         NOT NULL,
    status       VARCHAR(50)  NOT NULL DEFAULT 'RECEIVED',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
);

CREATE TABLE IF NOT EXISTS complaint_history (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    complaint_id BIGINT       NOT NULL,
    status       VARCHAR(50)  NOT NULL,
    memo         TEXT,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_complaint_id (complaint_id)
);

CREATE TABLE IF NOT EXISTS notifications (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    type         VARCHAR(50)  NOT NULL,
    title        VARCHAR(255) NOT NULL,
    body         TEXT         NOT NULL,
    reference_id BIGINT,
    is_read      TINYINT(1)   NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_type (type),
    INDEX idx_created_at (created_at)
);
