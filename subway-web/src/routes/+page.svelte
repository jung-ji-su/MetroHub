<script>
  import { api } from '$lib/api';
  import { favorites, routes, token, user } from '$lib/stores';
  import { LINE_META, LINE_STATIONS, LINE_BRANCHES, getBranchStations, SUPPORTED_LINES } from '$lib/lineStations';
  import { trendingStations, lineAlerts, latestCongestionLine, dismissAlert, sseConnected, sseRetryExhausted, lastSseUpdate, retrySSE } from '$lib/sseStore';
  import { notifications, unreadCount, markAllRead, notifRetryExhausted, retryNotificationSSE } from '$lib/notificationStore';
  import { findRoute, searchStations, calcRouteMins } from '$lib/routeCalculator';
  import StationSearch from '$lib/StationSearch.svelte';

  let showNotifPanel = $state(false);
  function toggleNotifPanel() {
    showNotifPanel = !showNotifPanel;
    if (showNotifPanel) markAllRead();
  }
  function formatNotifTime(iso) {
    if (!iso) return '';
    const d = new Date(iso);
    return d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
  }

  // ── 시간대별 혼잡도 차트 ────────────────────────────────────────────
  let hourlyData   = $state({});  // { [stationName]: { loading, data: [{hourOfDay, avgCongestion}] } }
  let showHourly   = $state(new Set());

  async function toggleHourly(stationName) {
    const next = new Set(showHourly);
    if (next.has(stationName)) {
      next.delete(stationName);
      showHourly = next;
      return;
    }
    next.add(stationName);
    showHourly = next;
    if (hourlyData[stationName]) return;
    hourlyData = { ...hourlyData, [stationName]: { loading: true, data: [] } };
    try {
      const data = await api.congestionHourly(stationName);
      hourlyData = { ...hourlyData, [stationName]: { loading: false, data } };
    } catch (_) {
      hourlyData = { ...hourlyData, [stationName]: { loading: false, data: [] } };
    }
  }

  // 24시간 배열로 채우기 (데이터 없는 시간대는 0)
  function buildHourlyBars(data) {
    const map = {};
    for (const d of data) map[d.hourOfDay] = d.avgCongestion;
    return Array.from({ length: 24 }, (_, h) => ({ hour: h, avg: map[h] ?? 0 }));
  }

  function hourlyBarColor(avg) {
    if (avg === 0)    return '#e5e7eb';
    if (avg <= 30)    return '#86efac';
    if (avg <= 60)    return '#fde047';
    if (avg <= 80)    return '#fb923c';
    return '#f87171';
  }

  function hourLabel(h) {
    if (h === 0)  return '자정';
    if (h === 12) return '정오';
    if (h % 6 === 0) return `${h}시`;
    return '';
  }

  // ── 공통 ────────────────────────────────────────────────────────────
  let activeTab = $state('congestion'); // 'congestion' | 'linemap'

  // ── 내 경로 관리 ────────────────────────────────────────────────────
  let showAddRoute  = $state(false);
  let routeFrom     = $state('');
  let routeTo       = $state('');

  function addRoute() {
    if (!routeFrom.trim() || !routeTo.trim()) return;
    routes.add(routeFrom.trim(), routeTo.trim());
    routeFrom = ''; routeTo = '';
    showAddRoute = false;
  }

  // 경로 세그먼트 캐시
  const routeSegments = $derived.by(() => {
    const result = {};
    for (const r of $routes) {
      result[r.id] = findRoute(r.from, r.to);
    }
    return result;
  });

  // 경로별 실시간 도착 정보 캐시
  let routeArrivals = $state({});

  let focusStation = $state(null);

  function goToLineMap(lineCode, stationName) {
    selectedLine = lineCode;
    activeTab = 'linemap';
    focusStation = stationName ?? null;
  }

  // 노선 로딩 완료 후 focusStation으로 스크롤
  $effect(() => {
    if (lineLoading || !focusStation || activeTab !== 'linemap') return;
    const target = focusStation;
    focusStation = null;
    requestAnimationFrame(() => {
      const el = document.getElementById(`station-${target}`);
      if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    });
  });

  async function fetchRouteArrival(routeId, from, lineCode) {
    routeArrivals = { ...routeArrivals, [routeId]: { loading: true, data: null } };
    try {
      const arrivals = await api.congestionByStation(from);
      // 해당 노선의 첫 도착 열차 필터
      const match = arrivals.find(a => a.lineNumber === lineCode);
      routeArrivals = { ...routeArrivals, [routeId]: { loading: false, data: match ?? null } };
    } catch (_) {
      routeArrivals = { ...routeArrivals, [routeId]: { loading: false, data: null } };
    }
  }

  // ── 혼잡도 탭 ───────────────────────────────────────────────────────
  const CONGESTION = {
    null:  { label: '정보없음',    bg: '#F3F4F6', text: '#9CA3AF' },
    low:   { label: '🥶 여유',    bg: '#DCFCE7', text: '#16A34A' },
    mid:   { label: '🤔 보통',    bg: '#FEF9C3', text: '#CA8A04' },
    high:  { label: '🥵 혼잡',    bg: '#FFEDD5', text: '#EA580C' },
    full:  { label: '🚨 매우혼잡', bg: '#FEE2E2', text: '#DC2626' },
  };

  function congestionKey(level) {
    if (level == null) return 'null';
    if (level <= 30)   return 'low';
    if (level <= 60)   return 'mid';
    if (level <= 80)   return 'high';
    return 'full';
  }

  let resultsMap  = $state({});
  let searchInput = $state('');
  let showSearch  = $state(false);

  async function fetchStation(name) {
    resultsMap = { ...resultsMap, [name]: { loading: true, error: '', data: [] } };
    try {
      const data = await api.congestionByStation(name.trim());
      resultsMap = { ...resultsMap, [name]: { loading: false, error: '', data } };
    } catch (e) {
      resultsMap = { ...resultsMap, [name]: { loading: false, error: e.message, data: [] } };
    }
  }

  // 즐겨찾기 초기 로드
  $effect(() => {
    for (const s of $favorites) {
      if (!resultsMap[s]) fetchStation(s);
    }
  });

  // 역이 속한 호선 코드 목록 반환
  function getLinesForStation(stationName) {
    return Object.entries(LINE_STATIONS)
      .filter(([, stations]) => stations.includes(stationName))
      .map(([lineCode]) => lineCode);
  }

  async function addFavorite(name) {
    if (!name?.trim()) return;
    favorites.add(name.trim());
    fetchStation(name.trim());
    showSearch = false;
    searchInput = '';
    // 로그인 상태이면 해당 역 호선 자동 구독
    if ($token) {
      try {
        const lineCodes = getLinesForStation(name.trim());
        const existing = await api.getSubscriptions($token);
        const existingValues = new Set(existing.map(s => s.subValue));
        for (const lineCode of lineCodes) {
          if (!existingValues.has(lineCode)) {
            await api.addSubscription({ subType: 'LINE', subValue: lineCode }, $token);
          }
        }
      } catch (_) {}
    }
  }

  function removeFavorite(name) {
    favorites.remove(name);
    const next = { ...resultsMap };
    delete next[name];
    resultsMap = next;
  }

  async function doSearch(name) {
    const n = (name ?? searchInput).trim();
    if (!n) return;
    showSearch = false;
    searchInput = '';
    await fetchStation(n);
  }

  function formatTime(dt) {
    if (!dt) return '';
    return new Date(dt + 'Z').toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
  }

  // ── 노선도 탭 ────────────────────────────────────────────────────────
  let selectedLine  = $state('1002'); // 기본: 2호선
  let trainData     = $state([]);
  let lineLoading   = $state(false);
  let lineError     = $state('');
  let lastUpdated   = $state(null);
  let filterDir     = $state('전체'); // '전체' | '상행' | '하행' | '외선순환' | '내선순환'
  let filterBranch  = $state('전체'); // '전체' | branch.id

  const lineColor   = $derived(LINE_META[selectedLine]?.color ?? '#6B7280');
  const lineName    = $derived(LINE_META[selectedLine]?.name ?? selectedLine);
  const dirLabels   = $derived(LINE_META[selectedLine]?.dirLabel ?? ['상행', '하행']);
  const currentBranches = $derived(LINE_BRANCHES[selectedLine]?.branches ?? []);

  // 계통 필터 적용 역 목록 (지도 표시 + 열차 위치 기준)
  const stations = $derived.by(() => {
    if (filterBranch === '전체' || !LINE_BRANCHES[selectedLine]) {
      return LINE_STATIONS[selectedLine] ?? [];
    }
    return getBranchStations(selectedLine, filterBranch);
  });

  // 노선도 레이아웃 상수
  const SEGMENT_PX  = 76;  // 역과 역 사이 픽셀 간격
  const AVG_INTER_S = 90;  // 역 간 평균 이동 시간(초)

  // 계통 필터 적용
  const branchTrains = $derived.by(() => {
    if (filterBranch === '전체' || !LINE_BRANCHES[selectedLine]) return trainData;
    const branch = LINE_BRANCHES[selectedLine].branches.find(b => b.id === filterBranch);
    if (!branch) return trainData;
    return trainData.filter(t => {
      const dest = t.destination ?? '';
      const matchesDest = branch.destKeywords.some(kw => dest.includes(kw));
      // 목적지 정보가 없으면 현재 역이 해당 계통 구간에 있는지 확인
      const branchStations = getBranchStations(selectedLine, filterBranch);
      const trunkStations = LINE_STATIONS[selectedLine] ?? [];
      const trunkEndIdx = trunkStations.indexOf(branch.trunkEnd ?? '');
      const inBranchSection = trunkEndIdx !== -1 &&
        trunkStations.indexOf(t.currentStation) > trunkEndIdx &&
        branchStations.includes(t.currentStation);
      return matchesDest || inBranchSection;
    });
  });

  // 방향 + 계통 필터 적용된 열차 목록
  const filteredTrains = $derived(
    filterDir === '전체'
      ? branchTrains
      : branchTrains.filter(t => t.direction === filterDir)
  );

  // 위치 기준 정렬 (상세 패널 prev/next용)
  const sortedTrains = $derived.by(() =>
    [...filteredTrains].sort((a, b) =>
      stations.indexOf(a.currentStation) - stations.indexOf(b.currentStation)
    )
  );

  let selectedTrain = $state(null);

  const selectedTrainIdx = $derived(
    selectedTrain ? sortedTrains.findIndex(t => t.trainNo === selectedTrain.trainNo) : -1
  );

  // 상행(첫 번째 dirLabel) 여부
  function isUpward(train) {
    return train.direction === (LINE_META[selectedLine]?.dirLabel[0] ?? '상행');
  }

  // arvlMsg3(currentStation)은 열차가 "지금 향하는 다음 도착역"이다.
  // 열차는 prevStation(출발한 역)과 currentStation(곧 도착할 역) 사이에 있다.
  function getProgressInfo(train) {
    const tIdx = stations.indexOf(train.currentStation);
    if (tIdx < 0) return null;
    const up = isUpward(train);
    // 상행: 아래쪽(높은 idx)에서 출발 / 하행: 위쪽(낮은 idx)에서 출발
    const prevIdx = up ? tIdx + 1 : tIdx - 1;
    if (prevIdx < 0 || prevIdx >= stations.length) return null;
    return {
      currentStation: stations[prevIdx],   // 방금 출발한 역
      nextStation:    train.currentStation, // 곧 도착할 역
      progress: 1 - Math.min(1, Math.max(0, train.etaSeconds / AVG_INTER_S)),
    };
  }

  // 열차 Y 좌표(px): prevStation ~ currentStation 사이 보간
  // etaSeconds=MAX → prevStation 위치, etaSeconds=0 → currentStation 위치
  function trainTopPx(train) {
    const idx = stations.indexOf(train.currentStation);
    if (idx < 0) return null;
    const ratio = Math.min(1, Math.max(0, train.etaSeconds / AVG_INTER_S));
    return isUpward(train)
      ? (idx + ratio) * SEGMENT_PX   // 상행: 아래(높은 idx)에서 위로 접근
      : (idx - ratio) * SEGMENT_PX;  // 하행: 위(낮은 idx)에서 아래로 접근
  }

  // CSS animation 이동 거리 — 상행 음수(위), 하행 양수(아래)
  function trainMoveDist(train) {
    const eta = Math.min(AVG_INTER_S, Math.max(0, train.etaSeconds));
    const dist = (eta / AVG_INTER_S) * SEGMENT_PX;
    return isUpward(train) ? -dist : dist;
  }

  // 열차 X 좌표(px) — 상행: 트랙 오른쪽, 하행: 트랙 왼쪽
  function trainLeftPx(train) {
    return isUpward(train) ? 28 : -2;
  }

  function selectPrev() {
    const idx = sortedTrains.findIndex(t => t.trainNo === selectedTrain?.trainNo);
    if (idx > 0) selectedTrain = sortedTrains[idx - 1];
  }
  function selectNext() {
    const idx = sortedTrains.findIndex(t => t.trainNo === selectedTrain?.trainNo);
    if (idx < sortedTrains.length - 1) selectedTrain = sortedTrains[idx + 1];
  }

  // API 한도 초과 시 UI 확인용 mock 데이터
  function makeMockTrains(lineCode, stationList) {
    const dirs = LINE_META[lineCode]?.dirLabel ?? ['상행', '하행'];
    const picks = [
      Math.floor(stationList.length * 0.1),
      Math.floor(stationList.length * 0.25),
      Math.floor(stationList.length * 0.45),
      Math.floor(stationList.length * 0.65),
      Math.floor(stationList.length * 0.82),
    ];
    return picks.map((idx, i) => ({
      trainNo:        `DEMO${i + 1}`,
      lineCode,
      currentStation: stationList[idx],
      direction:      dirs[i % 2],
      destination:    i % 2 === 0 ? `${stationList.at(-1)}행` : `${stationList[0]}행`,
      arrivalMessage: '시연용 데이터',
      etaSeconds:     (i + 1) * 45,
    }));
  }

  let useMock = $state(false);

  async function fetchLineTrains() {
    lineLoading = true; lineError = ''; useMock = false;
    try {
      const result = await api.lineTrains(selectedLine);
      if (result.length === 0) {
        // API 한도 초과 또는 새벽 운행 없음 → mock으로 UI 확인
        trainData = makeMockTrains(selectedLine, LINE_STATIONS[selectedLine] ?? []);
        useMock   = true;
      } else {
        trainData = result;
      }
      lastUpdated = new Date();
      filterDir   = '전체';
    } catch (e) {
      lineError = e.message;
    } finally {
      lineLoading = false;
    }
  }

  // 탭 전환 또는 노선 변경 시 재조회 + 필터 초기화
  $effect(() => {
    const _line = selectedLine;
    filterBranch = '전체';
    if (activeTab !== 'linemap') return;
    trainData = [];
    fetchLineTrains();
  });

  const LINE_REFRESH_MS = Number(import.meta.env.VITE_LINE_REFRESH_MS) || 30_000;

  // 노선도 탭 자동 새로고침 (기본 30초, VITE_LINE_REFRESH_MS로 조정 가능)
  async function fetchLineTrainsQuiet() {
    try {
      const result = await api.lineTrains(selectedLine);
      if (result.length > 0) {
        trainData = result;
        lastUpdated = new Date();
        useMock = false;
      }
    } catch (_) {}
  }

  $effect(() => {
    if (activeTab !== 'linemap') return;
    const id = setInterval(fetchLineTrainsQuiet, LINE_REFRESH_MS);
    return () => clearInterval(id);
  });

  // SSE 혼잡도 업데이트 수신 → 해당 노선의 즐겨찾기 역 자동 갱신
  const STATION_LINE_MAP = (() => {
    const map = {};
    for (const [code, stationList] of Object.entries(LINE_STATIONS)) {
      for (const s of stationList) {
        if (!map[s]) map[s] = [];
        map[s].push(code);
      }
    }
    return map;
  })();

  let _congestionDebounce = null;
  $effect(() => {
    const updatedLine = $latestCongestionLine;
    if (!updatedLine) return;
    // SSE 이벤트가 연속으로 와도 2초 내 한 번만 API 호출
    clearTimeout(_congestionDebounce);
    _congestionDebounce = setTimeout(() => {
      for (const fav of $favorites) {
        if ((STATION_LINE_MAP[fav] ?? []).includes(updatedLine)) {
          fetchStation(fav);
        }
      }
    }, 2000);
  });

  function formatUpdated(d) {
    if (!d) return '';
    return d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }

  function etaLabel(sec) {
    if (!sec || sec <= 0) return '곧 도착';
    if (sec < 60) return `${sec}초`;
    return `${Math.round(sec / 60)}분`;
  }
