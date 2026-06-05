<script>
  import { api } from '$lib/api';
  import { token, user } from '$lib/stores';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { LINE_META } from '$lib/lineStations';
  import StationSearch from '$lib/StationSearch.svelte';

  // URL 파라미터에서 열차 정보 파싱 (노선도에서 넘어온 경우)
  const trainNo      = $page.url.searchParams.get('trainNo') ?? '';
  const lineCode     = $page.url.searchParams.get('lineCode') ?? '';
  const lineName     = decodeURIComponent($page.url.searchParams.get('lineName') ?? '');
  const trainStation = decodeURIComponent($page.url.searchParams.get('station') ?? '');
  const direction    = decodeURIComponent($page.url.searchParams.get('direction') ?? '');
  const destination  = decodeURIComponent($page.url.searchParams.get('destination') ?? '');
  const hasTrainInfo = !!trainNo;

  // 운영사별 문자/전화 접수 정보
  const SMS_INFO = {
    '1001': { number: '1577-1234', operator: '서울교통공사', note: '1·3·4호선 일부 코레일 구간은 1544-7769' },
    '1002': { number: '1577-1234', operator: '서울교통공사' },
    '1003': { number: '1577-1234', operator: '서울교통공사', note: '1·3·4호선 일부 코레일 구간은 1544-7769' },
    '1004': { number: '1577-1234', operator: '서울교통공사', note: '1·3·4호선 일부 코레일 구간은 1544-7769' },
    '1005': { number: '1577-1234', operator: '서울교통공사' },
    '1006': { number: '1577-1234', operator: '서울교통공사' },
    '1007': { number: '1577-1234', operator: '서울교통공사', note: '인천 구간(석남↔까치울)은 1899-4446' },
    '1008': { number: '1577-1234', operator: '서울교통공사' },
    '1009': { number: '1544-4009', operator: '서울시메트로9호선' },
    '1063': { number: '1544-7769', operator: '코레일' },
    '1065': { number: '1599-7788', operator: '공항철도(주)' },
    '1067': { number: '1544-7769', operator: '코레일' },
    '1069': { number: '1899-4446', operator: '인천교통공사' },
    '1071': { number: '1899-4446', operator: '인천교통공사' },
    '1073': { number: '1899-2111', operator: '의정부경전철' },
    '1074': { number: '031-8048-1500', operator: '김포골드라인', phoneOnly: true },
    '1075': { number: '031-8018-7777', operator: '신분당선', phoneOnly: true },
    '1077': { number: '1544-7769', operator: '코레일' },
    '1079': { number: '1899-9001', operator: '에버라인' },
    '1081': { number: '1544-7769', operator: '코레일' },
    '1092': { number: '1577-1234', operator: '서울교통공사' },
    '1093': { number: '1544-7769', operator: '코레일' },
    '1094': { number: '1577-1234', operator: '서울교통공사' },
    '1021': { number: '1544-7769', operator: '코레일(GTX-A)' },
  };

  const ALL_LINES = Object.entries(LINE_META).map(([code, meta]) => ({ code, ...meta }));

  let category       = $state('');
  let stationName    = $state(trainStation);
  let content        = $state('');
  let error          = $state('');
  let success        = $state(false);
  let smsSent        = $state(false);
  let loading        = $state(false);

  // 직접 입력 필드 (노선도에서 넘어오지 않은 경우)
  let manualLineCode  = $state('');
  let manualTrainNo   = $state('');
  let carNo           = $state('');
  let arrivingTrains  = $state(/** @type {any[]} */([]));
  let loadingTrains   = $state(false);
  let selectedTrainId = $state('');

  // 실제 사용할 열차/노선 정보 (URL 파라미터 우선, 없으면 직접 입력)
  const effectiveLineCode  = $derived(hasTrainInfo ? lineCode    : manualLineCode);
  const effectiveLineName  = $derived(hasTrainInfo ? lineName    : (/** @type {any} */(LINE_META)[manualLineCode]?.name ?? ''));
  const effectiveTrainNo   = $derived(hasTrainInfo ? trainNo     : (selectedTrainId || manualTrainNo));
  const effectiveDirection = $derived(hasTrainInfo ? direction   : (arrivingTrains.find(t => t.trainNo === selectedTrainId)?.direction ?? ''));
  const effectiveDest      = $derived(hasTrainInfo ? destination : (arrivingTrains.find(t => t.trainNo === selectedTrainId)?.destination ?? ''));
  const lineColor          = $derived(/** @type {any} */(LINE_META)[effectiveLineCode]?.color ?? '#2563EB');
  const smsInfo            = $derived(/** @type {any} */(SMS_INFO)[effectiveLineCode] ?? { number: '120', operator: '다산콜센터' });

  // 노선 또는 역 변경 시 열차 목록 초기화
  $effect(() => {
    const _lc  = manualLineCode;
    const _stn = stationName;
    arrivingTrains  = [];
    selectedTrainId = '';
  });

  async function loadArrivingTrains() {
    if (!manualLineCode) return;
    loadingTrains = true;
    arrivingTrains = [];
    try {
      const all = await api.lineTrains(manualLineCode);
      arrivingTrains = stationName.trim()
        ? all.filter(/** @param {any} t */ t => t.nextStation === stationName.trim())
        : all.slice(0, 30);
    } catch (_) {}
    finally { loadingTrains = false; }
  }

  const CATEGORIES = [
    {
      id: '냉난방',
      label: '냉난방',
      icon: `<path stroke-linecap="round" stroke-linejoin="round" d="M15.362 5.214A8.252 8.252 0 0112 21 8.25 8.25 0 016.038 7.048 8.287 8.287 0 009 9.6a8.983 8.983 0 013.361-6.867 8.21 8.21 0 003 2.48z" /><path stroke-linecap="round" stroke-linejoin="round" d="M12 18a3.75 3.75 0 00.495-7.467 5.99 5.99 0 00-1.925 3.546 5.974 5.974 0 01-2.133-1A3.75 3.75 0 0012 18z" />`,
    },
    {
      id: '청결 불량',
      label: '청결',
      icon: `<path stroke-linecap="round" stroke-linejoin="round" d="M9.75 3.104v5.714a2.25 2.25 0 01-.659 1.591L5 14.5M9.75 3.104c-.251.023-.501.05-.75.082m.75-.082a24.301 24.301 0 014.5 0m0 0v5.714c0 .597.237 1.17.659 1.591L19.8 15M14.25 3.104c.251.023.501.05.75.082M19.8 15a2.25 2.25 0 01.169 2.603L20.04 18a2.25 2.25 0 01-2.121 1.5H6.08a2.25 2.25 0 01-2.12-1.5l-.044-.397A2.25 2.25 0 014.084 15m15.716 0h-15.716" />`,
    },
    {
      id: '시설 파손',
      label: '시설파손',
      icon: `<path stroke-linecap="round" stroke-linejoin="round" d="M11.42 15.17L17.25 21A2.652 2.652 0 0021 17.25l-5.877-5.877M11.42 15.17l2.496-3.03c.317-.384.74-.626 1.208-.766M11.42 15.17l-4.655 5.653a2.548 2.548 0 11-3.586-3.586l6.837-5.63m5.108-.233c.55-.164 1.163-.188 1.743-.14a4.5 4.5 0 004.486-6.336l-3.276 3.277a3.004 3.004 0 01-2.25-2.25l3.276-3.276a4.5 4.5 0 00-6.336 4.486c.091 1.076-.071 2.264-.904 2.95l-.102.085m-1.745 1.437L5.909 7.5H4.5L2.25 3.75l1.5-1.5L7.5 4.5v1.409l4.26 4.26m-1.745 1.437l1.745-1.437m6.615 8.206L15.75 15.75M4.867 19.125h.008v.008h-.008v-.008z" />`,
    },
    {
      id: '안전 위협',
      label: '안전위협',
      icon: `<path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />`,
    },
    {
      id: '직원 불친절',
      label: '직원불친절',
      icon: `<path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />`,
    },
    {
      id: '운행 지연',
      label: '운행지연',
      icon: `<path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z" />`,
    },
    {
      id: '의료/응급',
      label: '의료/응급',
      icon: `<path stroke-linecap="round" stroke-linejoin="round" d="M21 8.25c0-2.485-2.099-4.5-4.688-4.5-1.935 0-3.597 1.126-4.312 2.733-.715-1.607-2.377-2.733-4.313-2.733C5.1 3.75 3 5.765 3 8.25c0 7.22 9 12 9 12s9-4.78 9-12z" />`,
    },
    {
      id: '기타',
      label: '기타',
      icon: `<path stroke-linecap="round" stroke-linejoin="round" d="M8.625 12a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H8.25m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H12m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0h-.375M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />`,
    },
  ];

  function buildSmsBody() {
    const ln   = effectiveLineName;
    const tn   = effectiveTrainNo;
    const dir  = effectiveDirection;
    const dest = effectiveDest;
    const stn  = stationName;
    const car  = carNo.trim();

    const destTag = dest ? `_${dest}방면` : (dir ? `_${dir}` : '');
    const header  = ln ? `[지하철민원 (${ln}${destTag})]` : '[지하철민원]';

    const rows = [header];
    rows.push(`■ 위치: ${ln ? ln + ' ' : ''}${stn}역${dir ? ` (${dir})` : ''}`);
    if (tn)  rows.push(`■ 열차번호: ${tn}`);
    if (car) rows.push(`■ 탑승 칸: ${car}`);
    rows.push(`■ 유형: ${category}`);
    rows.push(`■ 내용: ${content}`);
    return rows.join('\n');
  }

  async function doSubmit() {
    if (!$token) { goto('/auth/login'); return false; }
    error = ''; loading = true;
    try {
      const hasAny = !!(effectiveTrainNo || effectiveLineCode);
      await api.createComplaint({
        category,
        stationName,
        content,
        ...(hasAny && {
          trainNo:     effectiveTrainNo    || null,
          lineCode:    effectiveLineCode   || null,
          lineName:    effectiveLineName   || null,
          direction:   effectiveDirection  || null,
          destination: effectiveDest       || null,
        }),
      }, $token);
      return true;
    } catch (e) {
      error = e instanceof Error ? e.message : String(e);
      return false;
    } finally {
      loading = false;
    }
  }

  function resetForm() {
    category = ''; stationName = trainStation; content = '';
    carNo = ''; manualTrainNo = ''; selectedTrainId = ''; manualLineCode = '';
  }

  async function submitApp() {
    const ok = await doSubmit();
    if (ok) { smsSent = false; success = true; resetForm(); }
  }

  async function submitWithSms() {
    const body = buildSmsBody();
    const num  = smsInfo.number.replace(/-/g, '');
    const ok   = await doSubmit();
    if (ok) {
      smsSent = true; success = true; resetForm();
      const scheme = smsInfo.phoneOnly ? 'tel' : 'sms';
      const qs     = smsInfo.phoneOnly ? '' : `?body=${encodeURIComponent(body)}`;
      setTimeout(() => { window.location.href = `${scheme}:${num}${qs}`; }, 200);
    }
  }

  const canSubmit = $derived(!!category && !!stationName.trim() && !!content.trim() && !loading);
