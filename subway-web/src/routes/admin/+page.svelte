<script>
  import { api } from '$lib/api';
  import { token } from '$lib/stores';
  import { goto } from '$app/navigation';
  import { onMount } from 'svelte';

  let complaints = $state([]);
  let total      = $state(0);
  let loading    = $state(true);
  let error      = $state('');
  let filterStatus = $state('');
  let page       = $state(0);
  const size = 20;

  const STATUS_OPTIONS = ['', 'RECEIVED', 'IN_PROGRESS', 'COMPLETED', 'REJECTED'];
  const STATUS = {
    'RECEIVED':    { label: '접수완료', bg: '#EFF6FF', text: '#2563EB' },
    'IN_PROGRESS': { label: '처리중',   bg: '#FFFBEB', text: '#D97706' },
    'COMPLETED':   { label: '처리완료', bg: '#F0FDF4', text: '#16A34A' },
    'REJECTED':    { label: '반려',     bg: '#FEF2F2', text: '#DC2626' },
  };
  const NEXT_STATUS = {
    'RECEIVED':    'IN_PROGRESS',
    'IN_PROGRESS': 'COMPLETED',
  };

  async function loadComplaints() {
    loading = true;
    error = '';
    try {
      const result = await api.adminGetComplaints($token, filterStatus, page, size);
      complaints = result.items;
      total = result.total;
    } catch (e) {
      if (e.message.includes('403') || e.message.includes('관리자')) {
        error = '관리자 권한이 없습니다.';
      } else if (e.message.includes('401')) {
        goto('/auth/login');
        return;
      } else {
        error = e.message;
      }
    } finally {
      loading = false;
    }
  }

  onMount(async () => {
    if (!$token) { goto('/auth/login'); return; }
    await loadComplaints();
  });

  async function changeStatus(id, newStatus) {
    try {
      const updated = await api.adminUpdateComplaintStatus(id, newStatus, $token);
      complaints = complaints.map(c => c.id === id ? updated : c);
    } catch (e) {
      error = e.message;
    }
  }

  async function applyFilter() {
    page = 0;
    await loadComplaints();
  }

  function formatDate(dt) {
    return new Date(dt).toLocaleDateString('ko-KR', {
      year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
    });
  }

  const totalPages = $derived(Math.ceil(total / size));
</script>

<header class="px-5 pt-12 pb-4 sticky top-0 z-40" style="background: #1e1b4b; border-bottom: 1px solid #3730a3;">
  <div class="flex items-center gap-3">
    <a href="/" class="w-9 h-9 flex items-center justify-center rounded-full bg-white/10 active:bg-white/20 transition-colors flex-shrink-0">
      <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
        <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
      </svg>
    </a>
    <div>
      <h1 class="text-[17px] font-bold text-white">관리자 — 민원 관리</h1>
      <p class="text-xs text-indigo-300">총 {total}건</p>
    </div>
  </div>
</header>

<div class="px-4 pt-4 pb-24">
  <!-- 필터 -->
  <div class="flex gap-2 mb-4 flex-wrap">
    {#each STATUS_OPTIONS as s}
      <button
        onclick={() => { filterStatus = s; applyFilter(); }}
        class="px-3 py-1.5 rounded-full text-xs font-bold border transition-colors"
        class:bg-indigo-600={filterStatus === s}
        class:text-white={filterStatus === s}
        class:border-indigo-600={filterStatus === s}
        class:bg-white={filterStatus !== s}
        class:text-gray-600={filterStatus !== s}
        class:border-gray-200={filterStatus !== s}
      >
        {s === '' ? '전체' : (STATUS[s]?.label ?? s)}
      </button>
    {/each}
  </div>

  {#if error}
    <div class="bg-red-50 rounded-2xl px-4 py-3 text-sm text-red-600 mb-4">{error}</div>
  {/if}

  {#if loading}
    <div class="space-y-3">
      {#each [1,2,3,4] as _}
        <div class="bg-white rounded-2xl p-4 shadow-sm animate-pulse">
          <div class="flex justify-between mb-3">
            <div class="h-4 bg-gray-100 rounded-lg w-1/3"></div>
            <div class="h-5 bg-gray-100 rounded-full w-16"></div>
          </div>
          <div class="h-3 bg-gray-100 rounded-lg w-5/6 mb-2"></div>
          <div class="h-3 bg-gray-100 rounded-lg w-1/4"></div>
        </div>
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
            <span
              class="flex-shrink-0 text-xs font-bold px-3 py-1 rounded-full ml-2"
              style="background-color: {st.bg}; color: {st.text};"
            >{st.label}</span>
          </div>

          <p class="text-sm text-gray-600 leading-relaxed mb-3 line-clamp-3">{c.content}</p>

          <div class="flex items-center justify-between">
            <p class="text-[11px] text-gray-400">{formatDate(c.createdAt)}</p>
            {#if next}
              <button
                onclick={() => changeStatus(c.id, next)}
                class="text-xs font-bold px-3 py-1.5 rounded-xl bg-indigo-600 text-white active:bg-indigo-700 transition-colors"
              >
                {STATUS[next]?.label ?? next}으로 변경
              </button>
            {:else if c.status !== 'REJECTED'}
              <button
                onclick={() => changeStatus(c.id, 'REJECTED')}
                class="text-xs font-bold px-3 py-1.5 rounded-xl bg-red-50 text-red-600 active:bg-red-100 transition-colors"
              >
                반려
              </button>
            {/if}
          </div>
        </div>
      {/each}
    </div>

    <!-- 페이지네이션 -->
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