</script>

<!-- ── 헤더 ─────────────────────────────────────────────────────────── -->
<header class="px-5 pt-12 pb-0 sticky top-0 z-40" style="background: #ffffff; border-bottom: 1px solid #f3f4f6;">
  <div class="flex items-center justify-between pb-3">
    <div>
      <div class="flex items-center gap-1.5 mb-0.5">
        <p class="text-xs text-gray-400 font-medium tracking-wide">METROHUB</p>
        <div class="flex items-center gap-1">
          {#if $sseRetryExhausted}
            <button
              onclick={retrySSE}
              class="flex items-center gap-1 text-[10px] font-semibold text-orange-500 bg-orange-50 px-2 py-0.5 rounded-full active:bg-orange-100 transition-colors"
            >
              <svg class="w-2.5 h-2.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
              </svg>
              재연결
            </button>
          {:else}
            <div class="w-1.5 h-1.5 rounded-full {$sseConnected ? 'bg-green-400 animate-pulse' : 'bg-gray-300'}"
                 title="{$sseConnected ? 'LIVE' : '연결 중...'}"></div>
            <span class="text-[10px] font-semibold {$sseConnected ? 'text-green-500' : 'text-gray-400'}">
              {$sseConnected ? 'LIVE' : '연결 중'}
            </span>
          {/if}
        </div>
      </div>
      <h1 class="text-xl font-bold text-gray-900 leading-tight">
        {activeTab === 'congestion' ? '실시간 혼잡도' : '실시간 노선도'}
      </h1>
    </div>
    <div class="flex items-center gap-2">
      {#if $user}
        <button
          onclick={toggleNotifPanel}
          class="w-10 h-10 flex items-center justify-center rounded-full bg-white/70 active:bg-white transition-colors relative"
        >
          <svg class="w-5 h-5 text-gray-700" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round"
              d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0" />
          </svg>
          {#if $unreadCount > 0}
            <span class="absolute top-1.5 right-1.5 w-[14px] h-[14px] bg-red-500 text-white text-[9px] font-bold rounded-full flex items-center justify-center leading-none">
              {$unreadCount > 9 ? '9+' : $unreadCount}
            </span>
          {/if}
        </button>
      {/if}
      {#if activeTab === 'congestion'}
        <button
          onclick={() => { showSearch = !showSearch; }}
          class="w-10 h-10 flex items-center justify-center rounded-full bg-white/70 active:bg-white transition-colors"
        >
          <svg class="w-5 h-5 text-gray-700" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 15.803a7.5 7.5 0 0010.607 0z" />
          </svg>
        </button>
      {:else}
        <button
          onclick={fetchLineTrains}
          disabled={lineLoading}
          class="w-10 h-10 flex items-center justify-center rounded-full bg-white/70 active:bg-white transition-colors disabled:opacity-40"
        >
          <svg class="w-5 h-5 text-gray-700 {lineLoading ? 'animate-spin' : ''}" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
          </svg>
        </button>
      {/if}
    </div>
  </div>

  <!-- 알림 패널 -->
  {#if showNotifPanel}
    <button onclick={() => showNotifPanel = false} class="fixed inset-0 z-40 bg-black/20" aria-label="닫기"></button>
    <div class="fixed top-[86px] left-1/2 -translate-x-1/2 w-full max-w-[430px] z-50 px-3">
      <div class="bg-white rounded-2xl shadow-xl overflow-hidden" style="max-height: 65vh;">
        <div class="flex items-center justify-between px-4 py-3 border-b border-gray-100">
          <span class="text-[15px] font-bold text-gray-900">알림</span>
          <button onclick={() => showNotifPanel = false} class="w-7 h-7 flex items-center justify-center rounded-full bg-gray-100 active:bg-gray-200">
            <svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div class="overflow-y-auto" style="max-height: calc(65vh - 48px);">
          {#if $notifications.length === 0}
            <div class="flex flex-col items-center justify-center py-12 text-center">
              <svg class="w-10 h-10 text-gray-200 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.5">
                <path stroke-linecap="round" stroke-linejoin="round"
                  d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0" />
              </svg>
              <p class="text-sm text-gray-400">새로운 알림이 없어요</p>
            </div>
          {:else}
            {#each $notifications as notif}
              <div class="px-4 py-3 border-b border-gray-50 last:border-0">
                <div class="flex items-start gap-3">
                  <div class="w-8 h-8 bg-blue-50 rounded-full flex items-center justify-center flex-shrink-0 mt-0.5">
                    <svg class="w-4 h-4 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
                      <path stroke-linecap="round" stroke-linejoin="round"
                        d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0" />
                    </svg>
                  </div>
                  <div class="flex-1 min-w-0">
                    <p class="text-[13px] font-semibold text-gray-900">{notif.title ?? ''}</p>
                    <p class="text-xs text-gray-500 mt-0.5 leading-relaxed">{notif.body ?? ''}</p>
                    {#if notif.createdAt}
                      <p class="text-[10px] text-gray-300 mt-1">{formatNotifTime(notif.createdAt)}</p>
                    {/if}
                  </div>
                </div>
              </div>
            {/each}
          {/if}
        </div>
      </div>
    </div>
  {/if}

  <!-- 검색바 (혼잡도 탭) -->
  {#if showSearch && activeTab === 'congestion'}
    <div class="pb-3">
      <StationSearch
        bind:value={searchInput}
        placeholder="역 이름 검색 후 선택해서 추가"
        onselect={(name) => addFavorite(name)}
        class="w-full"
      />
    </div>
  {/if}

  <!-- 탭 토글 -->
  <div class="flex" style="border-top: 1px solid #f3f4f6;">
    {#each [['congestion','역 혼잡도'],['linemap','실시간 노선도']] as [id, label]}
      <button
        onclick={() => activeTab = id}
        class="flex-1 py-3 text-[13px] font-bold transition-colors relative"
        style="color: {activeTab === id ? lineColor : '#9CA3AF'};"
      >
        {label}
        {#if activeTab === id}
          <div class="absolute bottom-0 left-0 right-0 h-0.5 rounded-full" style="background-color: {lineColor};"></div>
        {/if}
      </button>
    {/each}
  </div>
</header>

<!-- 실시간 연결 끊김 배너 -->
{#if $sseRetryExhausted}
  <div class="mx-4 mt-3 bg-orange-50 rounded-2xl px-4 py-2.5 flex items-center justify-between gap-3">
    <div class="flex items-center gap-2">
      <svg class="w-4 h-4 text-orange-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
      </svg>
      <span class="text-xs font-semibold text-orange-600">실시간 데이터가 오래됐을 수 있어요</span>
    </div>
    <button
      onclick={retrySSE}
      class="text-xs font-bold text-orange-600 bg-orange-100 px-3 py-1 rounded-full active:bg-orange-200 flex-shrink-0"
    >재연결</button>
  </div>
{/if}

<!-- ══════════════════════════════════════════════════════════════════ -->
<!-- 혼잡도 탭                                                          -->
<!-- ══════════════════════════════════════════════════════════════════ -->
{#if activeTab === 'congestion'}
<div class="px-4 pt-5 space-y-6 pb-4">

  <!-- 지금 이슈 트렌딩 위젯 -->
  {#if $trendingStations.length > 0}
    <section>
      <div class="flex items-center gap-2 mb-3">
        <div class="w-2 h-2 bg-red-500 rounded-full animate-pulse"></div>
        <h2 class="text-[15px] font-bold text-gray-900">지금 이슈</h2>
        <span class="text-xs text-gray-400">실시간 TOP {$trendingStations.length}</span>
      </div>
      <div class="flex gap-2 overflow-x-auto no-scrollbar pb-1">
        {#each $trendingStations as station, i}
          {@const score = station.score}
          {@const chipColor = score > 80 ? '#FEE2E2' : score > 50 ? '#FFEDD5' : '#F3F4F6'}
          {@const textColor = score > 80 ? '#DC2626' : score > 50 ? '#EA580C' : '#6B7280'}
          <button
            onclick={() => fetchStation(station.stationName)}
            class="flex-shrink-0 flex items-center gap-2 rounded-2xl px-3 py-2.5 active:opacity-70 transition-opacity"
            style="background: {chipColor};"
          >
            <span class="text-xs font-bold" style="color: {textColor};">#{i + 1}</span>
            <span class="text-sm font-semibold text-gray-800">{station.stationName}</span>
          </button>
        {/each}
      </div>
    </section>
  {/if}

  <!-- 내 경로 섹션 -->
  <section>
    <div class="flex items-center justify-between mb-3">
      <h2 class="text-[15px] font-bold text-gray-900">🚇 내 경로</h2>
      <button
        onclick={() => showAddRoute = !showAddRoute}
        class="text-xs font-bold px-3 py-1.5 rounded-full transition-colors active:opacity-70
               {showAddRoute ? 'bg-gray-100 text-gray-600' : 'bg-blue-50 text-blue-600'}"
      >{showAddRoute ? '취소' : '+ 경로 추가'}</button>
    </div>

    {#if showAddRoute}
      <div class="bg-white rounded-2xl shadow-sm p-4 mb-3 space-y-3">
        <div>
          <p class="text-xs font-bold text-gray-500 mb-2">🚉 출발역</p>
          <StationSearch bind:value={routeFrom} placeholder="출발역 입력 (예: 당정)" />
        </div>
        <div>
          <p class="text-xs font-bold text-gray-500 mb-2">🏁 도착역</p>
          <StationSearch bind:value={routeTo} placeholder="도착역 입력 (예: 국회의사당)" />
        </div>
        <button
          onclick={addRoute}
          disabled={!routeFrom.trim() || !routeTo.trim()}
          class="w-full bg-blue-600 text-white text-sm font-bold py-3.5 rounded-2xl disabled:opacity-40 active:bg-blue-700 transition-colors"
        >경로 추가</button>
      </div>
    {/if}

    {#if $routes.length === 0 && !showAddRoute}
      <div class="bg-white rounded-2xl p-5 text-center shadow-sm">
        <p class="text-3xl mb-2">🚇</p>
        <p class="text-sm font-semibold text-gray-700">자주 이용하는 구간을 등록하세요</p>
        <p class="text-xs text-gray-400 mt-1">출발역과 도착역을 등록하면<br>최적 환승 경로를 안내해드려요</p>
      </div>
    {:else}
      <div class="space-y-3">
        {#each $routes as route}
          {@const segs = routeSegments[route.id]}
          <div class="bg-white rounded-2xl shadow-sm overflow-hidden">
            <!-- 경로 헤더 -->
            <div class="flex items-center justify-between px-4 pt-4 pb-2">
              <div class="flex items-center gap-2 min-w-0">
                <span class="text-[15px] font-bold text-gray-900 truncate">{route.from}</span>
                <svg class="w-4 h-4 text-blue-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 4.5L21 12m0 0l-7.5 7.5M21 12H3" />
                </svg>
                <span class="text-[15px] font-bold text-gray-900 truncate">{route.to}</span>
              </div>
              <button
                onclick={() => routes.remove(route.id)}
                class="w-7 h-7 flex items-center justify-center rounded-full text-gray-300 active:text-red-400 active:bg-red-50 flex-shrink-0"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <!-- 환승 경로 시각화 -->
            {#if segs}
              <div class="px-4 pb-4">
                {#each segs as seg, si}
                  <div class="flex items-stretch gap-3">
                    <!-- 세로 라인 -->
                    <div class="flex flex-col items-center flex-shrink-0 w-4">
                      <div class="w-3 h-3 rounded-full border-2 bg-white mt-0.5" style="border-color: {seg.lineColor};"></div>
                      {#if si < segs.length - 1}
                        <div class="w-0.5 flex-1 mt-0.5" style="background-color: {seg.lineColor};"></div>
                      {/if}
                    </div>
                    <!-- 역/환승 정보 -->
                    <div class="pb-3 min-w-0 flex-1">
                      <div class="flex items-center gap-1.5">
                        <span class="text-[11px] font-bold px-1.5 py-0.5 rounded-full text-white" style="background: {seg.lineColor};">
                          {seg.lineName}
                        </span>
                        <span class="text-[11px] text-gray-400">{seg.stations.length - 1}개역</span>
                        {#if seg.stations.length > 1}
                          <span class="text-[11px] text-gray-300">·</span>
                          <span class="text-[11px] text-gray-400">~{Math.round(seg.durationSecs / 60)}분</span>
                        {/if}
                        <button
                          onclick={() => goToLineMap(seg.line, seg.stations[0])}
                          class="ml-auto text-[10px] font-bold px-2 py-0.5 rounded-full active:opacity-60 transition-opacity flex-shrink-0"
                          style="background-color: {seg.lineColor}20; color: {seg.lineColor};"
                        >지도 →</button>
                      </div>
                      <p class="text-[13px] font-semibold text-gray-800 mt-0.5">
                        {seg.stations[0]}{#if seg.stations.length > 2}<span class="text-gray-300"> ··· </span>{/if}{seg.stations.at(-1)}
                      </p>
                      {#if si < segs.length - 1}
                        <span class="inline-flex items-center gap-1 mt-1 text-[10px] font-bold text-orange-500 bg-orange-50 px-2 py-0.5 rounded-full">
                          🔄 환승 → {segs[si + 1].lineName}
                        </span>
                      {/if}
                    </div>
                  </div>
                {/each}
                <!-- 도착 -->
                <div class="flex items-center gap-3 mt-0">
                  <div class="w-4 flex justify-center flex-shrink-0">
                    <div class="w-3 h-3 rounded-full bg-gray-700"></div>
                  </div>
                  <p class="text-[13px] font-bold text-gray-700">🏁 {route.to} 도착</p>
                </div>
                <div class="flex items-center justify-between mt-2 ml-7">
                  <div>
                    <p class="text-[12px] font-bold text-gray-700">약 {calcRouteMins(segs)}분 <span class="text-gray-400 font-normal">도착 예상 {(() => { const d = new Date(Date.now() + calcRouteMins(segs) * 60000); return `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`; })()}</span></p>
                    <p class="text-[11px] text-gray-400 mt-0.5">
                      총 {segs.length - 1}회 환승 · {segs.reduce((s, g) => s + g.stations.length - 1, 0)}개역
                    </p>
                  </div>
                  <button
                    onclick={() => fetchRouteArrival(route.id, route.from, segs[0]?.line)}
                    class="text-[10px] font-bold text-blue-500 bg-blue-50 px-2 py-1 rounded-full active:bg-blue-100 transition-colors"
                  >
                    🔴 실시간
                  </button>
                </div>
                {#if routeArrivals[route.id]}
                  {@const ra = routeArrivals[route.id]}
                  <div class="mt-2 ml-7 bg-blue-50 rounded-xl px-3 py-2">
                    {#if ra.loading}
                      <p class="text-[11px] text-blue-400 animate-pulse">도착 정보 조회 중...</p>
                    {:else if ra.data}
                      <p class="text-[11px] font-bold text-blue-700">
                        출발역 ({route.from}) 다음 열차: {ra.data.arrivalMessage ?? '정보없음'}
                      </p>
                    {:else}
                      <p class="text-[11px] text-gray-400">현재 도착 정보가 없습니다</p>
                    {/if}
                  </div>
                {/if}
              </div>
            {:else if segs === null}
              <p class="px-4 pb-4 text-xs text-red-400">⚠️ 경로를 찾을 수 없어요 (역 이름을 확인해주세요)</p>
            {/if}
          </div>
        {/each}
      </div>
    {/if}
  </section>

  <!-- 조회 결과 -->
  {#if Object.keys(resultsMap).length > 0}
    <section class="space-y-4">
      {#each Object.entries(resultsMap) as [station, result]}
        <div>
          <div class="flex items-center justify-between mb-2 px-1">
            <span class="text-[15px] font-bold text-gray-900">🚉 {station}</span>
            <div class="flex items-center gap-2">
              <button onclick={() => fetchStation(station)} class="active:opacity-60" title="새로고침">
                <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
                </svg>
              </button>
              {#if $favorites.includes(station)}
                <button onclick={() => removeFavorite(station)} class="active:opacity-60" title="즐겨찾기 삭제">
                  <svg class="w-4 h-4 text-yellow-400" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M11.48 3.499a.562.562 0 011.04 0l2.125 5.111a.563.563 0 00.475.345l5.518.442c.499.04.701.663.321.988l-4.204 3.602a.563.563 0 00-.182.557l1.285 5.385a.562.562 0 01-.84.61l-4.725-2.885a.563.563 0 00-.586 0L6.982 20.54a.562.562 0 01-.84-.61l1.285-5.386a.562.562 0 00-.182-.557l-4.204-3.602a.563.563 0 01.321-.988l5.518-.442a.563.563 0 00.475-.345L11.48 3.5z" />
                  </svg>
                </button>
              {:else}
                <button onclick={() => addFavorite(station)} class="active:opacity-60" title="즐겨찾기 추가">
                  <svg class="w-4 h-4 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M11.48 3.499a.562.562 0 011.04 0l2.125 5.111a.563.563 0 00.475.345l5.518.442c.499.04.701.663.321.988l-4.204 3.602a.563.563 0 00-.182.557l1.285 5.385a.562.562 0 01-.84.61l-4.725-2.885a.563.563 0 00-.586 0L6.982 20.54a.562.562 0 01-.84-.61l1.285-5.386a.562.562 0 00-.182-.557l-4.204-3.602a.563.563 0 01.321-.988l5.518-.442a.563.563 0 00.475-.345L11.48 3.5z" />
                  </svg>
                </button>
              {/if}
            </div>
          </div>

          {#if result.loading}
            <div class="space-y-2">
              {#each [1,2] as _}
                <div class="bg-white rounded-2xl p-4 shadow-sm">
                  <div class="h-4 bg-gray-100 rounded-lg w-1/3 mb-3 animate-pulse"></div>
                  <div class="h-6 bg-gray-100 rounded-lg w-2/3 mb-2 animate-pulse"></div>
                  <div class="h-3 bg-gray-100 rounded-lg w-1/4 animate-pulse"></div>
                </div>
              {/each}
            </div>
          {:else if result.error}
            <div class="bg-red-50 rounded-2xl px-4 py-3 text-sm text-red-500">{result.error}</div>
          {:else if result.data.length === 0}
            <div class="bg-white rounded-2xl px-4 py-5 text-center shadow-sm">
              <p class="text-sm text-gray-400">현재 운행 정보가 없습니다</p>
              <p class="text-xs text-gray-300 mt-1">자정~오전 5시에는 운행이 없습니다</p>
            </div>
          {:else}
            <div class="space-y-2">
              {#each result.data as item}
                {@const ck = congestionKey(item.congestionLevel)}
                {@const cInfo = CONGESTION[ck]}
                {@const lc = LINE_META[item.lineNumber]?.color ?? '#6B7280'}
                <div class="bg-white rounded-2xl shadow-sm overflow-hidden" style="border-left: 4px solid {lc};">
                  <div class="p-4">
                    <div class="flex items-center justify-between mb-2">
                      <span class="text-xs font-bold px-2 py-0.5 rounded-full text-white" style="background-color: {lc};">
                        {LINE_META[item.lineNumber]?.name ?? item.lineNumber}
                      </span>
                      <span class="text-xs font-semibold px-2 py-0.5 rounded-full" style="background-color: {cInfo.bg}; color: {cInfo.text};">
                        {cInfo.label}
                      </span>
                    </div>
                    {#if item.arrivalMessage}
                      <p class="text-[17px] font-bold text-gray-900 leading-snug">{item.arrivalMessage}</p>
                    {:else}
                      <p class="text-[15px] font-medium text-gray-400">도착 정보 없음</p>
                    {/if}
                    <div class="flex items-center justify-between mt-2">
                      {#if item.trainNo}
                        <span class="text-xs text-gray-400">열차 {item.trainNo}</span>
                      {:else}
                        <span></span>
                      {/if}
                      <span class="text-xs text-gray-400">{formatTime(item.updatedAt)} 기준</span>
                    </div>
                  </div>
                </div>
              {/each}
            </div>
          {/if}

          <!-- 시간대별 혼잡도 차트 -->
          <button
            onclick={() => toggleHourly(station)}
            class="w-full mt-2 flex items-center justify-center gap-1.5 py-2.5 rounded-xl text-xs font-semibold bg-white shadow-sm text-gray-500 active:bg-gray-50 transition-colors"
          >
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round"
                d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 013 19.875v-6.75zM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V8.625zM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V4.125z" />
            </svg>
            시간대별 혼잡도
            <svg class="w-3 h-3 transition-transform {showHourly.has(station) ? 'rotate-180' : ''}"
                 fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5" />
            </svg>
          </button>

          {#if showHourly.has(station)}
            {@const hEntry = hourlyData[station]}
            <div class="bg-white rounded-2xl shadow-sm p-4 mt-1">
              {#if !hEntry || hEntry.loading}
                <div class="flex items-end gap-0.5 h-16">
                  {#each Array(24) as _}
                    <div class="flex-1 bg-gray-100 rounded-t animate-pulse" style="height: {20 + Math.random() * 60}%"></div>
                  {/each}
                </div>
              {:else if hEntry.data.length === 0}
                <p class="text-xs text-gray-400 text-center py-4">아직 수집된 데이터가 없어요</p>
              {:else}
                {@const bars = buildHourlyBars(hEntry.data)}
                {@const maxAvg = Math.max(...bars.map(b => b.avg), 1)}
                <p class="text-[11px] font-semibold text-gray-500 mb-2">시간대별 평균 혼잡도</p>
                <div class="flex items-end gap-[2px] h-16">
                  {#each bars as bar}
                    <div class="flex-1 flex flex-col items-center gap-0 relative group">
                      <div
                        class="w-full rounded-t transition-all"
                        style="height: {Math.max(4, (bar.avg / maxAvg) * 100)}%; background-color: {hourlyBarColor(bar.avg)};"
                        title="{bar.hour}시: {bar.avg.toFixed(0)}"
                      ></div>
                    </div>
                  {/each}
                </div>
                <!-- 시간 라벨 (0, 6, 12, 18, 24) -->
                <div class="flex mt-1">
                  {#each bars as bar}
                    <div class="flex-1 text-center">
                      {#if hourLabel(bar.hour)}
                        <span class="text-[8px] text-gray-400">{hourLabel(bar.hour)}</span>
                      {/if}
                    </div>
                  {/each}
                </div>
                <!-- 범례 -->
                <div class="flex items-center gap-3 mt-2 justify-end">
                  {#each [['#86efac','여유'],['#fde047','보통'],['#fb923c','혼잡'],['#f87171','매우혼잡']] as [color, label]}
                    <div class="flex items-center gap-1">
                      <div class="w-2 h-2 rounded-sm" style="background-color: {color};"></div>
                      <span class="text-[9px] text-gray-400">{label}</span>
                    </div>
                  {/each}
                </div>
              {/if}
            </div>
          {/if}
        </div>
      {/each}
    </section>
  {:else if $favorites.length === 0}
    <div class="flex flex-col items-center justify-center pt-12 pb-8 text-center">
      <div class="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center mb-4">
        <svg class="w-8 h-8 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15 10.5a3 3 0 11-6 0 3 3 0 016 0z" />
          <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 10.5c0 7.142-7.5 11.25-7.5 11.25S4.5 17.642 4.5 10.5a7.5 7.5 0 1115 0z" />
        </svg>
      </div>
      <p class="text-gray-700 font-semibold">자주 이용하는 역을 추가해보세요</p>
      <p class="text-sm text-gray-400 mt-1">탭 한 번으로 실시간 혼잡도를<br>바로 확인할 수 있어요</p>
    </div>
  {/if}
</div>

<!-- ══════════════════════════════════════════════════════════════════ -->
<!-- 노선도 탭                                                          -->
<!-- ══════════════════════════════════════════════════════════════════ -->
{:else}
<div class="pb-4">

  <!-- 노선 선택 칩 (가로 스크롤) -->
  <div class="overflow-x-auto no-scrollbar">
    <div class="flex gap-2 px-4 py-3" style="min-width: max-content;">
      {#each SUPPORTED_LINES as code}
        {@const meta = LINE_META[code]}
        <button
          onclick={() => selectedLine = code}
          class="flex-shrink-0 px-3 py-1.5 rounded-full text-xs font-bold transition-all active:scale-95"
          style="
            background-color: {selectedLine === code ? meta.color : meta.color + '15'};
            color: {selectedLine === code ? '#fff' : meta.color};
            border: 1.5px solid {selectedLine === code ? meta.color : meta.color + '40'};
          "
        >{meta.name}</button>
      {/each}
    </div>
  </div>

  <!-- 계통(분기) 필터 -->
  {#if currentBranches.length > 0}
    <div class="flex gap-1.5 px-4 pb-2 overflow-x-auto no-scrollbar">
      <button
        onclick={() => filterBranch = '전체'}
        class="flex-shrink-0 px-3 py-1 rounded-full text-xs font-semibold transition-all"
        style="background-color: {filterBranch === '전체' ? lineColor : '#F3F4F6'}; color: {filterBranch === '전체' ? '#fff' : '#6B7280'};"
      >전체 계통</button>
      {#each currentBranches as branch}
        <button
          onclick={() => filterBranch = branch.id}
          class="flex-shrink-0 px-3 py-1 rounded-full text-xs font-semibold transition-all"
          style="background-color: {filterBranch === branch.id ? lineColor : '#F3F4F6'}; color: {filterBranch === branch.id ? '#fff' : '#6B7280'};"
        >{branch.label}</button>
      {/each}
    </div>
  {/if}

  <!-- 방향 필터 + 업데이트 시각 -->
  <div class="flex items-center justify-between px-4 pb-3">
    <div class="flex gap-1.5">
      {#each ['전체', ...dirLabels] as dir}
        <button
          onclick={() => filterDir = dir}
          class="px-3 py-1 rounded-full text-xs font-semibold transition-all"
          style="
            background-color: {filterDir === dir ? lineColor : '#F3F4F6'};
            color: {filterDir === dir ? '#fff' : '#6B7280'};
          "
        >{dir}</button>
      {/each}
    </div>
    {#if lastUpdated}
      <span class="text-[10px] text-gray-400">{formatUpdated(lastUpdated)} 기준</span>
    {/if}
  </div>

  <!-- 노선 경보 배너 -->
  {#if $lineAlerts[selectedLine]}
    {@const alert = $lineAlerts[selectedLine]}
    <div class="mx-4 mb-3 rounded-2xl px-4 py-3 flex items-start justify-between gap-3
                {alert.severity === 'HIGH' ? 'bg-red-50 border border-red-200' : 'bg-orange-50 border border-orange-200'}">
      <div class="flex items-start gap-2">
        <div class="w-2 h-2 rounded-full animate-pulse mt-1.5 flex-shrink-0
                    {alert.severity === 'HIGH' ? 'bg-red-500' : 'bg-orange-400'}"></div>
        <div>
          <p class="text-[13px] font-bold {alert.severity === 'HIGH' ? 'text-red-700' : 'text-orange-700'}">
            {alert.alertType === 'CONGESTION_SPIKE' ? '혼잡도 급등 감지' : '운행 이슈'}
          </p>
          <p class="text-[12px] mt-0.5 {alert.severity === 'HIGH' ? 'text-red-600' : 'text-orange-600'}">
            {alert.message}
          </p>
        </div>
      </div>
      <button onclick={() => dismissAlert(selectedLine)} class="flex-shrink-0 mt-0.5
              {alert.severity === 'HIGH' ? 'text-red-400 active:text-red-600' : 'text-orange-400 active:text-orange-600'}">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
        </svg>
      </button>
    </div>
  {/if}

  <!-- 에러 -->
  {#if lineError}
    <div class="mx-4 bg-red-50 rounded-2xl px-4 py-3 text-sm text-red-500 mb-3">{lineError}</div>
  {/if}

  <!-- 로딩 -->
  {#if lineLoading}
    <div class="flex flex-col items-center justify-center py-16 gap-3">
      <div class="w-10 h-10 rounded-full border-4 border-gray-200 animate-spin" style="border-top-color: {lineColor};"></div>
      <p class="text-sm text-gray-400">열차 위치 조회 중...</p>
    </div>

  <!-- 노선도 본체 -->
  {:else if stations.length > 0}
    <div class="relative px-4">

      <!-- mock 안내 배너 -->
      {#if useMock}
        <div class="mb-3 bg-amber-50 rounded-2xl px-4 py-3 flex items-start gap-2">
          <svg class="w-4 h-4 text-amber-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
          </svg>
          <div>
            <p class="text-xs font-bold text-amber-700">API 한도 초과 — 시연 모드</p>
            <p class="text-[11px] text-amber-600 mt-0.5">열차 위치는 임의로 표시됩니다. 자정 이후 실제 데이터로 전환됩니다.</p>
          </div>
        </div>
      {/if}

      <!-- 열차 수 요약 -->
      {#if trainData.length > 0}
        <div class="mb-3 flex items-center gap-2">
          <div class="w-2 h-2 rounded-full {useMock ? '' : 'animate-pulse'}" style="background-color: {useMock ? '#F59E0B' : lineColor};"></div>
          <span class="text-xs font-semibold" style="color: {useMock ? '#D97706' : lineColor};">
            {filteredTrains.length}개 열차 {useMock ? '(시연)' : '운행 중'}
          </span>
          {#if filterBranch !== '전체'}
            <span class="text-xs font-semibold px-2 py-0.5 rounded-full text-white text-[10px]"
                  style="background-color: {lineColor};">
              {currentBranches.find(b => b.id === filterBranch)?.label}
            </span>
          {/if}
          {#if filterDir !== '전체'}
            <span class="text-xs text-gray-400">({filterDir})</span>
          {/if}
        </div>
      {:else}
        <div class="mb-3 bg-amber-50 rounded-2xl px-4 py-3 text-sm text-amber-600">
          현재 운행 정보가 없습니다 — 상단 새로고침을 눌러주세요
        </div>
      {/if}

      <!-- ── 절대위치 노선도 ─────────────────────────────────────── -->
      <div class="relative mx-4"
           style="height: {stations.length * SEGMENT_PX + 20}px;">

        <!-- 노선 트랙 세로선 -->
        <div class="absolute rounded-full"
             style="left: 22px; top: 8px; width: 5px;
                    height: {(stations.length - 1) * SEGMENT_PX}px;
                    background: linear-gradient(to bottom, {lineColor}, {lineColor}cc);"></div>

        <!-- 역 점 + 이름 -->
        {#each stations as station, i}
          <div class="absolute flex items-center gap-3"
               id="station-{station}"
               style="top: {i * SEGMENT_PX}px; left: 0; right: 0;">
            <!-- 역 원 -->
            <div class="w-12 flex justify-center flex-shrink-0">
              <div class="w-[14px] h-[14px] rounded-full border-[3px] bg-white z-10"
                   style="border-color: {lineColor}; box-shadow: 0 0 0 2px white;"></div>
            </div>
            <!-- 역명 -->
            <span class="text-[13px] font-semibold text-gray-800 leading-none">{station}</span>
          </div>
        {/each}

        <!-- 열차 아이콘 (절대 위치 + CSS 이동 애니메이션) -->
        {#each filteredTrains as train (train.trainNo)}
          {@const topPx = trainTopPx(train)}
          {@const moveDist = trainMoveDist(train)}
          {@const leftPx = trainLeftPx(train)}
          {@const up = isUpward(train)}
          {@const isSelected = selectedTrain?.trainNo === train.trainNo}
          {@const trainColor = useMock ? '#F59E0B' : (train.express ? '#EF4444' : lineColor)}
          {#if topPx !== null}
            <button
              onclick={() => selectedTrain = isSelected ? null : train}
              class="absolute z-20 train-btn flex flex-col items-center"
              style="
                top: {topPx - (up ? 25 : 16)}px;
                left: {leftPx}px;
                --move-dist: {moveDist}px;
                --anim-dur: {train.etaSeconds > 0 ? train.etaSeconds : 0}s;
              "
              aria-label="열차 {train.trainNo}"
            >
              <!-- 상행 위쪽 화살표 -->
              {#if up}
                <svg width="10" height="7" viewBox="0 0 10 7" class="mb-0.5 flex-shrink-0">
                  <polygon points="5,0 10,7 0,7" fill="{trainColor}"/>
                </svg>
              {/if}

              <!-- 지하철 차량 SVG (탑뷰, 상행은 180° 회전) -->
              <div class="train-car {isSelected ? 'selected' : ''} {up ? 'up' : ''} {train.express ? 'express' : ''}"
                   style="background: {trainColor}; box-shadow: 0 3px 10px {trainColor}66;">
                <svg width="20" height="30" viewBox="0 0 20 30" fill="none">
                  <rect x="1" y="1" width="18" height="28" rx="5" fill="white" opacity="0.25"/>
                  <rect x="3.5" y="3"  width="13" height="7"  rx="2" fill="white" opacity="0.55"/>
                  <rect x="3.5" y="13" width="13" height="5.5" rx="1.5" fill="white" opacity="0.45"/>
                  <rect x="3.5" y="21" width="13" height="5"   rx="1.5" fill="white" opacity="0.35"/>
                </svg>
              </div>

              <!-- 하행 아래쪽 화살표 -->
              {#if !up}
                <svg width="10" height="7" viewBox="0 0 10 7" class="mt-0.5 flex-shrink-0">
                  <polygon points="5,7 10,0 0,0" fill="{trainColor}"/>
                </svg>
              {/if}

              <!-- 선택 시 ETA 말풍선 (아이콘 아래 중앙) -->
              {#if isSelected}
                <div class="absolute top-full left-1/2 -translate-x-1/2 mt-1 bg-white rounded-xl px-2 py-1 shadow-lg whitespace-nowrap border z-30"
                     style="border-color: {trainColor}40;">
                  <p class="text-[11px] font-bold" style="color: {trainColor};">
                    {train.express ? '[급행] ' : ''}{train.destination ?? train.direction}
                  </p>
                  <p class="text-[10px] text-gray-400">{etaLabel(train.etaSeconds)}</p>
                </div>
              {/if}
            </button>
          {/if}
        {/each}

      </div><!-- /절대위치 노선도 -->

    </div>

  {:else}
    <div class="flex flex-col items-center justify-center py-16 text-center">
      <p class="text-gray-400 text-sm">노선 정보를 불러올 수 없습니다</p>
    </div>
  {/if}
</div>
{/if}

<!-- ── 선택된 열차 하단 상세 패널 ─────────────────────────────────── -->
{#if selectedTrain && activeTab === 'linemap'}
  <!-- 딤 배경 -->
  <div class="fixed inset-0 z-[55]" onclick={() => selectedTrain = null} role="presentation"></div>

  <!-- 패널 -->
  <div class="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[430px] bg-white z-[60] rounded-t-3xl shadow-2xl"
       style="padding-bottom: calc(56px + max(1rem, env(safe-area-inset-bottom)));">
    <!-- 드래그 핸들 -->
    <div class="flex justify-center pt-3 pb-1">
      <div class="w-10 h-1 bg-gray-200 rounded-full"></div>
    </div>

    <div class="px-5 pt-2 pb-4">
      <!-- 헤더 -->
      <div class="flex items-center justify-between mb-3">
        <div class="flex items-center gap-2">
          <span class="text-xs font-bold px-2.5 py-1 rounded-full text-white"
                style="background: {lineColor};">{lineName}</span>
          <span class="text-[17px] font-bold text-gray-900">
            {selectedTrain.destination ?? selectedTrain.direction ?? '운행 중'}
          </span>
        </div>
        <button onclick={() => selectedTrain = null}
                class="w-8 h-8 flex items-center justify-center rounded-full bg-gray-100 active:bg-gray-200">
          <svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>

      <!-- 도착 메시지 -->
      <p class="text-[15px] text-gray-700 mb-4">
        {selectedTrain.arrivalMessage ?? '위치 정보 없음'}
      </p>

      <!-- 구간 프로그레스바 -->
      {#each [getProgressInfo(selectedTrain)] as sp}
        {#if sp}
          <div class="mb-4">
            <div class="flex justify-between text-xs font-semibold text-gray-500 mb-2">
              <span>{sp.currentStation}</span>
              <span>{sp.nextStation}</span>
            </div>
            <div class="h-2.5 bg-gray-100 rounded-full overflow-hidden">
              <div class="h-full rounded-full transition-all duration-500"
                   style="width: {sp.progress * 100}%; background: {lineColor};"></div>
            </div>
            <div class="flex justify-between text-[10px] text-gray-400 mt-1">
              <span>●</span><span>○</span>
            </div>
          </div>
        {/if}
      {/each}

      <!-- 열차번호 + ETA -->
      <div class="flex items-center justify-between mb-5">
        <span class="text-sm text-gray-400">
          열차 <span class="font-semibold text-gray-600">{selectedTrain.trainNo}</span>
        </span>
        <span class="text-sm font-bold" style="color: {lineColor};">
          {selectedTrain.etaSeconds > 0 ? `약 ${etaLabel(selectedTrain.etaSeconds)}` : '곧 도착'}
        </span>
      </div>

      <!-- 이전/다음 열차 -->
      <div class="flex gap-3">
        <button onclick={selectPrev}
                disabled={selectedTrainIdx <= 0}
                class="flex-1 bg-gray-100 text-gray-700 text-sm font-bold py-3.5 rounded-2xl
                       disabled:opacity-30 active:bg-gray-200 transition-colors">
          ← 이전 열차
        </button>
        <button onclick={selectNext}
                disabled={selectedTrainIdx >= sortedTrains.length - 1}
                class="flex-1 text-white text-sm font-bold py-3.5 rounded-2xl
                       disabled:opacity-30 active:opacity-80 transition-colors"
                style="background: {lineColor};">
          다음 열차 →
        </button>
      </div>

      <!-- 민원 접수 -->
      <a href="/complaints?trainNo={encodeURIComponent(selectedTrain.trainNo)}&lineCode={encodeURIComponent(selectedTrain.lineCode)}&lineName={encodeURIComponent(lineName)}&station={encodeURIComponent(selectedTrain.currentStation)}&direction={encodeURIComponent(selectedTrain.direction ?? '')}&destination={encodeURIComponent(selectedTrain.destination ?? '')}"
         onclick={() => selectedTrain = null}
         class="mt-1 flex items-center justify-center gap-2 w-full text-sm font-bold py-3.5 rounded-2xl
                active:opacity-80 transition-colors"
         style="background: #fff7ed; color: #ea580c; border: 1.5px solid #fed7aa;">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round"
            d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
        </svg>
        이 열차로 민원 접수
      </a>
    </div>
  </div>
{/if}

<style>
  .no-scrollbar::-webkit-scrollbar { display: none; }
  .no-scrollbar { -ms-overflow-style: none; scrollbar-width: none; }

  /* 지하철 차량 스타일 */
  .train-car {
    width: 22px;
    height: 32px;
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: transform 0.15s ease, box-shadow 0.15s ease;
  }
  .train-car.up              { transform: rotate(180deg); }
  .train-car.selected        { transform: scale(1.25); z-index: 30; }
  .train-car.up.selected     { transform: rotate(180deg) scale(1.25); z-index: 30; }
  .train-btn:active .train-car     { transform: scale(1.1); }
  .train-btn:active .train-car.up  { transform: rotate(180deg) scale(1.1); }

  /* 열차 이동 애니메이션 */
  @keyframes trainMove {
    from { transform: translateY(0); }
    to   { transform: translateY(var(--move-dist)); }
  }
  .train-btn {
    animation: trainMove var(--anim-dur) linear forwards;
  }
</style>