</script>

<!-- 헤더 -->
<header class="px-5 pt-12 pb-4 sticky top-0 z-40" style="background: #ffffff; border-bottom: 1px solid #f3f4f6;">
  <div class="flex items-center justify-between">
    <div>
      <p class="text-xs text-gray-400 font-medium tracking-wide">METROHUB</p>
      <h1 class="text-xl font-bold text-gray-900 leading-tight">민원 접수</h1>
    </div>
    <a
      href="/complaints/my"
      class="text-xs font-semibold text-blue-600 bg-blue-50 px-3 py-1.5 rounded-full active:opacity-70"
    >내 민원</a>
  </div>
</header>

<div class="px-4 pt-5 pb-6 space-y-6">

  {#if !$user}
    <div class="flex flex-col items-center justify-center pt-12 pb-8 text-center">
      <div class="w-16 h-16 bg-blue-50 rounded-full flex items-center justify-center mb-4">
        <svg class="w-8 h-8 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
        </svg>
      </div>
      <p class="text-gray-700 font-semibold mb-1">로그인이 필요해요</p>
      <p class="text-sm text-gray-400 mb-5">민원 접수는 로그인 후 이용할 수 있습니다</p>
      <a href="/auth/login" class="bg-blue-600 text-white text-sm font-bold px-8 py-3 rounded-2xl active:bg-blue-700 transition-colors">
        로그인하기
      </a>
    </div>

  {:else if success}
    <div class="flex flex-col items-center justify-center pt-12 pb-8 text-center">
      <div class="w-16 h-16 rounded-full flex items-center justify-center mb-4
                  {smsSent ? 'bg-orange-50' : 'bg-green-50'}">
        <svg class="w-8 h-8 {smsSent ? 'text-orange-500' : 'text-green-500'}" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
        </svg>
      </div>
      <p class="text-gray-900 font-bold text-lg mb-1">민원이 접수되었습니다</p>
      <p class="text-sm text-gray-400 mb-6">
        {#if smsSent}앱 접수 완료. {smsInfo.phoneOnly ? '전화' : '문자'} 전송 화면이 열렸어요.{:else}내 민원 목록에서 진행상황을 확인할 수 있어요{/if}
      </p>
      <div class="flex gap-3">
        <a href="/complaints/my" class="bg-blue-600 text-white text-sm font-bold px-6 py-3 rounded-2xl active:bg-blue-700 transition-colors">
          내 민원 보기
        </a>
        <button
          onclick={() => { success = false; smsSent = false; }}
          class="bg-gray-100 text-gray-700 text-sm font-bold px-6 py-3 rounded-2xl active:bg-gray-200 transition-colors"
        >
          추가 접수
        </button>
      </div>
    </div>

  {:else}
    {#if error}
      <div class="bg-red-50 rounded-2xl px-4 py-3 text-sm text-red-500">{error}</div>
    {/if}

    <!-- 열차 정보 카드 (노선도에서 넘어온 경우) -->
    {#if hasTrainInfo}
      <div class="rounded-2xl px-4 py-3.5" style="background: #fff7ed; border: 1.5px solid #fed7aa;">
        <div class="flex items-center gap-2 mb-1.5">
          <span class="text-xs font-bold px-2.5 py-0.5 rounded-full text-white" style="background: {lineColor};">
            {lineName}
          </span>
          <span class="text-sm font-bold text-gray-800">{destination || direction}</span>
        </div>
        <div class="flex items-center gap-3 text-xs text-gray-500">
          <span>🚉 {trainStation}역</span>
          <span>🚇 운행ID {trainNo}</span>
          {#if direction}<span>→ {direction}</span>{/if}
        </div>
      </div>
    {/if}

    <!-- 민원 유형 선택 -->
    <section>
      <h2 class="text-[15px] font-bold text-gray-900 mb-3">
        민원 유형
        {#if category}<span class="text-blue-600 font-semibold ml-1">✓</span>{/if}
      </h2>
      <div class="grid grid-cols-4 gap-2">
        {#each CATEGORIES as cat}
          {@const selected = category === cat.id}
          <button
            onclick={() => category = selected ? '' : cat.id}
            class="flex flex-col items-center justify-center py-4 rounded-2xl transition-all active:scale-95"
            style="background-color: {selected ? '#EFF6FF' : '#F9FAFB'}; border: 2px solid {selected ? '#3B82F6' : 'transparent'};"
          >
            <svg class="w-6 h-6 mb-1.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.8"
                 style="color: {selected ? '#2563EB' : '#9CA3AF'};">
              {@html cat.icon}
            </svg>
            <span class="text-[10px] font-semibold leading-tight text-center"
                  style="color: {selected ? '#2563EB' : '#6B7280'};">{cat.label}</span>
          </button>
        {/each}
      </div>
    </section>

    <!-- 해당 역 -->
    <section>
      <h2 class="text-[15px] font-bold text-gray-900 mb-3">🚉 해당 역</h2>
      <StationSearch bind:value={stationName} placeholder="역 이름 입력 (예: 강남)" />
    </section>

    <!-- 열차 정보 (직접 입력 시 — 노선도에서 넘어오지 않은 경우) -->
    {#if !hasTrainInfo}
      <section>
        <h2 class="text-[15px] font-bold text-gray-900 mb-1">🚇 열차 정보</h2>
        <p class="text-[11px] text-gray-400 mb-3">선택사항 — 빠른 처리를 위해 입력해주세요</p>

        <!-- 노선 선택 -->
        <div class="mb-3">
          <select
            bind:value={manualLineCode}
            class="w-full bg-gray-100 rounded-2xl px-4 py-3.5 text-[14px] text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 appearance-none"
          >
            <option value="">노선 선택</option>
            {#each ALL_LINES as line}
              <option value={line.code}>{line.name}</option>
            {/each}
          </select>
        </div>

        <!-- 실시간 도착 예정 열차 (노선 선택 시) -->
        {#if manualLineCode}
          <div class="mb-3">
            <div class="flex gap-2 mb-1.5">
              <select
                bind:value={selectedTrainId}
                disabled={loadingTrains}
                class="flex-1 bg-gray-100 rounded-2xl px-4 py-3.5 text-[14px] text-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500 appearance-none disabled:opacity-60"
              >
                {#if loadingTrains}
                  <option value="">불러오는 중...</option>
                {:else if arrivingTrains.length === 0}
                  <option value="">열차를 불러오세요</option>
                {:else}
                  <option value="">열차 선택 (선택사항)</option>
                  {#each arrivingTrains as t}
                    <option value={t.trainNo}>{t.trainNo} — {t.destination ?? t.direction ?? ''}</option>
                  {/each}
                {/if}
              </select>
              <button
                onclick={loadArrivingTrains}
                disabled={loadingTrains}
                class="px-4 py-3.5 bg-blue-50 text-blue-600 text-sm font-semibold rounded-2xl active:bg-blue-100 transition-colors disabled:opacity-50 flex-shrink-0"
              >
                {#if loadingTrains}
                  <svg class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"></path>
                  </svg>
                {:else}
                  불러오기
                {/if}
              </button>
            </div>
            {#if arrivingTrains.length > 0}
              <p class="text-[11px] text-gray-400 px-1">
                {stationName.trim() ? `${stationName}역 도착 예정` : '전체'} 열차 {arrivingTrains.length}대
              </p>
            {/if}
          </div>
        {/if}

        <!-- 직접 입력 (편성번호) -->
        <input
          bind:value={manualTrainNo}
          placeholder="편성번호 직접 입력 (예: 1234) — 선택사항"
          class="w-full bg-gray-100 rounded-2xl px-4 py-3.5 text-[14px] text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </section>
    {/if}

    <!-- 탑승 위치 (칸/문 번호) -->
    <section>
      <h2 class="text-[15px] font-bold text-gray-900 mb-1">🚃 탑승 위치</h2>
      <p class="text-[11px] text-gray-400 mb-3">선택사항 — 정확한 위치 전달에 도움이 됩니다</p>
      <input
        bind:value={carNo}
        placeholder="예: 3번칸, 3-2번 문, 약냉방칸, 맨 앞칸"
        class="w-full bg-gray-100 rounded-2xl px-4 py-3.5 text-[14px] text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
      />
    </section>

    <!-- 내용 -->
    <section>
      <h2 class="text-[15px] font-bold text-gray-900 mb-3">상세 내용</h2>
      <textarea
        bind:value={content}
        placeholder="불편사항을 자세하게 입력해주세요"
        rows="5"
        class="w-full bg-gray-100 rounded-2xl px-4 py-4 text-[15px] text-gray-900 placeholder-gray-400
               focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors resize-none"
      ></textarea>
    </section>

    <!-- 운영사 문자 안내 (노선 선택 시) -->
    {#if effectiveLineCode}
      <div class="rounded-2xl px-4 py-3" style="background: #f0f9ff; border: 1px solid #bae6fd;">
        <div class="flex items-center justify-between mb-0.5">
          <span class="text-[13px] font-semibold text-blue-800">📱 {smsInfo.operator}</span>
          <span class="text-[13px] font-bold text-blue-700">{smsInfo.number}</span>
        </div>
        {#if smsInfo.note}
          <p class="text-[11px] text-blue-500">※ {smsInfo.note}</p>
        {/if}
        {#if smsInfo.phoneOnly}
          <p class="text-[11px] text-orange-500 mt-0.5">⚠️ 이 노선은 문자 접수 불가 · 전화만 가능</p>
        {/if}
      </div>
    {/if}

    <!-- 제출 버튼 -->
    <div class="flex gap-3">
      <button
        onclick={submitApp}
        disabled={!canSubmit}
        class="flex-1 bg-blue-600 text-white text-[14px] font-bold py-4 rounded-2xl
               disabled:opacity-40 active:bg-blue-700 transition-colors">
        {loading ? '접수 중...' : '앱 내 접수'}
      </button>
      <button
        onclick={submitWithSms}
        disabled={!canSubmit}
        class="flex-1 text-[14px] font-bold py-4 rounded-2xl transition-colors
               disabled:opacity-40 active:opacity-80"
        style="background: {smsInfo.phoneOnly ? '#f0fdf4' : '#fff7ed'}; color: {smsInfo.phoneOnly ? '#16a34a' : '#ea580c'}; border: 1.5px solid {smsInfo.phoneOnly ? '#bbf7d0' : '#fed7aa'};">
        {#if loading}
          접수 중...
        {:else if smsInfo.phoneOnly}
          앱 + 전화 ({smsInfo.operator})
        {:else}
          앱 + 문자 ({smsInfo.number})
        {/if}
      </button>
    </div>

    <!-- SMS/전화 안내 -->
    <p class="text-[11px] text-gray-400 text-center -mt-3">
      {#if smsInfo.phoneOnly}
        '앱 + 전화'는 {smsInfo.operator} 고객센터 전화 앱을 열어드려요.
      {:else}
        '앱 + 문자'는 {smsInfo.operator}({smsInfo.number})로 문자 앱을 열어드려요. 내용 확인 후 직접 전송하세요.
      {/if}
    </p>
  {/if}
</div>
