# MetroHub - 지하철 통합 플랫폼

지하철 실시간 혼잡도 조회, 호선별 커뮤니티, 민원 신청 기능을 제공하는 마이크로서비스 플랫폼입니다.

## 기술 스택

| 서비스 | 기술 |
|--------|------|
| subway-collector | Python 3.11, kafka-python, schedule |
| subway-api | Java 17, Spring Boot 3.2, MyBatis, MySQL, Spring Security, JWT |
| subway-notification | Java 17, Spring Boot 3.2, Spring Kafka |
| 인프라 | Apache Kafka, Zookeeper, MySQL 8 |
| CI/CD | GitHub Actions |
| 배포 | Docker, Docker Compose |

## 아키텍처

```
[서울 열린데이터 API]
        │
        ▼
[subway-collector] ──(subway-realtime)──▶ [Kafka]
                                              │
                         ┌────────────────────┼────────────────────┐
                         ▼                    ▼                    ▼
                   [subway-api]      [community-events]   [complaint-events]
                   (REST API)               │                      │
                   MySQL ◀──┘         [subway-notification] ◀──────┘
```

## 주요 API

### 인증
- `POST /api/users/register` - 회원가입
- `POST /api/users/login` - 로그인 (JWT 토큰 발급)
- `GET  /api/users/me` - 내 프로필

### 혼잡도
- `GET /api/congestion/station/{stationName}` - 역별 혼잡도
- `GET /api/congestion/line/{lineNumber}` - 호선별 혼잡도

### 커뮤니티
- `GET  /api/community/line/{lineNumber}/posts` - 호선별 게시글 목록
- `POST /api/community/posts` - 게시글 작성
- `GET  /api/community/posts/{postId}/comments` - 댓글 목록
- `POST /api/community/posts/{postId}/comments` - 댓글 작성

### 민원
- `POST /api/complaints` - 민원 접수
- `GET  /api/complaints/my` - 내 민원 목록
- `GET  /api/complaints/{id}` - 민원 상세

## 로컬 실행 방법

### 사전 요구사항
- Docker 및 Docker Compose 설치
- 서울 열린데이터광장 API 키 발급 ([https://data.seoul.go.kr](https://data.seoul.go.kr))

### 환경 변수 설정

```bash
cp .env.example .env
```

`.env` 파일을 열어 값을 채워 넣습니다:

```env
SEOUL_API_KEY=발급받은_API_키
MYSQL_ROOT_PASSWORD=안전한_루트_비밀번호
MYSQL_PASSWORD=metrohub_db_비밀번호
JWT_SECRET=Base64_인코딩된_시크릿_키
```

> JWT_SECRET은 32바이트 이상의 문자열을 Base64 인코딩한 값이어야 합니다.
> 예시: `echo -n "your-super-secret-key-at-least-32-chars" | base64`

### 실행

```bash
# 전체 서비스 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f

# 특정 서비스 로그
docker-compose logs -f subway-api
```

### 서비스 포트

| 서비스 | 포트 |
|--------|------|
| subway-api | http://localhost:8080 |
| subway-notification | http://localhost:8081 |
| MySQL | localhost:3306 |
| Kafka | localhost:9092 |
| Zookeeper | localhost:2181 |

### 종료

```bash
docker-compose down

# 볼륨 포함 완전 삭제
docker-compose down -v
```

## 개발 환경 설정

### subway-collector (Python)

```bash
cd subway-collector
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
cp ../.env.example .env
python main.py
```

### subway-api (Spring Boot)

```bash
cd subway-api
# 로컬 MySQL, Kafka가 실행 중인 상태에서
gradle bootRun
```

### subway-notification (Spring Boot)

```bash
cd subway-notification
gradle bootRun
```

## 프로젝트 구조

```
MetroHub/
├── docker-compose.yml
├── .env.example
├── mysql/
│   └── init.sql                    # DB 초기 스키마
├── .github/
│   └── workflows/
│       └── ci.yml                  # GitHub Actions CI
├── subway-collector/               # Python 데이터 수집기
│   ├── collector/
│   │   ├── subway_api.py           # 서울 열린데이터 API 클라이언트
│   │   └── kafka_producer.py       # Kafka 프로듀서
│   └── main.py                     # 스케줄러 (30초 주기)
├── subway-api/                     # Spring Boot 메인 API
│   └── src/main/java/com/metrohub/api/
│       ├── domain/
│       │   ├── congestion/         # 혼잡도 도메인
│       │   ├── community/          # 커뮤니티 도메인
│       │   ├── complaint/          # 민원 도메인
│       │   └── user/               # 사용자 도메인
│       └── global/
│           ├── config/             # Security, Kafka, JWT 설정
│           └── exception/          # 전역 예외 처리
└── subway-notification/            # Spring Boot 알림 서비스
    └── src/main/java/com/metrohub/notification/
        ├── consumer/               # Kafka Consumer
        └── config/                 # Kafka Consumer 설정
```
