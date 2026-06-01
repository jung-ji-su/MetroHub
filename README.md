# MetroHub

**서울 지하철 실시간 통합 플랫폼** — 혼잡도 조회, 실시간 노선도, 경로 탐색, 알림, 커뮤니티, 민원 접수를 하나의 모바일 웹앱에서.

[![CI/CD](https://github.com/jung-ji-su/MetroHub/actions/workflows/ci.yml/badge.svg)](https://github.com/jung-ji-su/MetroHub/actions/workflows/ci.yml)
[![Collector](https://github.com/jung-ji-su/MetroHub/actions/workflows/collector.yml/badge.svg)](https://github.com/jung-ji-su/MetroHub/actions/workflows/collector.yml)

**Live** → [metrohub-api.onrender.com](https://metrohub-api.onrender.com/actuator/health) · [metrohub-notification.onrender.com](https://metrohub-notification.onrender.com/actuator/health)

> Render 무료 티어 사용 — 콜드 스타트 시 최대 1분 소요될 수 있습니다.

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| **실시간 혼잡도** | 역별·호선별 혼잡도 실시간 조회 및 시간대별 바차트 |
| **실시간 노선도** | 열차 위치 추적 + 상행/하행 방향 애니메이션 + 상세 패널 |
| **경로 탐색** | 0-1 BFS 최소환승 알고리즘으로 최적 경로 시각화 |
| **실시간 알림** | SSE 기반 푸시 알림 + 호선별 알림 구독 설정 |
| **커뮤니티** | 호선별 게시판 + 댓글 + 좋아요 |
| **민원 접수** | 역·열차 기반 민원 등록 및 처리 현황 조회 |
| **트렌딩** | 실시간 조회 급상승 역 위젯 |

---

## 아키텍처

```
[서울 열린데이터 API]
        │
        ▼
[subway-collector]  ──────────────────────────────────── [Kafka]
  Python 스케줄러                                           │
  30초 주기 폴링                              ┌─────────────┴─────────────┐
  GitHub Actions 5분 One-shot               ▼                           ▼
                                     [subway-api]          [subway-notification]
                                     Spring Boot :8080     Spring Boot :8081
                                     JWT 인증              JWT 인증 (SSE)
                                     MySQL                 MySQL
                                     SSE (공개 이벤트)      SSE (사용자별 알림)
                                            │
                                            ▼
                                      [subway-web]
                                      SvelteKit 5
                                      Tailwind CSS v4
                                      max-width 430px
```

### 서비스별 역할

| 서비스 | 역할 | 포트 |
|--------|------|------|
| **subway-collector** | Python 스케줄러, 서울 API 폴링 → Kafka 이벤트 발행 | — |
| **subway-api** | 메인 REST API, JWT 인증, 혼잡도·커뮤니티·민원·노선도 | 8080 |
| **subway-notification** | 알림 구독 관리, Kafka 수신 → 사용자별 SSE push | 8081 |
| **subway-web** | SvelteKit 5 모바일 웹앱 | 5173 |

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| **Backend** | Java 17, Spring Boot 3.2, MyBatis, Spring Security, JWT |
| **Messaging** | Apache Kafka, Zookeeper |
| **Database** | MySQL 8 |
| **Frontend** | SvelteKit 5 (Svelte runes), Tailwind CSS v4, Vite |
| **Infra** | Docker, Docker Compose, Nginx |
| **CI/CD** | GitHub Actions, GHCR, Render |

---

## 빠른 시작

### 1. 환경 변수 설정

```bash
cp .env.example .env
```

`.env` 파일을 열어 값을 채웁니다:

```env
SEOUL_API_KEY=발급받은_실시간지하철_API_키
MYSQL_ROOT_PASSWORD=루트_비밀번호
MYSQL_PASSWORD=metrohub_비밀번호
JWT_SECRET=Base64인코딩된_32바이트_이상_시크릿
```

> **API 키**: [서울 열린데이터광장](https://data.seoul.go.kr) → 지하철 실시간 도착정보 (`realtimeStationArrival`)
>
> **JWT_SECRET 생성 예시**
> ```bash
> echo -n "your-super-secret-key-at-least-32-chars!!" | base64
> ```

### 2. 전체 서비스 실행

```bash
docker-compose up -d

# 프론트엔드 개발 서버
cd subway-web && npm install && npm run dev
```

### 3. 접속

| 서비스 | 주소 |
|--------|------|
| 웹앱 | http://localhost:5173 |
| subway-api | http://localhost:8080 |
| subway-notification | http://localhost:8081 |
| MySQL | localhost:3306 |
| Kafka | localhost:9092 |

```bash
# 로그 확인
docker-compose logs -f subway-api

# 전체 종료 (볼륨 유지)
docker-compose down

# 데이터 포함 완전 초기화 (주의)
docker-compose down -v
```

---

## CI/CD

### 파이프라인 흐름

```
Push to master
      │
      ├── Test subway-api           (JUnit, H2 in-memory)
      ├── Test subway-notification  (JUnit, H2 in-memory)
      ├── Lint subway-collector     (flake8)
      └── Build subway-web          (npm build)
                │
                ▼  (모두 통과 시)
      Docker Build & Push → GHCR
                │
                ▼
      Render Deploy (metrohub-api + metrohub-notification 동시 배포)
```

### 필요한 GitHub Secrets

| Secret | 설명 |
|--------|------|
| `SEOUL_API_KEY` | 서울 열린데이터광장 API 키 |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka 브로커 주소 |
| `KAFKA_SASL_USERNAME` | Kafka SASL 사용자 |
| `KAFKA_SASL_PASSWORD` | Kafka SASL 비밀번호 |
| `RENDER_DEPLOY_HOOK_API` | Render — metrohub-api 배포 훅 URL |
| `RENDER_DEPLOY_HOOK_NOTIFICATION` | Render — metrohub-notification 배포 훅 URL |

> **Render 배포 훅 URL 확인**: Render 대시보드 → 서비스 선택 → Settings → Deploy Hook

### 워크플로 목록

| 파일 | 트리거 | 역할 |
|------|--------|------|
| `ci.yml` | push/PR | 테스트 → Docker 빌드 → Render 자동 배포 |
| `collector.yml` | cron 5분 | 지하철 데이터 수집 (one-shot) |
| `keepalive.yml` | cron 10분 | Render 무료 티어 슬립 방지 |

---

## API 레퍼런스

### 인증 (`subway-api :8080`)

```
POST /api/users/register    회원가입
POST /api/users/login       로그인 → JWT 토큰 반환
GET  /api/users/me          내 프로필          [JWT]
```

### 혼잡도

```
GET /api/congestion/station/{역명}              실시간 혼잡도
GET /api/congestion/line/{호선번호}             호선 전체 혼잡도
GET /api/congestion/station/{역명}/hourly      시간대별 평균 혼잡도 (0–23시)
```

### 실시간 노선도

```
GET /api/line/{lineCode}/trains    열차 위치 목록
```

### 커뮤니티

```
GET  /api/community/line/{line}/posts          게시글 목록
POST /api/community/posts                      게시글 작성   [JWT]
GET  /api/community/posts/{id}/comments        댓글 목록
POST /api/community/posts/{id}/comments        댓글 작성     [JWT]
```

### 민원

```
POST   /api/complaints          민원 접수         [JWT]
GET    /api/complaints/my       내 민원 목록       [JWT]
DELETE /api/complaints/{id}     민원 삭제         [JWT]
```

### SSE (`subway-api :8080`)

```
GET /api/sse/subscribe    공개 이벤트 스트림 (혼잡도 업데이트, 노선 알림, 트렌딩)
```

### 알림 (`subway-notification :8081`)

```
GET    /api/notifications/stream              SSE 연결 (?token=JWT)        [JWT]
GET    /api/notifications/my                  내 알림 목록                  [JWT]
GET    /api/notifications/subscriptions       구독 목록                    [JWT]
POST   /api/notifications/subscriptions       구독 추가                    [JWT]
DELETE /api/notifications/subscriptions/{id}  구독 해제                    [JWT]
```

---

## 프로젝트 구조

```
MetroHub/
├── docker-compose.yml
├── render.yaml                     # Render 배포 설정
├── .github/workflows/
│   ├── ci.yml                      # CI/CD 파이프라인
│   ├── collector.yml               # 데이터 수집 (5분 주기)
│   └── keepalive.yml               # Render 슬립 방지 (10분 주기)
│
├── subway-collector/               # Python 데이터 수집기
│   ├── collector/
│   │   ├── subway_api.py           # 서울 열린데이터 API 클라이언트
│   │   ├── kafka_producer.py       # subway-realtime Kafka 프로듀서
│   │   └── event_publisher.py      # 혼잡도·알림 이벤트 발행
│   ├── collect_once.py             # GitHub Actions one-shot 진입점
│   └── main.py                     # 로컬 스케줄러 (30초 주기)
│
├── subway-api/                     # Spring Boot 메인 API (:8080)
│   └── src/main/java/com/metrohub/api/
│       ├── domain/
│       │   ├── congestion/         # 혼잡도 + 실시간 노선도 + 시간대별 통계
│       │   ├── community/          # 커뮤니티 게시판
│       │   ├── complaint/          # 민원
│       │   ├── user/               # 사용자 인증
│       │   ├── sse/                # 공개 SSE 스트림
│       │   └── trending/           # 트렌딩 역
│       └── global/config/          # Security, JWT, Kafka 설정
│
├── subway-notification/            # Spring Boot 알림 서비스 (:8081)
│   └── src/main/java/com/metrohub/notification/
│       ├── sse/                    # 사용자별 SSE 엔드포인트
│       ├── subscription/           # 호선 구독 관리
│       ├── consumer/               # Kafka Consumer (subway.line.alert)
│       └── global/config/          # JWT Auth Filter, Security Config
│
└── subway-web/                     # SvelteKit 5 프론트엔드
    └── src/
        ├── lib/
        │   ├── api.js              # API 클라이언트
        │   ├── sseStore.js         # 공개 SSE (혼잡도·알림·트렌딩)
        │   ├── notificationStore.js # 인증된 SSE (사용자 알림)
        │   ├── lineStations.js     # 호선별 역 순서 + 메타데이터
        │   └── routeCalculator.js  # 0-1 BFS 최소환승 경로 탐색
        └── routes/
            ├── +page.svelte        # 메인 (혼잡도 + 노선도 + 경로 탭)
            ├── my/                 # 마이페이지 + 구독 설정
            ├── auth/               # 로그인 / 회원가입
            ├── community/          # 커뮤니티 게시판
            └── complaints/         # 민원 접수 + 내 민원 목록
```

---

## DB 스키마

| 테이블 | 설명 |
|--------|------|
| `users` | 회원 (email, password_hash, nickname) |
| `congestion` | 실시간 혼잡도 캐시 |
| `congestion_hourly` | 시간대별 누적 평균 (ON DUPLICATE KEY 점진적 평균) |
| `community_posts` | 커뮤니티 게시글 |
| `community_comments` | 댓글 |
| `complaints` | 민원 (RECEIVED → IN_PROGRESS → COMPLETED) |
| `notifications` | 사용자별 알림 (user_id nullable = 전체 알림) |
| `notification_subscriptions` | 호선 구독 (user_id + sub_type + sub_value UNIQUE) |

---

## 개별 서비스 실행 (개발 환경)

### subway-collector

```bash
cd subway-collector
python -m venv venv && source venv/bin/activate
pip install -r requirements.txt
python main.py
```

### subway-api / subway-notification

```bash
cd subway-api   # 또는 subway-notification
./gradlew bootRun
```

### subway-web

```bash
cd subway-web
npm install
cp .env.example .env   # VITE_API_BASE, VITE_NOTIFICATION_BASE 설정
npm run dev
```

**`.env` 예시 (subway-web)**

```env
VITE_API_BASE=http://localhost:8080
VITE_NOTIFICATION_BASE=http://localhost:8081
```
