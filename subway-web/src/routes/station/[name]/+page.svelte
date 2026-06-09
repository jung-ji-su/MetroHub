<script>
  import { page } from '$app/stores';
  import { onMount } from 'svelte';
  import { api } from '$lib/api';
  import { favorites } from '$lib/stores';
  import { LINE_META, LINE_STATIONS } from '$lib/lineStations';
  import { congestionAlerts } from '$lib/sseStore';

  const stationName = $derived(decodeURIComponent($page.params.name));

  // 이 역이 속한 호선 목록
  const stationLines = $derived(
    Object.entries(LINE_STATIONS)
      .filter(([, stations]) => stations.includes(stationName))
      .map(([code]) => ({ code, ...LINE_META[code] }))
  );

  const isFav = $derived($favorites.includes(stationName));

  function toggleFav() {
    if (isFav) favorites.remove(stationName);
    else favorites.add(stationName);
  }

  // 혼잡도
  let congestion = $state([]);  // [{ lineNumber, congestionLevel, congestionText }]
  let congestionLoading = $state(true);

  // 시간대별 차트
  let hourlyData = $state([]);
  let hourlyLoading = $state(false);
  let showHourly = $state(false);

  // 실시간 도착
  let arrivals = $state([]);
  let arrivalsLoading = $state(true);
  let arrivalsError = $state('');

  async function loadCongestion() {
    congestionLoading = true;
    try {
      const data = await api.congestionByStation(stationName);
      congestion = Array.isArray(data) ? data : [data].filter(Boolean);
    } catch (_) {
      congestion = [];
    } finally {
      congestionLoading = false;
    }
  }

  async function loadArrivals() {
    arrivalsLoading = true;
    arrivalsError = '';
    try {
      const data = await api.stationArrival(stationName);
      arrivals = Array.isArray(data) ? data : [];
    } catch (e) {
      arrivalsError = e.message || '도착 정보를 불러올 수 없습니다.';
      arrivals = [];
    } finally {
      arrivalsLoading = false;
    }
  }

  async function toggleHourly() {
    showHourly = !showHourly;
    if (showHourly && hourlyData.length === 0) {
      hourlyLoading = true;
      try {
        const raw = await api.congestionHourly(stationName);
        const map = {};
        for (const d of raw) map[d.hourOfDay] = d.avgCongestion;
        hourlyData = Array.from({ length: 24 }, (_, h) => ({ hour: h, avg: map[h] ?? 0 }));
      } catch (_) {
        hourlyData = [];
      } finally {
        hourlyLoading = false;
      }
    }
  }

  function congestionColor(level) {
    if (!level) return '#e5e7eb';
    if (level >= 90) return '#FCA5A5';
    if (level >= 80) return '#FCD34D';
    if (level >= 60) return '#86EFAC';
    return '#BAE6FD';
  }

  function congestionLabel(level) {
    if (!level) return '정보없음';
    if (level >= 90) return '매우혼잡';
    if (level >= 80) return '혼잡';
    if (level >= 60) return '보통';
    return '여유';
  }

  const maxHourly = $derived(Math.max(...hourlyData.map(d => d.avg), 1));

  const alert = $derived($congestionAlerts[stationName]);

  onMount(() => {
    loadCongestion();
    loadArrivals();
  });
</script>

