# MetroHub — Claude Code 프로젝트 컨텍스트

## 프로젝트 개요
서울 지하철 실시간 정보 플랫폼. 혼잡도·열차 위치 실시간 확인, 커뮤니티, 민원 접수 기능 제공.

## 아키텍처
```
subway-api          Spring Boot :8080  — 혼잡도, 노선도, 커뮤니티, 민원, 인증
subway-notification Spring Boot :8081  — SSE 알림, 구독, Kafka Consumer
subway-collector    Python             — 서울 공공 API → Kafka 수집
subway-web          SvelteKit 5        — 모바일 웹 프론트엔드 (max-width 430px)
```

## 기술 스택
- **프론트**: SvelteKit 5 (Svelte 5 runes: `$state`, `$derived`, `$effect`), Tailwind CSS v4
- **백엔드**: Spring Boot 3, MyBatis (XML mapper), Spring Security + JWT
- **DB**: MySQL 8 (Docker)
- **메시지**: Kafka (Docker)
- **배포**: Docker Compose

## 로컬 실행
```bash
# 1. 환경변수 파일 준비 (.gitignore에 포함됨 — 직접 생성 필요)
cp .env.example .env
# .env 에 실제 값 입력: SEOUL_API_KEY, MYSQL_ROOT_PASSWORD, MYSQL_PASSWORD, JWT_SECRET

# 2. 전체 서비스 실행
docker compose up -d

# 3. 프론트 개발 서버
cd subway-web && npm install && npm run dev
```

## 주요 파일 위치
| 파일 | 설명 |
|------|------|
| `subway-web/src/routes/+page.svelte` | 메인 홈 (혼잡도 + 실시간 노선도 탭) |
| `subway-web/src/lib/lineStations.js` | 전 노선 역 목록, LINE_META, LINE_BRANCHES |
| `subway-web/src/lib/sseStore.js` | 공개 SSE (혼잡도·경보·트렌딩) |
| `subway-web/src/lib/notificationStore.js` | 개인 알림 SSE (로그인 필요) |
| `subway-web/src/lib/api.js` | 모든 API 호출 함수 |
| `subway-web/src/lib/routeCalculator.js` | 0-1 BFS 최적 환승 경로 계산 |

## 코딩 규칙
- Svelte 5 runes 사용 (`$state`, `$derived.by()`, `$effect`)
- 헤더 배경: `background: #ffffff; border-bottom: 1px solid #f3f4f6;` (전 페이지 통일)
- 카드: `bg-white rounded-2xl shadow-sm`
- 노선 색상은 `LINE_META[code].color` 참조

## 현재 구현된 기능
- [x] 실시간 혼잡도 (즐겨찾기, 검색, 시간대별 차트)
- [x] 실시간 노선도 (열차 위치, 계통 필터, 30초 자동 새로고침)
- [x] 내 경로 (최적 환승 계산, 노선도 연동 + 출발역 자동 스크롤)
- [x] 커뮤니티 (24개 호선 게시판, 글쓰기, 댓글/삭제, 좋아요)
- [x] 내 게시글 목록 (`/community/my`)
- [x] 민원 접수 + 내 민원 목록 (상태 추적)
- [x] 로그인/회원가입 (JWT)
- [x] MY 페이지 (프로필, 노선 알림 구독)
- [x] SSE 실시간 알림 (지수 백오프 재연결)
- [x] 이슈 트렌딩 위젯

## DB 스키마 핵심
```sql
users, congestion_data, community_posts, community_comments,
post_likes, complaints, notifications, subscriptions
```
MySQL은 Docker volume `mysql-data`에 저장됨. `docker compose down -v` 하면 데이터 삭제되니 주의.
