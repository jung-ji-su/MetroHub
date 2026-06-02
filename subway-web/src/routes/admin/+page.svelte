<script>
  import { api } from '$lib/api';
  import { token, user } from '$lib/stores';
  import { goto } from '$app/navigation';
  import { onMount } from 'svelte';

  // ── 탭 ──────────────────────────────────────────────
  let activeTab = $state('dashboard'); // 'dashboard' | 'complaints'

  // ── 대시보드 ────────────────────────────────────────
  let stats   = $state(null);
  let statsLoading = $state(true);
  let statsError   = $state('');

  // ── 민원 관리 ───────────────────────────────────────
  let complaints   = $state([]);
  let total        = $state(0);
  let cLoading     = $state(false);
  let cError       = $state('');
  let filterStatus = $state('');
  let page         = $state(0);
  const size = 20;

  const STATUS_OPTIONS = ['', 'RECEIVED', 'IN_PROGRESS', 'COMPLETED', 'REJECTED'];
  const STATUS = {
    'RECEIVED':    { label: '접수완료', bg: '#EFF6FF', text: '#2563EB' },
    'IN_PROGRESS': { label: '처리중',   bg: '#FFFBEB', text: '#D97706' },
    'COMPLETED':   { label: '처리완료', bg: '#F0FDF4', text: '#16A34A' },
    'REJECTED':    { label: '반려',     bg: '#FEF2F2', text: '#DC2626' },
  };
  const NEXT_STATUS = { 'RECEIVED': 'IN_PROGRESS', 'IN_PROGRESS': 'COMPLETED' };

  const C_STATUS_LABELS = {
    RECEIVED: '접수', IN_PROGRESS: '처리중', COMPLETED: '완료', REJECTED: '반려'
  };

  function lineLabel(n) {
    if (/^\d+$/.test(n)) return n + '호선';
    return n + '선';
  }

  function formatDate(dt) {
    if (!dt) return '';
    return new Date(dt).toLocaleDateString('ko-KR', {
      month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }

  // ── 대시보드 로드 ───────────────────────────────────
  async function loadDashboard() {
    statsLoading = true; statsError = '';
    try {
      stats = await api.adminGetDashboard($token);
    } catch (e) {
      statsError = e.message;
    } finally {
      statsLoading = false;
    }
  }

  // ── 민원 로드 ───────────────────────────────────────
  async function loadComplaints() {
    cLoading = true; cError = '';
    try {
      const result = await api.adminGetComplaints($token, filterStatus, page, size);
      complaints = result.items;
      total = result.total;
    } catch (e) {
      cError = e.message;
    } finally {
      cLoading = false;
    }
  }

  async function changeStatus(id, newStatus) {
    try {
      const updated = await api.adminUpdateComplaintStatus(id, newStatus, $token);
      complaints = complaints.map(c => c.id === id ? updated : c);
    } catch (e) {
      cError = e.message;
    }
  }

  function switchTab(tab) {
    activeTab = tab;
    if (tab === 'complaints' && complaints.length === 0) loadComplaints();
  }

  const totalPages = $derived(Math.ceil(total / size));

  // ── 민원 상태별 집계 (대시보드용) ───────────────────
  const complaintStatusCards = $derived(
    stats ? [
      { key: 'RECEIVED',    label: '접수',   color: '#2563EB', bg: '#EFF6FF' },
      { key: 'IN_PROGRESS', label: '처리중', color: '#D97706', bg: '#FFFBEB' },
      { key: 'COMPLETED',   label: '완료',   color: '#16A34A', bg: '#F0FDF4' },
      { key: 'REJECTED',    label: '반려',   color: '#DC2626', bg: '#FEF2F2' },
    ].map(s => ({ ...s, count: stats.complaintsByStatus?.[s.key] ?? 0 }))
    : []
  );

  // 호선별 게시글 바 차트 (최대값 기준 %)
  const maxLineCount = $derived(
    stats?.postsByLine?.length ? Math.max(...stats.postsByLine.map(l => l.count)) : 1
  );

  onMount(async () => {
    if (!$token) { goto('/auth/login'); return; }
    if ($user?.role !== 'ADMIN') { goto('/'); return; }
    await loadDashboard();
  });
</script>

<!-- 헤더 -->
<header class="px-5 pt-12 pb-0 sticky top-0 z-40" style="background: #1e1b4b; border-bottom: 1px solid #3730a3;">
  <div class="flex items-center gap-3 pb-3">
    <div class="w-8 h-8 rounded-full bg-white/10 flex items-center justify-center flex-shrink-0">
      <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round"
          d="M10.343 3.94c.09-.542.56-.94 1.11-.94h1.093c.55 0 1.02.398 1.11.94l.149.894c.07.424.384.764.78.93.398.164.855.142 1.205-.108l.737-.527a1.125 1.125 0 011.45.12l.773.774c.39.389.44 1.002.12 1.45l-.527.737c-.25.35-.272.806-.107 1.204.165.397.505.71.93.78l.893.15c.543.09.94.56.94 1.109v1.094c0 .55-.397 1.02-.94 1.11l-.893.149c-.425.07-.765.383-.93.78-.165.398-.143.854.107 1.204l.527.738c.32.447.269 1.06-.12 1.45l-.774.773a1.125 1.125 0 01-1.449.12l-.738-.527c-.35-.25-.806-.272-1.203-.107-.397.165-.71.505-.781.929l-.149.894c-.09.542-.56.94-1.11.94h-1.094c-.55 0-1.019-.398-1.11-.94l-.148-.894c-.071-.424-.384-.764-.781-.93-.398-.164-.854-.142-1.204.108l-.738.527c-.447.32-1.06.269-1.45-.12l-.773-.774a1.125 1.125 0 01-.12-1.45l.527-.737c.25-.35.273-.806.108-1.204-.165-.397-.505-.71-.93-.78l-.894-.15c-.542-.09-.94-.56-.94-1.109v-1.094c0-.55.398-1.02.94-1.11l.894-.149c.424-.07.765-.383.93-.78.165-.398.143-.854-.107-1.204l-.527-.738a1.125 1.125 0 01.12-1.45l.773-.773a1.125 1.125 0 011.45-.12l.737.527c.35.25.807.272 1.204.107.397-.165.71-.505.78-.929l.15-.894z" />
        <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
      </svg>
    </div>
    <div>
      <h1 class="text-[17px] font-bold text-white">관리자</h1>
      <p class="text-xs text-indigo-300">MetroHub Admin</p>
    </div>
  </div>

  <!-- 탭 -->
  <div class="flex">
    <button
      onclick={() => switchTab('dashboard')}
      class="flex-1 py-2.5 text-sm font-semibold transition-colors border-b-2
             {activeTab === 'dashboard' ? 'text-white border-white' : 'text-indigo-300 border-transparent'}"
    >대시보드</button>
    <button
      onclick={() => switchTab('complaints')}
      class="flex-1 py-2.5 text-sm font-semibold transition-colors border-b-2
             {activeTab === 'complaints' ? 'text-white border-white' : 'text-indigo-300 border-transparent'}"
    >민원 관리</button>
  </div>
</header>

<!-- ── 대시보드 탭 ─────────────────────────────────── -->
{#if activeTab === 'dashboard'}
<div class="px-4 pt-5 pb-24 space-y-5">

  {#if statsError}
    <div class="bg-red-50 rounded-2xl px-4 py-3 text-sm text-red-600">{statsError}</div>
  {/if}

  {#if statsLoading}
    <div class="grid grid-cols-2 gap-3">
      {#each [1,2,3,4] as _}
        <div class="bg-white rounded-2xl p-4 shadow-sm animate-pulse h-20"></div>
      {/each}
    </div>

  {:else if stats}
    <!-- 핵심 수치 4개 -->
    <div class="grid grid-cols-2 gap-3">
      {#each [
        { label: '총 사용자', value: stats.totalUsers, sub: `오늘 +${stats.todayNewUsers}`, color: '#2563EB', icon: 'M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z' },
        { label: '총 게시글', value: stats.totalPosts, sub: `오늘 +${stats.todayNewPosts}`, color: '#7C3AED', icon: 'M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z' },
        { label: '총 댓글',   value: stats.totalComments, sub: '누적', color: '#0891B2', icon: 'M7.5 8.25h9m-9 3H12m-9.75 1.51c0 1.6 1.123 2.994 2.707 3.227 1.129.166 2.27.293 3.423.379.35.026.67.21.865.501L12 21l2.755-4.133a1.14 1.14 0 01.865-.501 48.172 48.172 0 003.423-.379c1.584-.233 2.707-1.626 2.707-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0012 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018z' },
        { label: '총 민원',   value: stats.totalComplaints, sub: '누적', color: '#DC2626', icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4' },
      ] as card}
        <div class="bg-white rounded-2xl p-4 shadow-sm">
          <div class="flex items-center gap-2 mb-2">
            <div class="w-7 h-7 rounded-xl flex items-center justify-center" style="background: {card.color}18;">
              <svg class="w-4 h-4" style="color: {card.color}" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.8">
                <path stroke-linecap="round" stroke-linejoin="round" d={card.icon} />
              </svg>
            </div>
            <span class="text-xs text-gray-500 font-medium">{card.label}</span>
          </div>
          <p class="text-2xl font-bold text-gray-900">{card.value.toLocaleString()}</p>
          <p class="text-[11px] text-gray-400 mt-0.5">{card.sub}</p>
        </div>
      {/each}
    </div>

    <!-- 민원 상태별 현황 -->
    <div class="bg-white rounded-2xl shadow-sm p-4">
      <h2 class="text-sm font-bold text-gray-700 mb-3">민원 상태별 현황</h2>
      <div class="grid grid-cols-4 gap-2">
        {#each complaintStatusCards as s}
          <div class="rounded-xl p-2.5 text-center" style="background: {s.bg};">
            <p class="text-lg font-bold" style="color: {s.color};">{s.count}</p>
            <p class="text-[10px] font-semibold mt-0.5" style="color: {s.color};">{s.label}</p>
          </div>
        {/each}
      </div>
    </div>

    <!-- 호선별 게시글 -->
    {#if stats.postsByLine?.length}
    <div class="bg-white rounded-2xl shadow-sm p-4">
      <h2 class="text-sm font-bold text-gray-700 mb-3">호선별 게시글 TOP {stats.postsByLine.length}</h2>
      <div class="space-y-2">
        {#each stats.postsByLine as line}
          {@const pct = Math.round((line.count / maxLineCount) * 100)}
          <div class="flex items-center gap-2">
            <span class="text-xs text-gray-600 font-medium w-16 flex-shrink-0">{lineLabel(line.lineNumber)}</span>
            <div class="flex-1 h-5 bg-gray-100 rounded-full overflow-hidden">
              <div class="h-full rounded-full bg-indigo-500 transition-all" style="width: {pct}%;"></div>
            </div>
            <span class="text-xs text-gray-500 font-bold w-8 text-right">{line.count}</span>
          </div>
        {/each}
      </div>
    </div>
    {/if}

    <!-- 최근 가입자 -->
    <div class="bg-white rounded-2xl shadow-sm p-4">
      <h2 class="text-sm font-bold text-gray-700 mb-3">최근 가입자</h2>
      {#if stats.recentUsers?.length}
        <div class="space-y-2">
          {#each stats.recentUsers as u}
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2.5">
                <div class="w-7 h-7 rounded-full bg-indigo-100 flex items-center justify-center">
                  <span class="text-xs font-bold text-indigo-600">{u.nickname.slice(0,1).toUpperCase()}</span>
                </div>
                <span class="text-sm font-medium text-gray-800">{u.nickname}</span>
              </div>
              <span class="text-[11px] text-gray-400">{formatDate(u.createdAt)}</span>
            </div>
          {/each}
        </div>
      {:else}
        <p class="text-sm text-gray-400 text-center py-2">가입자가 없습니다</p>
      {/if}
    </div>

    <!-- 최근 민원 -->
    <div class="bg-white rounded-2xl shadow-sm p-4">
      <div class="flex items-center justify-between mb-3">
        <h2 class="text-sm font-bold text-gray-700">최근 민원</h2>
        <button
          onclick={() => switchTab('complaints')}
          class="text-xs text-indigo-600 font-semibold"
        >전체 보기</button>
      </div>
      {#if stats.recentComplaints?.length}
        <div class="space-y-2">
          {#each stats.recentComplaints as c}
            {@const st = STATUS[c.status] ?? { label: c.status, bg: '#F3F4F6', text: '#6B7280' }}
            <div class="flex items-center justify-between py-1">
              <div class="flex-1 min-w-0 mr-2">
                <p class="text-sm font-medium text-gray-800 truncate">{c.category} · {c.stationName}</p>
                <p class="text-[11px] text-gray-400">{formatDate(c.createdAt)}</p>
              </div>
              <span class="text-[11px] font-bold px-2.5 py-1 rounded-full flex-shrink-0"
                    style="background: {st.bg}; color: {st.text};">{st.label}</span>
            </div>
          {/each}
        </div>
      {:else}
        <p class="text-sm text-gray-400 text-center py-2">민원이 없습니다</p>
      {/if}
    </div>

    <!-- 새로고침 -->
    <button
      onclick={loadDashboard}
      class="w-full py-3 rounded-2xl bg-indigo-50 text-indigo-600 text-sm font-semibold active:bg-indigo-100 transition-colors"
    >새로고침</button>
  {/if}
</div>
{/if}

<!-- ── 민원 관리 탭 ─────────────────────────────────── -->
{#if activeTab === 'complaints'}
<div class="px-4 pt-4 pb-24">

  <!-- 필터 -->
  <div class="flex gap-2 mb-4 flex-wrap">
    {#each STATUS_OPTIONS as s}
      <button
        onclick={() => { filterStatus = s; page = 0; loadComplaints(); }}
        class="px-3 py-1.5 rounded-full text-xs font-bold border transition-colors"
        class:bg-indigo-600={filterStatus === s}
        class:text-white={filterStatus === s}
        class:border-indigo-600={filterStatus === s}
        class:bg-white={filterStatus !== s}
        class:text-gray-600={filterStatus !== s}
        class:border-gray-200={filterStatus !== s}
      >
        {s === '' ? `전체 (${total})` : (STATUS[s]?.label ?? s)}
      </button>
    {/each}
  </div>

  {#if cError}
    <div class="bg-red-50 rounded-2xl px-4 py-3 text-sm text-red-600 mb-4">{cError}</div>
  {/if}

  {#if cLoading}
    <div class="space-y-3">
      {#each [1,2,3,4] as _}
        <div class="bg-white rounded-2xl p-4 shadow-sm animate-pulse h-24"></div>
      {/each}
    </div>

  {:else if complaints.length === 0}
    <div class="flex flex-col items-center justify-center pt-16 text-center">
      <p class="text-gray-500 font-medium">해당 조건의 민원이 없습니다</p>
    </div>

  {:else}
    <div class="space-y-3">
      {#each complaints as c}
        {@const st = STATUS[c.status] ?? { label: c.status, bg: '#F3F4F6', text: '#6B7280' }}
        {@const next = NEXT_STATUS[c.status]}
        <div class="bg-white rounded-2xl shadow-sm p-4">
          <div class="flex items-start justify-between mb-2">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-0.5">
                <p class="font-bold text-gray-900 text-[15px]">{c.category}</p>
                <span class="text-xs text-gray-400">#{c.id}</span>
              </div>
              <p class="text-xs text-gray-500">{c.stationName}</p>
            </div>
            <span class="flex-shrink-0 text-xs font-bold px-3 py-1 rounded-full ml-2"
                  style="background: {st.bg}; color: {st.text};">{st.label}</span>
          </div>
          <p class="text-sm text-gray-600 leading-relaxed mb-3 line-clamp-3">{c.content}</p>
          <div class="flex items-center justify-between">
            <p class="text-[11px] text-gray-400">{formatDate(c.createdAt)}</p>
            {#if next}
              <button
                onclick={() => changeStatus(c.id, next)}
                class="text-xs font-bold px-3 py-1.5 rounded-xl bg-indigo-600 text-white active:bg-indigo-700 transition-colors"
              >{STATUS[next]?.label ?? next}으로 변경</button>
            {:else if c.status !== 'REJECTED'}
              <button
                onclick={() => changeStatus(c.id, 'REJECTED')}
                class="text-xs font-bold px-3 py-1.5 rounded-xl bg-red-50 text-red-600 active:bg-red-100 transition-colors"
              >반려</button>
            {/if}
          </div>
        </div>
      {/each}
    </div>

    {#if totalPages > 1}
      <div class="flex items-center justify-center gap-3 mt-6">
        <button
          onclick={() => { page = Math.max(0, page - 1); loadComplaints(); }}
          disabled={page === 0}
          class="w-9 h-9 flex items-center justify-center rounded-full bg-white shadow-sm disabled:opacity-30"
        >
          <svg class="w-4 h-4 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
          </svg>
        </button>
        <span class="text-sm text-gray-600 font-medium">{page + 1} / {totalPages}</span>
        <button
          onclick={() => { page = Math.min(totalPages - 1, page + 1); loadComplaints(); }}
          disabled={page >= totalPages - 1}
          class="w-9 h-9 flex items-center justify-center rounded-full bg-white shadow-sm disabled:opacity-30"
        >
          <svg class="w-4 h-4 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
          </svg>
        </button>
      </div>
    {/if}
  {/if}
</div>
{/if}