<div class="min-h-screen bg-gray-50">
  <!-- 헤더 -->
  <header class="px-5 pt-12 pb-4 sticky top-0 z-40"
          style="background: #ffffff; border-bottom: 1px solid #f3f4f6;">
    <div class="flex items-center gap-3">
      <a href="javascript:history.back()"
         class="w-9 h-9 flex items-center justify-center rounded-full bg-gray-100 active:bg-gray-200 flex-shrink-0"
         aria-label="뒤로가기">
        <svg class="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
        </svg>
      </a>
      <div class="flex-1 min-w-0">
        <h1 class="text-[17px] font-bold text-gray-900 truncate">{stationName}역</h1>
        <p class="text-[12px] text-gray-400 mt-0.5">
          {stationLines.map(l => l.name).join(' · ')}
        </p>
      </div>
      <!-- 즐겨찾기 버튼 -->
      <button onclick={toggleFav}
              class="w-9 h-9 flex items-center justify-center rounded-full transition-colors active:scale-90
                     {isFav ? 'bg-yellow-50' : 'bg-gray-100 active:bg-gray-200'}"
              aria-label="즐겨찾기">
        <svg class="w-5 h-5 {isFav ? 'text-yellow-400' : 'text-gray-400'}"
             fill={isFav ? 'currentColor' : 'none'}
             stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round"
            d="M11.48 3.499a.562.562 0 011.04 0l2.125 5.111a.563.563 0 00.475.345l5.518.442c.499.04.701.663.321.988l-4.204 3.602a.563.563 0 00-.182.557l1.285 5.385a.562.562 0 01-.84.61l-4.725-2.885a.563.563 0 00-.586 0L6.982 20.54a.562.562 0 01-.84-.61l1.285-5.386a.562.562 0 00-.182-.557l-4.204-3.602a.562.562 0 01.321-.988l5.518-.442a.563.563 0 00.475-.345L11.48 3.5z" />
        </svg>
      </button>
    </div>
  </header>

  <div class="px-4 py-4 space-y-4">

    <!-- 혼잡 경보 -->
    {#if alert}
      <div class="rounded-2xl px-4 py-3 flex items-center gap-3 animate-pulse"
           style="background: {alert.severity === 'VERY_CROWDED' ? '#FEE2E2' : '#FFEDD5'};">
        <span class="text-xl">{alert.severity === 'VERY_CROWDED' ? '🚨' : '🥵'}</span>
        <div>
          <p class="text-[13px] font-bold" style="color: {alert.severity === 'VERY_CROWDED' ? '#DC2626' : '#EA580C'};">
            {alert.severity === 'VERY_CROWDED' ? '매우혼잡' : '혼잡'} 경보
          </p>
          <p class="text-[12px] text-gray-600">{alert.message ?? '현재 혼잡도가 높습니다'}</p>
        </div>
      </div>
    {/if}

    <!-- 호선별 혼잡도 -->
    <div class="bg-white rounded-2xl shadow-sm p-4">
      <h2 class="text-[14px] font-bold text-gray-900 mb-3">실시간 혼잡도</h2>
      {#if congestionLoading}
        <div class="flex gap-2">
          {#each [1,2] as _}
            <div class="flex-1 h-16 bg-gray-100 rounded-xl animate-pulse"></div>
          {/each}
        </div>
      {:else if congestion.length === 0}
        <!-- 호선별 기본 카드 (API 데이터 없을 때) -->
        <div class="flex flex-wrap gap-2">
          {#each stationLines as line}
            <div class="flex items-center gap-2 px-3 py-2 rounded-xl bg-gray-50">
              <span class="w-3 h-3 rounded-full flex-shrink-0" style="background:{line.color}"></span>
              <span class="text-[13px] font-semibold text-gray-700">{line.name}</span>
              <span class="text-[12px] text-gray-400">정보없음</span>
            </div>
          {/each}
        </div>
      {:else}
        <div class="flex flex-wrap gap-2">
          {#each congestion as c}
            {@const meta = LINE_META[c.lineNumber]}
            {@const color = congestionColor(c.congestionLevel)}
            <div class="flex items-center gap-2 px-3 py-2.5 rounded-xl flex-1 min-w-[120px]"
                 style="background:{color}20; border: 1.5px solid {color};">
              {#if meta}
                <span class="w-2.5 h-2.5 rounded-full flex-shrink-0" style="background:{meta.color}"></span>
                <span class="text-[12px] font-bold text-gray-700">{meta.name}</span>
              {/if}
              <span class="ml-auto text-[13px] font-bold" style="color:{color === '#e5e7eb' ? '#9ca3af' : '#374151'}">
                {congestionLabel(c.congestionLevel)}
              </span>
            </div>
          {/each}
        </div>
      {/if}

      <!-- 시간대별 차트 토글 -->
      <button onclick={toggleHourly}
              class="mt-3 w-full flex items-center justify-between px-3 py-2 rounded-xl bg-gray-50 active:bg-gray-100 transition-colors">
        <span class="text-[13px] font-semibold text-gray-700">시간대별 평균 혼잡도</span>
        <svg class="w-4 h-4 text-gray-400 transition-transform {showHourly ? 'rotate-180' : ''}"
             fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {#if showHourly}
        <div class="mt-3">
          {#if hourlyLoading}
            <div class="h-24 bg-gray-100 rounded-xl animate-pulse"></div>
          {:else if hourlyData.length === 0}
            <p class="text-[12px] text-gray-400 text-center py-4">데이터가 없습니다</p>
          {:else}
            <div class="flex items-end gap-[2px] h-20">
              {#each hourlyData as d}
                <div class="flex-1 flex flex-col items-center gap-[2px]">
                  <div class="w-full rounded-t"
                       style="height:{Math.max((d.avg/maxHourly)*64, d.avg > 0 ? 4 : 0)}px;
                              background:{congestionColor(d.avg)};
                              min-height:{d.avg > 0 ? '4px' : '0'}">
                  </div>
                  {#if d.hour % 6 === 0}
                    <span class="text-[8px] text-gray-400">{d.hour}시</span>
                  {:else}
                    <span class="text-[8px] text-transparent">·</span>
                  {/if}
                </div>
              {/each}
            </div>
          {/if}
        </div>
      {/if}
    </div>

    <!-- 실시간 도착 정보 -->
    <div class="bg-white rounded-2xl shadow-sm p-4">
      <div class="flex items-center justify-between mb-3">
        <h2 class="text-[14px] font-bold text-gray-900">실시간 도착 정보</h2>
        <button onclick={loadArrivals}
                class="w-7 h-7 flex items-center justify-center rounded-full bg-gray-100 active:bg-gray-200"
                aria-label="새로고침">
          <svg class="w-4 h-4 text-gray-500 {arrivalsLoading ? 'animate-spin' : ''}"
               fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round"
              d="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M4.031 9.865a8.25 8.25 0 0113.803-3.7l3.181 3.182m0-4.991v4.99" />
          </svg>
        </button>
      </div>
      {#if arrivalsLoading}
        <div class="space-y-2">
          {#each [1,2,3] as _}
            <div class="h-12 bg-gray-100 rounded-xl animate-pulse"></div>
          {/each}
        </div>
      {:else if arrivalsError}
        <p class="text-[13px] text-gray-400 text-center py-4">{arrivalsError}</p>
      {:else if arrivals.length === 0}
        <p class="text-[13px] text-gray-400 text-center py-4">도착 예정 열차가 없습니다</p>
      {:else}
        <div class="space-y-2">
          {#each arrivals.slice(0, 6) as arr}
            {@const meta = LINE_META[arr.lineNumber] ?? LINE_META[arr.subwayId]}
            <div class="flex items-center gap-3 px-3 py-2.5 rounded-xl bg-gray-50">
              {#if meta}
                <span class="text-[11px] font-bold px-2 py-0.5 rounded-full text-white flex-shrink-0"
                      style="background:{meta.color}">
                  {meta.name}
                </span>
              {/if}
              <div class="flex-1 min-w-0">
                <p class="text-[13px] font-semibold text-gray-800 truncate">
                  {arr.trainLineNm ?? arr.destination ?? '행선지 정보없음'}
                </p>
                <p class="text-[11px] text-gray-400 truncate">{arr.arvlMsg2 ?? arr.message ?? ''}</p>
              </div>
              <span class="text-[13px] font-bold flex-shrink-0"
                    style="color:{meta?.color ?? '#6b7280'}">
                {arr.arvlMsg3 ?? arr.arrivalTime ?? ''}
              </span>
            </div>
          {/each}
        </div>
      {/if}
    </div>

    <!-- 관련 호선 커뮤니티 -->
    {#if stationLines.length > 0}
      <div class="bg-white rounded-2xl shadow-sm p-4">
        <h2 class="text-[14px] font-bold text-gray-900 mb-3">관련 커뮤니티</h2>
        <div class="flex flex-wrap gap-2">
          {#each stationLines as line}
            <a href="/community/{line.code}"
               class="flex items-center gap-2 px-3 py-2 rounded-xl active:opacity-70 transition-opacity"
               style="background:{line.color}15; border: 1.5px solid {line.color}40;">
              <span class="w-2.5 h-2.5 rounded-full" style="background:{line.color}"></span>
              <span class="text-[13px] font-semibold" style="color:{line.color}">{line.name}</span>
              <svg class="w-3.5 h-3.5" style="color:{line.color}" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
              </svg>
            </a>
          {/each}
        </div>
      </div>
    {/if}

  </div>
</div>
