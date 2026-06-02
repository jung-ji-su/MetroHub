CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS congestion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    line_number VARCHAR(50),
    station_name VARCHAR(100),
    congestion_level VARCHAR(20),
    train_no VARCHAR(50),
    arrival_message VARCHAR(200),
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_line_station (line_number, station_name)
);

CREATE TABLE IF NOT EXISTS congestion_hourly (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    station_name VARCHAR(100),
    line_number VARCHAR(50),
    hour_of_day INT,
    avg_congestion DOUBLE,
    sample_count INT DEFAULT 1,
    UNIQUE KEY uq_station_line_hour (station_name, line_number, hour_of_day)
);

CREATE TABLE IF NOT EXISTS community_posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    line_number VARCHAR(50),
    user_id BIGINT,
    title VARCHAR(255),
    content TEXT,
    alert BOOLEAN DEFAULT FALSE,
    author_nickname VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_community_posts_created_at ON community_posts (created_at);

CREATE TABLE IF NOT EXISTS community_comments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT,
    user_id BIGINT,
    content TEXT,
    author_nickname VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS complaints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    category VARCHAR(100),
    station_name VARCHAR(100),
    content TEXT,
    status VARCHAR(50) DEFAULT 'RECEIVED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
