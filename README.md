# MetroHub

**서울 지하철 실시간 통합 플랫폼** — 혼잡도 조회, 실시간 노선도, 경로 탐색, SSE 알림, 커뮤니티, 민원 접수를 하나의 모바일 웹앱에서.

[![CI/CD](https://github.com/jung-ji-su/MetroHub/actions/workflows/ci.yml/badge.svg)](https://github.com/jung-ji-su/MetroHub/actions/workflows/ci.yml)
[![Collector](https://github.com/jung-ji-su/MetroHub/actions/workflows/collector.yml/badge.svg)](https://github.com/jung-ji-su/MetroHub/actions/workflows/collector.yml)

**🌐 서비스** → [metro-hub-mu.vercel.app](https://metro-hub-mu.vercel.app)

**API** → [metrohub-api.onrender.com](https://metrohub-api.onrender.com/actuator/health) · [metrohub-notification.onrender.com](https://metrohub-notification.onrender.com/actuator/health)

> Render 무료 티어 사용 — 첫 요청 시 콜드 스타트 최대 1분 소요될 수 있습니다.

---

## 목차

1. [주요 기능](#주요-기능)
2. [아키텍처](#아키텍처)
3. [Kafka 스트림 파이프라인](#kafka-스트림-파이프라인)
4. [기술 스택](#기술-스택)
5. [빠른 시작 (Docker Compose)](#빠른-시작-docker-compose)
6. [Kubernetes 배포](#kubernetes-배포)
7. [Jenkins CI/CD](#jenkins-cicd)
8. [API 레퍼런스](#api-레퍼런스)
9. [프로젝트 구조](#프로젝트-구조)
10. [DB 스키마](#db-스키마)

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| **실시간 혼잡도** | 역별·호선별 혼잡도 실시간 조회 및 시간대별 바차트 |
| **실시간 노선도** | 열차 위치 추적 + 상행/하행 방향 애니메이션 + 상세 패널 |
| **경로 탐색** | 0-1 BFS 최소환승 알고리즘으로 최적 경로 시각화 |
| **실시간 알림** | SSE 기반 푸시 알림 + 호선별 알림 구독 설정 |
| **혼잡 경보** | Kafka 스트림 처리 — 혼잡도 ≥ 80% 시 실시간 경보 카드 표시 |
| **커뮤니티** | 호선별 게시판 + 댓글 + 좋아요 |
| **민원 접수** | 역·열차 기반 민원 등록 및 처리 현황 조회 |
| **트렌딩** | 실시간 조회 급상승 역 위젯 |

---

## 아키텍처

```
[서울 열린데이터 API]
        │
        ▼
[subway-collector] ──► Kafka: subway-realtime
  Python 30초 폴링
  GitHub Actions 5분 One-shot
                                    │
              ┌─────────────────────┴──────────────────────┐
              ▼                                            ▼
       [subway-api]                          [subway-notification]
       Spring Boot :8080                     Spring Boot :8081
       ├ 혼잡도 REST API                      ├ Kafka Consumer
       ├ 커뮤니티 / 민원 / 인증               ├ 사용자별 SSE 알림
       ├ Kafka Consumer (subway-realtime)    └ 호선 구독 관리
       ├ Kafka Producer (congestion-alerts)
       └ SSE 공개 스트림
              │
              ▼
       [subway-web]
       SvelteKit 5 + Tailwind CSS v4
       max-width 430px 모바일 최적화

─────────────────────────────────────────────────────────────
인프라 (Kubernetes — Docker Desktop)

  Ingress (nginx)
    ├── /                    → subway-web          (replicas: 2)
    ├── /api                 → subway-api          (replicas: 2, HPA)
    └── /api/notifications   → subway-notification (replicas: 1)

  내부 서비스
    ├── MySQL 8      (PVC 10Gi)
    ├── Kafka        (PVC 5Gi, Recreate 전략)
    └── Zookeeper

─────────────────────────────────────────────────────────────
CI/CD (Jenkins + GitHub Webhook)

  GitHub Push
    └── Webhook → Jenkins (ngrok 터널)
          ├── Test (병렬)
          ├── Docker Build & Push → GHCR
          └── kubectl set image → k8s 롤링 배포
```

---

## Kafka 스트림 파이프라인

단순 pub/sub을 넘어 **조건 기반 실시간 스트림 처리**를 구현했습니다.

```
subway-realtime (topic)
        │
        ▼
SubwayRealtimeConsumer
  ├── MySQL upsert (혼잡도 저장)
  └── CongestionAlertProducer.publishIfNeeded()
            │ 혼잡도 ≥ 80% (CROWDED / VERY_CROWDED)
            │ 5분 쿨다운 (역+호선 단위)
            ▼
  congestion-alerts (topic)
            │
            ▼
  CongestionAlertConsumer
            │
            ▼
  SSE 브로드캐스트 → 프론트엔드 경보 카드
```

### 처리 규칙

| 혼잡도 | 경보 등급 | 색상 |
|--------|-----------|------|
| ≥ 90% | `VERY_CROWDED` | 빨간색 🚨 |
| ≥ 80% | `CROWDED` | 주황색 🥵 |
| < 80% | 경보 없음 | — |

- **쿨다운**: 동일 역+호선 조합은 5분 이내 중복 경보 억제 (`ConcurrentHashMap`)
- **파티셔닝**: `lineNumber`를 Kafka 파티션 키로 사용 → 같은 호선 이벤트 순서 보장
- **자동 해제**: 프론트엔드에서 5분 후 경보 카드 자동 제거

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| **Frontend** | SvelteKit 5 (Svelte runes: `$state`, `$derived`, `$effect`), Tailwind CSS v4, Vite |
| **Backend** | Java 17, Spring Boot 3.2, MyBatis (XML mapper), Spring Security, JWT |
| **Messaging** | Apache Kafka 7.5 (Confluent), Zookeeper |
| **Database** | MySQL 8 |
| **Container** | Docker, Docker Compose |
| **Orchestration** | Kubernetes (Docker Desktop), nginx Ingress Controller, HPA |
| **CI/CD** | Jenkins (Docker), GitHub Webhook, GHCR (GitHub Container Registry) |
| **Cloud** | Render (무료 티어), GitHub Actions (keepalive·collector) |

---

## 빠른 시작 (Docker Compose)

### 1. 환경 변수 설정

```bash
cp .env.example .env
```

| 변수 | 설명 |
|------|------|
| `SEOUL_API_KEY` | [서울 열린데이터광장](https://data.seoul.go.kr) API 키 |
| `MYSQL_ROOT_PASSWORD` | MySQL root 비밀번호 |
| `MYSQL_PASSWORD` | metrohub 계정 비밀번호 |
| `JWT_SECRET` | Base64 인코딩된 서명 키 (`echo -n "your-key" \| base64`) |

### 2. 전체 서비스 실행

```bash
docker compose up -d

# 프론트엔드 개발 서버
cd subway-web && npm install && npm run dev
```

### 3. 접속 주소

| 서비스 | 주소 |
|--------|------|
| 웹앱 (dev) | http://localhost:5173 |
| subway-api | http://localhost:8080 |
| subway-notification | http://localhost:8081 |
| MySQL | localhost:3306 |
| Kafka | localhost:9092 |

```bash
# 로그 확인
docker compose logs -f subway-api

# 전체 종료 (볼륨 유지)
docker compose down

# 데이터 포함 완전 초기화 (주의)
docker compose down -v
```

---

## Kubernetes 배포

Docker Desktop에 내장된 Kubernetes를 사용합니다.

### 사전 준비

1. Docker Desktop → Settings → Kubernetes → **Enable Kubernetes** 체크
2. nginx Ingress Controller 설치:

```bash
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.1/deploy/static/provider/cloud/deploy.yaml
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=90s
```

3. hosts 파일 등록 (최초 1회, 관리자 권한 PowerShell):

```powershell
Add-Content -Path "C:\Windows\System32\drivers\etc\hosts" -Value "127.0.0.1 metrohub.local"
```

### 배포 스크립트 실행

```powershell
.\k8s-deploy.ps1
```

스크립트가 다음을 순서대로 수행합니다:

1. Namespace / Secret / ConfigMap 적용
2. MySQL → Zookeeper → Kafka 순서로 기동 (Ready 대기)
3. subway-api / subway-notification / subway-collector / subway-web 배포
4. HPA / Ingress 적용

완료 후 `http://metrohub.local` 접속.

### 배포 구성

```
k8s/
├── namespace.yaml
├── secrets.yaml                    # MySQL PW, JWT 시크릿, API 키 (base64)
├── configmap.yaml                  # 공통 환경변수 (DB URL, Kafka 주소 등)
├── ingress.yaml                    # nginx 라우팅 규칙
├── hpa.yaml                        # subway-api HPA (CPU 70% 기준, 2–5 replicas)
├── mysql/
│   ├── configmap.yaml              # DB 초기화 SQL (V1~V4 마이그레이션 통합)
│   ├── deployment.yaml
│   ├── service.yaml
│   └── pvc.yaml                    # 10Gi
├── kafka/
│   ├── zookeeper.yaml
│   ├── kafka.yaml
│   └── kafka-pvc.yaml              # 5Gi
├── subway-api/
├── subway-notification/
├── subway-collector/
└── subway-web/
```

### 주요 k8s 설계 포인트

| 항목 | 설정 | 이유 |
|------|------|------|
| Kafka `strategy.type` | `Recreate` | PVC ReadWriteOnce — 동시 마운트 불가, 구 파드 완전 종료 후 신 파드 시작 |
| Kafka `enableServiceLinks` | `false` | k8s가 `KAFKA_*` env 자동 주입 → Confluent 브로커 설정 충돌 방지 |
| Kafka `initContainer` | `.lock` 삭제 + chown | 재시작 시 stale lock 파일 및 PVC 권한 오류 방지 |
| Kafka `securityContext.fsGroup` | `1000` | Confluent 이미지 appuser(gid 1000)의 PVC 쓰기 권한 부여 |
| Kafka Service `publishNotReadyAddresses` | `true` | 브로커 자기 자신 연결 시 치킨-에그 문제 해결 |
| JWT Secret | double-base64 | `JwtUtil`이 env 값을 base64 디코딩하므로 `secrets.yaml`에서 한 번 더 인코딩 필요 |

### 파드 상태 확인

```bash
kubectl get pods -n metrohub
kubectl get ingress -n metrohub
```

---

## Jenkins CI/CD

### 파이프라인 흐름

```
GitHub Push (master)
      │
      ▼ Webhook → ngrok → Jenkins :8090
┌─────────────────────────────────────┐
│  Test (병렬)                        │
│  ├── subway-api      JUnit          │
│  ├── subway-notification JUnit      │
│  ├── subway-collector flake8        │
│  └── subway-web      npm build      │
└──────────────┬──────────────────────┘
               │ 전체 통과 시
               ▼
┌─────────────────────────────────────┐
│  Docker Build & Push (병렬)         │
│  metrohub-{api|notification|        │
│            collector|web}           │
│  태그: :BUILD_NUMBER + :latest      │
│  레지스트리: ghcr.io/jung-ji-su     │
└──────────────┬──────────────────────┘
               │
               ▼
┌─────────────────────────────────────┐
│  Deploy                             │
│  kubectl set image (버전 태그)       │
│  kubectl rollout status (완료 대기) │
└─────────────────────────────────────┘
```

이미지가 `BUILD_NUMBER` 태그로 GHCR에 누적 저장되므로  
`kubectl rollout undo deployment/<name> -n metrohub` 으로 이전 버전 롤백이 가능합니다.

### Jenkins 로컬 실행

```powershell
cd jenkins
.\start.ps1
# → http://localhost:8090 에서 초기 비밀번호 출력
```

`jenkins/Dockerfile`에는 다음이 포함됩니다:
- Jenkins LTS (JDK 21)
- Docker CLI (docker.sock 마운트)
- kubectl
- Node.js 20
- Python 3 + pip

### 필요한 Credential

| ID | 종류 | 발급 |
|----|------|------|
| `ghcr-token` | Secret text | GitHub → Settings → Developer settings → PAT (`write:packages`) |

### GitHub Webhook 연결

```bash
# ngrok으로 Jenkins 외부 노출
docker run -d --name ngrok-jenkins \
  -p 4040:4040 \
  -e NGROK_AUTHTOKEN=<your-ngrok-token> \
  ngrok/ngrok:latest \
  http host.docker.internal:8090

# 터널 URL 확인
curl http://localhost:4040/api/tunnels
```

GitHub 리포지터리 → Settings → Webhooks:
- **Payload URL**: `https://<ngrok-url>/github-webhook/`
- **Content type**: `application/json`
- **Events**: `Just the push event`

---

## API 레퍼런스

### 인증 (`subway-api :8080`)

```
POST /api/users/register    회원가입
POST /api/users/login       로그인 → JWT 토큰 반환
GET  /api/users/me          내 프로필                    [JWT]
```

### 혼잡도

```
GET /api/congestion/station/{역명}             실시간 혼잡도
GET /api/congestion/line/{호선번호}            호선 전체 혼잡도
GET /api/congestion/station/{역명}/hourly     시간대별 평균 혼잡도 (0–23시)
```

### 실시간 노선도

```
GET /api/line/{lineCode}/trains    열차 위치 목록
```

### 커뮤니티

```
GET    /api/community/line/{line}/posts       게시글 목록
POST   /api/community/posts                   게시글 작성      [JWT]
DELETE /api/community/posts/{id}              게시글 삭제      [JWT]
GET    /api/community/posts/{id}/comments     댓글 목록
POST   /api/community/posts/{id}/comments     댓글 작성        [JWT]
POST   /api/community/posts/{id}/like         좋아요 토글      [JWT]
GET    /api/community/my                      내 게시글 목록   [JWT]
```

### 민원

```
POST   /api/complaints           민원 접수          [JWT]
GET    /api/complaints/my        내 민원 목록        [JWT]
DELETE /api/complaints/{id}      민원 삭제           [JWT]
```

### SSE 이벤트 (`subway-api :8080`)

```
GET /api/sse/subscribe    공개 이벤트 스트림
```

| 이벤트명 | 발생 조건 |
|----------|-----------|
| `congestion.update` | 혼잡도 데이터 갱신 |
| `congestion.alert` | 혼잡도 ≥ 80% (Kafka 스트림 처리 결과) |
| `line.alert` | 노선 장애 알림 |
| `trending.update` | 트렌딩 역 갱신 |

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
├── render.yaml                         # Render 배포 설정
├── Jenkinsfile                         # Jenkins CI/CD 파이프라인 정의
├── k8s-deploy.ps1                      # k8s 원클릭 배포 스크립트 (PowerShell)
│
├── .github/workflows/
│   ├── ci.yml                          # Docker 빌드 → GHCR 푸시
│   ├── collector.yml                   # 데이터 수집 (5분 주기)
│   └── keepalive.yml                   # Render 슬립 방지 (10분 주기)
│
├── jenkins/                            # Jenkins 로컬 실행 환경
│   ├── Dockerfile                      # Docker CLI + kubectl + Node.js + Python 포함 이미지
│   ├── docker-compose.yml              # docker.sock·kubeconfig 마운트
│   └── start.ps1                       # 컨테이너 기동 + 초기 비밀번호 출력
│
├── k8s/                                # Kubernetes 매니페스트
│   ├── namespace.yaml
│   ├── secrets.yaml
│   ├── configmap.yaml
│   ├── ingress.yaml
│   ├── hpa.yaml
│   ├── mysql/
│   ├── kafka/
│   ├── subway-api/
│   ├── subway-notification/
│   ├── subway-collector/
│   └── subway-web/
│
├── subway-collector/                   # Python 데이터 수집기
│   ├── collector/
│   │   ├── subway_api.py
│   │   ├── kafka_producer.py           # subway-realtime 토픽 발행
│   │   └── event_publisher.py
│   ├── collect_once.py                 # GitHub Actions one-shot 진입점
│   └── main.py                         # 로컬 스케줄러 (30초 주기)
│
├── subway-api/                         # Spring Boot 메인 API (:8080)
│   └── src/main/java/com/metrohub/api/
│       ├── domain/
│       │   ├── congestion/
│       │   │   ├── SubwayRealtimeConsumer.java    # Kafka Consumer → DB upsert + 경보 트리거
│       │   │   ├── CongestionAlertProducer.java   # congestion-alerts 발행 (5분 쿨다운)
│       │   │   └── CongestionAlertConsumer.java   # congestion-alerts → SSE 브로드캐스트
│       │   ├── community/
│       │   ├── complaint/
│       │   ├── user/
│       │   ├── sse/
│       │   └── trending/
│       └── global/config/
│
├── subway-notification/                # Spring Boot 알림 서비스 (:8081)
│   └── src/main/java/com/metrohub/notification/
│       ├── sse/
│       ├── subscription/
│       ├── consumer/
│       └── global/config/
│
└── subway-web/                         # SvelteKit 5 프론트엔드
    ├── Dockerfile                      # node:20-alpine 빌드 → nginx:1.27-alpine 서빙
    ├── nginx.conf                      # SPA fallback (try_files → /index.html)
    └── src/
        ├── lib/
        │   ├── api.js
        │   ├── sseStore.js             # congestion.alert 수신 + 5분 자동 해제 타이머
        │   ├── notificationStore.js
        │   ├── lineStations.js         # 전 노선 역 목록 + LINE_META
        │   └── routeCalculator.js      # 0-1 BFS 최소환승 경로 탐색
        └── routes/
            ├── +page.svelte            # 혼잡 경보 카드 (animate-pulse)
            ├── my/
            ├── auth/
            ├── community/
            └── complaints/
```

---

## DB 스키마

| 테이블 | 설명 |
|--------|------|
| `users` | 회원 (username, password_hash, nickname) |
| `congestion_data` | 실시간 혼잡도 캐시 (역별·호선별) |
| `congestion_hourly` | 시간대별 누적 평균 (ON DUPLICATE KEY 점진적 평균) |
| `community_posts` | 게시글 (호선 태그, 알림 플래그) |
| `community_comments` | 댓글 |
| `post_likes` | 좋아요 (user_id + post_id UNIQUE) |
| `complaints` | 민원 (RECEIVED → IN_PROGRESS → COMPLETED) |
| `notifications` | 사용자별 알림 (user_id nullable = 전체 알림) |
| `subscriptions` | 호선 구독 (user_id + line_number UNIQUE) |

MySQL 데이터는 Docker volume `mysql-data`에 저장됩니다.  
`docker compose down -v` 실행 시 데이터가 삭제됩니다.

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
