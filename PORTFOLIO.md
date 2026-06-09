# MetroHub — 서울 지하철 실시간 통합 플랫폼

> **이력서 / 노션 포트폴리오 제출용** · 2025

[![CI/CD](https://github.com/jung-ji-su/MetroHub/actions/workflows/ci.yml/badge.svg)](https://github.com/jung-ji-su/MetroHub/actions/workflows/ci.yml)
[![Collector](https://github.com/jung-ji-su/MetroHub/actions/workflows/collector.yml/badge.svg)](https://github.com/jung-ji-su/MetroHub/actions/workflows/collector.yml)

🌐 **서비스** → [metro-hub-mu.vercel.app](https://metro-hub-mu.vercel.app) &nbsp;|&nbsp; 🐙 **GitHub** → [jung-ji-su/MetroHub](https://github.com/jung-ji-su/MetroHub)

---

## 📌 Executive Summary

**MetroHub**는 서울 지하철 이용자를 위한 실시간 모바일 웹 플랫폼입니다.  
서울시 공공 API에서 수집한 열차 도착·혼잡도 데이터를 **Kafka 스트림 파이프라인**으로 처리하고,  
**SSE(Server-Sent Events)**를 통해 별도의 폴링 없이 클라이언트에 실시간으로 전달합니다.

이 프로젝트를 통해 증명하고자 한 핵심 역량은 다음 세 가지입니다.

| 역량 | 구현 포인트 |
|------|------------|
| **대용량 실시간 데이터 처리** | 24개 노선 × 수백 개 역을 `CompletableFuture` 병렬 조회 + Kafka 스트림 처리 |
| **분산 인프라 설계·운영** | MSA 2-서비스 분리, Kubernetes 멀티 컴포넌트 배포, 예외 상황 대응 설계 |
| **CI/CD 자동화** | GitHub Actions(클라우드) + Jenkins(로컬 k8s) 이원화 파이프라인 |

---

## 🏗️ 아키텍처 및 데이터 흐름

### 전체 시스템 구성

```
┌─────────────────────┐
│  서울 열린데이터 API  │  (realtimeStationArrival)
└──────────┬──────────┘
           │ HTTP Polling (30초 / 5분 One-shot)
           ▼
┌─────────────────────┐
│   subway-collector   │  Python 수집기
│  kafka_producer.py   │──► Topic: subway-realtime
└─────────────────────┘
                │
   ┌────────────┴────────────┐
   ▼                         ▼
┌──────────────────┐   ┌──────────────────────┐
│   subway-api     │   │ subway-notification   │
│  Spring Boot     │   │   Spring Boot :8081   │
│     :8080        │   │                       │
│ ├ Kafka Consumer │   │ ├ Kafka Consumer       │
│ ├ DB upsert      │   │ ├ 사용자별 SSE 스트림  │
│ ├ 혼잡 경보 판단 │   │ └ 호선 구독 관리      │
│ ├ Kafka Producer │   └──────────────────────┘
│ └ 공개 SSE 스트림│
└──────────────────┘
           │
           ▼
┌─────────────────────┐
│     subway-web       │  SvelteKit 5 (max-width 430px)
│  Vercel 자동 배포    │  SSE 수신 → 실시간 UI 갱신
└─────────────────────┘
```

---

### ⚙️ MSA 분리를 선택한 기술적 이유

> **"SSE 커넥션이 메인 API의 스레드를 고갈시키지 않도록"**

단일 서버 구조에서 SSE는 치명적인 문제를 내포합니다. HTTP/1.1 기반 SSE는 클라이언트가 연결을 유지하는 동안 서버 측 스레드(또는 소켓 슬롯)를 점유합니다. 사용자가 증가할수록 SSE 커넥션이 쌓이고, 이는 결국 **Tomcat 스레드 풀 고갈 → 일반 REST 요청 타임아웃**으로 이어집니다.

이를 방지하기 위해 개인화된 알림 SSE 역할을 `subway-notification` 서비스로 분리하였습니다.

| 서비스 | 역할 | 분리 이유 |
|--------|------|----------|
| `subway-api :8080` | 혼잡도 REST, 커뮤니티, 민원, 인증, **공개 SSE** | 빠른 트랜잭션 처리 최우선 |
| `subway-notification :8081` | **개인화 SSE**, 구독 관리, 알림 이력 | Long-lived 커넥션 격리 |

두 서비스를 분리함으로써 알림 SSE 트래픽이 급증하더라도 메인 API의 응답성이 보장되는 **장애 격리(Fault Isolation)** 구조를 확보했습니다.

---

### 🔄 Kafka 파이프라인의 기술적 당위성

외부 API 직접 호출 방식이 아닌 **Kafka 기반 스트림 처리**를 선택한 이유는 다음과 같습니다.

```
[직접 호출 방식의 문제]
  클라이언트 → API 서버 → 서울시 API
  - 서울시 API 응답 지연이 클라이언트 지연으로 직결
  - API 호출 한도(Rate Limit) 소진 시 서비스 전면 장애
  - 복수의 서버 인스턴스가 API를 중복 호출

[Kafka 파이프라인 방식의 이점]
  collector(생산) → Kafka → consumer(소비)
  - 수집과 소비의 완전한 디커플링 → 외부 API 장애가 내부로 전파되지 않음
  - 데이터가 Kafka에 적재되면 N개의 Consumer가 독립적으로 처리 가능
  - MySQL upsert + 혼잡 경보 판단을 동일 메시지로 처리 (Single Source of Truth)
  - lineNumber를 파티션 키로 설정 → 같은 호선 이벤트의 순서 보장
```

---

## 🔬 엔지니어링 디테일 — 핵심 구현 분석

### 1. Kafka 스트림 처리 — 혼잡 경보 중복 억제 설계

단순한 임계값 판단을 넘어, **운영 환경의 노이즈(Noise) 문제**를 해결하는 데 집중했습니다.

서울 지하철 데이터는 30초 간격으로 폴링됩니다. 특정 역의 혼잡도가 80% 이상인 상태가 10분간 지속된다면, 단순 구현에서는 20번의 경보가 발송됩니다. 이는 사용자 경험을 심각하게 훼손하고 알림 피로(Alert Fatigue)를 유발합니다.

**해결책: `ConcurrentHashMap` 기반 쿨다운 메커니즘**

```java
// CongestionAlertProducer.java
private static final long COOLDOWN_MS = 5 * 60 * 1000L; // 5분 쿨다운

// 역명:노선코드 를 복합 키로 사용 — 같은 역의 다른 호선은 독립 관리
private final ConcurrentHashMap<String, Long> lastAlertTime = new ConcurrentHashMap<>();

public void publishIfNeeded(String stationName, String lineNumber, int congestionLevel) {
    if (congestionLevel < THRESHOLD_CROWDED) return;

    String key = stationName + ":" + lineNumber;
    long now = System.currentTimeMillis();
    Long last = lastAlertTime.get(key);
    if (last != null && (now - last) < COOLDOWN_MS) return; // 쿨다운 중이면 조기 반환

    lastAlertTime.put(key, now);
    // ... Kafka 발행
    kafkaTemplate.send(TOPIC, lineNumber, payload); // lineNumber = 파티션 키
}
```

**설계 포인트 3가지:**

- **복합 키 설계** (`역명:노선코드`): 강남역에 2호선과 신분당선이 모두 지나가므로, 노선을 키에 포함시켜 호선별로 독립적인 쿨다운을 적용했습니다.
- **Lock-free 동시성**: Kafka Consumer 스레드가 복수로 실행되는 환경에서 `synchronized` 블록 없이 `ConcurrentHashMap`의 원자적 연산으로 스레드 안전성을 확보했습니다.
- **파티션 키를 `lineNumber`로 설정**: 동일 호선의 이벤트가 항상 같은 파티션으로 라우팅되어, Consumer가 처리 순서를 보장받습니다. 이는 단순 이벤트 발행이 아닌 **스트림의 순서 의미론(Ordering Semantics)**을 고려한 설계입니다.

**경보 등급 분류:**

| 혼잡도 수치 | 등급 | 색상 | 처리 |
|------------|------|------|------|
| ≥ 90% | `VERY_CROWDED` | 🔴 빨간색 | 즉시 경보 발행 |
| ≥ 80% | `CROWDED` | 🟠 주황색 | 즉시 경보 발행 |
| < 80% | 정상 | — | 이벤트 미발행 |

프론트엔드에서는 경보 수신 후 **5분 타이머**를 설정하여, 별도 신호 없이도 경보 카드가 자동으로 사라지도록 구현했습니다.

---

### 2. 실시간 노선도 — 전체 역 병렬 조회 + 중복 제거 로직

서울 지하철 1호선의 경우 소요산부터 신창까지 100개 이상의 역이 존재합니다. 이 모든 역의 실시간 도착 정보를 순차적으로 조회하면 수십 초가 소요됩니다.

**해결책: `CompletableFuture` + 전용 I/O 스레드풀 + `ConcurrentHashMap` 중복 제거**

```java
// SubwayLineService.java

// I/O 전용 스레드풀 — ForkJoinPool.commonPool이 아닌 별도 풀 사용
private static final ExecutorService FETCH_POOL =
    Executors.newFixedThreadPool(50, r -> {
        Thread t = new Thread(r, "subway-fetch");
        t.setDaemon(true);
        return t;
    });

// 전체 역 병렬 조회 — 결과를 trainNo 기준으로 중복 제거 (ETA 최솟값 유지)
Map<String, ArrivalDetail> trainMap = new ConcurrentHashMap<>();
List<CompletableFuture<Void>> futures = stations.stream()
    .map(station -> CompletableFuture.runAsync(() -> {
        List<ArrivalDetail> details = seoulClient.fetchArrivalDetails(station);
        for (ArrivalDetail d : details) {
            if (!lineCode.equals(d.getLineCode())) continue;
            trainMap.merge(d.getTrainNo(), d, (existing, newer) ->
                newer.getEtaSeconds() < existing.getEtaSeconds() ? newer : existing);
        }
    }, FETCH_POOL))
    .collect(Collectors.toList());
CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
```

**이 설계가 해결하는 문제:**

서울시 API는 역 이름으로 조회하면 해당 역에 접근 중인 모든 열차를 반환합니다. 열차 하나가 여러 역에 동시에 "접근 중"으로 표시되므로, 단순 수집 시 동일 열차가 3~4회 중복 등장합니다. `trainMap.merge()`의 `remappingFunction`에서 **ETA가 더 작은(= 더 가까운 역의) 데이터를 선택**함으로써, 열차의 실제 현재 위치를 정확히 특정합니다.

- **`ForkJoinPool.commonPool` 미사용**: 공용 풀은 CPU 코어 수에 묶여 있어 I/O 바운드 작업에 부적합합니다. 명시적인 50-스레드 전용 풀을 선언하여 네트워크 대기 시간 동안 다른 역 조회가 블로킹되지 않도록 했습니다.
- **10초 서버 사이드 캐시**: 동일 노선에 대한 중복 요청이 들어와도 10초 이내엔 캐시된 결과를 즉시 반환, 외부 API 과부하를 차단합니다.
- **급행·특급 통과역 필터링**: `btrainSttus` 필드와 `arvlMsg2` (도착 메시지)를 교차 검증하여, 급행 열차가 정차하지 않는 역을 다음 역으로 표시하는 버그를 제거했습니다.

---

### 3. 경로 탐색 — 0-1 BFS로 최소 환승 경로 계산

**왜 일반 BFS가 아닌 0-1 BFS인가?**

일반 BFS는 모든 간선의 비용이 동일할 때 최단 거리를 보장합니다. 지하철 경로 탐색에서 "비용"은 **환승 횟수**입니다. 같은 노선 내 이동(비용 0)과 환승(비용 1)이 혼재하는 이 문제는 **0-1 BFS(Deque를 활용한 BFS)**가 Dijkstra보다 간단하고 효율적으로 해결합니다.

```javascript
// routeCalculator.js — 핵심 알고리즘

export function findRoute(from, to) {
  const dist = new Map();   // key: "역명:노선코드" → 최소 환승 횟수
  const prev = new Map();   // 경로 역추적용
  const deque = [];

  // 출발역의 모든 노선을 환승 비용 0으로 초기화
  for (const line of STATION_LINES[from]) {
    const key = `${from}:${line}`;
    dist.set(key, 0);
    deque.push({ station: from, line, transfers: 0 });
  }

  while (deque.length > 0) {
    const { station, line, transfers } = deque.shift();

    if (station === to) return buildSegments(prev, `${station}:${line}`);

    // 같은 노선의 인접 역 → 비용 0 → deque 앞에 삽입 (unshift)
    for (const delta of [-1, 1]) {
      const ns = ls[idx + delta];
      const nk = `${ns}:${line}`;
      if (transfers < (dist.get(nk) ?? Infinity)) {
        dist.set(nk, transfers);
        prev.set(nk, { key, transfer: false });
        deque.unshift({ station: ns, line, transfers }); // ← 비용 0: 앞 삽입
      }
    }

    // 환승 → 비용 1 → deque 뒤에 삽입 (push)
    for (const otherLine of STATION_LINES[station] ?? []) {
      if (otherLine === line) continue;
      const ok = `${station}:${otherLine}`;
      if (transfers + 1 < (dist.get(ok) ?? Infinity)) {
        dist.set(ok, transfers + 1);
        prev.set(ok, { key, transfer: true });
        deque.push({ station, line: otherLine, transfers: transfers + 1 }); // ← 비용 1: 뒤 삽입
      }
    }
  }
  return null;
}
```

**알고리즘 설계의 핵심:**

- **상태 공간**: `(역명, 탑승 노선)` 쌍을 노드로 정의합니다. 이렇게 해야 같은 역에 여러 노선이 통과하는 환승역에서 어느 노선을 타고 있는지를 구분할 수 있습니다.
- **Deque 기반 O(V+E) 탐색**: 비용 0 간선은 `unshift`(앞 삽입), 비용 1 간선은 `push`(뒤 삽입). Dijkstra의 O(E log V)보다 단순하고, 이 문제 구조상 불필요한 Priority Queue를 제거했습니다.
- **소요 시간 계산**: 경로 탐색 이후 각 노선 구간에 노선별 평균 역간 이동 시간(GTX-A: 300초 / 경의중앙선: 200초 / 1~9호선: 90~130초)과 환승 시간(평균 3분)을 적용하여 총 소요 시간을 추정합니다.

---

## ☸️ Kubernetes — 단순 배포를 넘어선 인프라 예외 상황 대응

Docker Desktop 내장 Kubernetes에 4개 서비스(subway-api, subway-notification, subway-collector, subway-web)와 MySQL, Kafka, Zookeeper를 배포하며 발생한 **실제 장애와 그 해결책**을 정리합니다.

### 트러블슈팅 & 설계 결정

| 문제 상황 | 원인 분석 | 적용한 해결책 |
|----------|----------|-------------|
| Kafka 파드 재시작 후 `Lock File` 오류로 무한 CrashLoopBackOff | Kafka는 PVC에 `.lock` 파일을 생성하는데, 비정상 종료 시 파일이 잔류 | `initContainer`에서 파드 시작 전 `.lock` 파일 삭제 + `chown` 수행 |
| Kafka 파드가 자기 자신의 Service를 찾지 못해 브로커 초기화 실패 | k8s가 `KAFKA_*` 환경변수를 자동 주입 → Confluent 브로커 설정값 덮어씌움 | `enableServiceLinks: false` 로 자동 환경변수 주입 차단 |
| Kafka PVC 교체(Recreate) 시 기존 파드가 떠 있어 신규 파드가 볼륨 마운트 실패 | PVC `accessMode: ReadWriteOnce` — 동시 마운트 불허 | Deployment `strategy.type: Recreate` 적용, 구 파드 완전 종료 후 신 파드 기동 |
| Kafka 파드가 PVC에 쓰기 실패 (`Permission Denied`) | Confluent 이미지의 프로세스 실행 유저가 `appuser (gid 1000)` | `securityContext.fsGroup: 1000` 설정으로 PVC 볼륨 그룹 소유권 부여 |
| Kafka 브로커가 자신의 Service Endpoint를 찾을 때 Not Ready 오류 | 파드가 준비되기 전에 자기 자신에 연결 시도 (chicken-and-egg) | Service에 `publishNotReadyAddresses: true` 설정 |
| Spring Boot 서비스가 k8s에서 JWT 검증 실패 | `JwtUtil`이 env를 base64 디코딩하는데, `secrets.yaml`은 k8s가 base64로 한 번 더 인코딩 | `secrets.yaml`에서 JWT 시크릿을 **double base64** 인코딩하여 디코딩 후 원본값 확보 |

### k8s 인프라 구성 다이어그램

```
Ingress (nginx)
  ├── /                    → subway-web     (Deployment, replicas: 2)
  ├── /api                 → subway-api     (Deployment, replicas: 2)
  │                                          HPA: CPU 70% 기준, 2~5 replicas
  └── /api/notifications   → subway-notification (Deployment, replicas: 1)

StatefulStorage
  ├── MySQL 8      PVC 10Gi  (ReadWriteOnce)
  └── Kafka        PVC 5Gi   (ReadWriteOnce, Recreate 전략)

ConfigMap / Secret
  ├── DB URL, Kafka Bootstrap, 공통 환경변수
  └── MySQL PW, JWT Secret, Seoul API Key (base64)
```

**원클릭 배포 스크립트** (`k8s-deploy.ps1`): Namespace → Secret/ConfigMap → MySQL(Ready 대기) → Zookeeper → Kafka(Ready 대기) → 애플리케이션 4종 → HPA/Ingress 순서를 자동으로 조율합니다. 의존성이 있는 컴포넌트를 `kubectl wait --for=condition=ready`로 동기화하여 타이밍 이슈를 원천 차단했습니다.

---

## 🚀 이원화 CI/CD 파이프라인

클라우드 운영 환경과 로컬 k8s 환경의 파이프라인을 별도로 구축했습니다.

### GitHub Actions — 클라우드 자동 배포

```
push to master
      │
      ├──[병렬]──────────────────────────────────┐
      │  test-subway-api    (Maven JUnit)          │
      │  test-subway-notification (Maven JUnit)    │
      │  lint-collector     (Python flake8)        │
      │  build-web          (SvelteKit npm build)  │
      └───────────────────────────────────────────┘
                     │ 4개 모두 통과
                     ▼
             deploy (순차 실행)
               ├─ Docker Build & Push → Docker Hub
               │  (metrohub-api, notification, collector, web)
               ├─ Render Webhook → subway-api 재배포
               └─ Render Webhook → subway-notification 재배포

Vercel: GitHub 연동으로 master push 시 프론트엔드 자동 배포 (별도 시크릿 불필요)
```

**keepalive.yml**: Render 무료 티어의 15분 슬립 정책을 GitHub Actions cron으로 10분마다 헬스체크 ping을 보내 우회합니다.

**collector.yml**: subway-collector Python 수집기를 GitHub Actions cron(5분 주기)으로 실행합니다. 별도 서버 없이 GitHub 인프라를 활용한 **서버리스 데이터 수집 파이프라인**입니다.

---

### Jenkins — 로컬 Kubernetes 자동 배포

```
GitHub Push
      │ Webhook (ngrok 터널로 로컬 Jenkins 외부 노출)
      ▼
Jenkins :8090
  ├──[병렬]──────────────────────────────────────┐
  │  subway-api      JUnit                        │
  │  subway-notification JUnit                    │
  │  subway-collector flake8                      │
  │  subway-web      npm build                    │
  └───────────────────────────────────────────────┘
                     │ 전체 통과
                     ▼
  Docker Build & Push ─► GHCR (ghcr.io/jung-ji-su)
    이미지 태그: :BUILD_NUMBER + :latest
    (빌드 번호별 누적 보관 → 버전별 롤백 가능)
                     │
                     ▼
  kubectl set image (버전 태그 지정)
  kubectl rollout status (배포 완료 대기)
```

**롤백 전략**: 이미지를 `:latest`와 `:BUILD_NUMBER` 두 가지 태그로 GHCR에 푸시합니다. 장애 발생 시 `kubectl rollout undo` 또는 이전 `BUILD_NUMBER` 태그로 `kubectl set image`를 실행하면 즉시 이전 버전으로 복구됩니다.

### 두 파이프라인 비교

| 항목 | GitHub Actions | Jenkins + k8s |
|------|---------------|---------------|
| **배포 대상** | Render (클라우드) + Vercel | 로컬 Kubernetes |
| **이미지 레지스트리** | Docker Hub | GHCR |
| **롤백 방법** | Render 대시보드 수동 / 새 push | `kubectl rollout undo` / 빌드 번호 태그 |
| **외부 접근** | GitHub 인프라 활용 | ngrok 터널 |
| **장점** | 설정 최소화, 클라우드 가용성 | k8s 롤링 배포, 세밀한 제어 |

---

## 🛠️ 기술 스택

| 분류 | 기술 | 선택 이유 |
|------|------|----------|
| **Frontend** | SvelteKit 5, Svelte Runes, Tailwind CSS v4 | 컴파일 타임 반응성, 번들 크기 최소화 |
| **Backend** | Java 17, Spring Boot 3.2, MyBatis | 타입 안전성, XML Mapper로 복잡 쿼리 분리 |
| **Messaging** | Apache Kafka (Confluent) | 수집·처리 디커플링, 파티션 키 기반 순서 보장 |
| **Database** | MySQL 8 | `ON DUPLICATE KEY UPDATE`로 시간대별 이동 평균 집계 |
| **Infra** | Docker Compose, Kubernetes, nginx Ingress | 개발-스테이징-운영 환경 일관성 |
| **CI/CD** | GitHub Actions, Jenkins, GHCR | 이원화 배포 환경 대응 |
| **Auth** | Spring Security + JWT | Stateless 인증, 서비스 간 토큰 재사용 |
| **SSE** | Spring SseEmitter | 폴링 없는 실시간 이벤트 푸시 |

---

## 📦 주요 구현 기능 목록

| 기능 | 상세 |
|------|------|
| **실시간 혼잡도** | 역별 혼잡도 카드, 즐겨찾기, 시간대별 바차트 |
| **실시간 노선도** | 24개 노선 열차 위치, 계통 필터, 30초 자동 갱신 |
| **경로 탐색** | 0-1 BFS 최소환승, 소요시간 추정, 노선도 자동 스크롤 |
| **역 상세** | 역별 실시간 도착 정보 |
| **혼잡 경보** | Kafka 스트림 처리, 5분 쿨다운, 자동 해제 |
| **개인화 알림** | 호선별 구독, SSE 실시간 수신, 알림 이력 페이지 |
| **커뮤니티** | 24개 호선 게시판, 검색 필터, 댓글, 좋아요 |
| **민원** | 등록·상태 추적, 관리자 처리 |
| **온보딩** | 첫 방문 3슬라이드, localStorage 완료 기록 |

---

## 📂 Repository

```
github.com/jung-ji-su/MetroHub
```

> 전체 소스코드, k8s 매니페스트, Jenkinsfile, GitHub Actions 워크플로우가 단일 모노레포로 관리됩니다.
