<script>
  import { api } from '$lib/api';
  import { token } from '$lib/stores';
  import { goto } from '$app/navigation';
  import { onMount } from 'svelte';

  let complaints  = $state([]);
  let loading     = $state(true);
  let error       = $state('');
  let deletingId  = $state(null);

  const STATUS = {
    'RECEIVED':    { label: '접수완료', bg: '#EFF6FF', text: '#2563EB' },
    'IN_PROGRESS': { label: '처리중',   bg: '#FFFBEB', text: '#D97706' },
    'COMPLETED':   { label: '처리완료', bg: '#F0FDF4', text: '#16A34A' },
    'REJECTED':    { label: '반려',     bg: '#FEF2F2', text: '#DC2626' },
  };

  onMount(async () => {
    if (!$token) { goto('/auth/login'); return; }
    try {
      complaints = await api.myComplaints($token);
    } catch (e) {
      error = e.message;
    } finally {
      loading = false;
    }
  });

  function formatDate(dt) {
    return new Date(dt).toLocaleDateString('ko-KR', {
      year: 'numeric', month: 'short', day: 'numeric',
    });
  }

  async function deleteComplaint(id) {
    if (!confirm('이 민원을 삭제하시겠어요?')) return;
    deletingId = id;
    try {
      await api.deleteComplaint(id, $token);
      complaints = complaints.filter(c => c.id !== id);
    } catch (e) {
      error = e.message;
    } finally {
      deletingId = null;
    }
  }
</script>

<!-- 헤더 -->
<header class="px-5 pt-12 pb-4 sticky top-0 z-40" style="background: #dbeafe; border-bottom: 1px solid #93c5fd;">
  <div class="flex items-center gap-3">
    <a href="/complaints" class="w-9 h-9 flex items-center justify-center rounded-full bg-gray-100 active:bg-gray-200 transition-colors flex-shrink-0">
      <svg class="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
        <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
      </svg>
    </a>
    <h1 class="text-[17px] font-bold text-gray-900">내 민원 목록</h1>
  </div>
</header>

<div class="px-4 pt-4 pb-4">
  {#if error}
    <div class="bg-red-50 rounded-2xl px-4 py-3 text-sm text-red-500 mb-4">{error}</div>
  {/if}

  {#if loading}
    <div class="space-y-3">
      {#each [1,2,3] as _}
        <div class="bg-white rounded-2xl p-4 shadow-sm">
          <div class="flex justify-between mb-3">
            <div class="h-4 bg-gray-100 rounded-lg w-1/3 animate-pulse"></div>
            <div class="h-5 bg-gray-100 rounded-full w-16 animate-pulse"></div>
          </div>
          <div class="h-3 bg-gray-100 rounded-lg w-5/6 mb-2 animate-pulse"></div>
          <div class="h-3 bg-gray-100 rounded-lg w-1/4 animate-pulse"></div>
        </div>
      {/each}
    </div>

  {:else if complaints.length === 0}
    <div class="flex flex-col items-center justify-center pt-16 pb-8 text-center">
      <div class="w-14 h-14 bg-gray-100 rounded-full flex items-center justify-center mb-4">
        <svg class="w-7 h-7 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
        </svg>
      </div>
      <p class="text-gray-700 font-semibold mb-1">접수한 민원이 없어요</p>
      <p class="text-sm text-gray-400 mb-5">불편사항을 바로 접수해보세요</p>
      <a href="/complaints" class="bg-blue-600 text-white text-sm font-bold px-6 py-3 rounded-2xl active:bg-blue-700 transition-colors">
        민원 접수하기
      </a>
    </div>

  {:else}
    <div class="space-y-3">
      {#each complaints as c}
        {@const st = STATUS[c.status] ?? { label: c.status, bg: '#F3F4F6', text: '#6B7280' }}
        <div class="bg-white rounded-2xl shadow-sm p-4">
          <div class="flex items-start justify-between mb-2">
            <div>
              <p class="font-bold text-gray-900 text-[15px]">{c.category}</p>
              <p class="text-xs text-gray-400 mt-0.5">{c.stationName}</p>
            </div>
            <span
              class="flex-shrink-0 text-xs font-bold px-3 py-1 rounded-full"
              style="background-color: {st.bg}; color: {st.text};"
            >{st.label}</span>
          </div>
          <p class="text-sm text-gray-600 leading-relaxed line-clamp-2">{c.content}</p>
          <div class="flex items-center justify-between mt-2">
            <p class="text-[11px] text-gray-400">{formatDate(c.createdAt)}</p>
            <button
              onclick={() => deleteComplaint(c.id)}
              disabled={deletingId === c.id}
              class="w-7 h-7 flex items-center justify-center rounded-full text-gray-400 hover:bg-red-50 hover:text-red-500 active:bg-red-100 transition-colors disabled:opacity-40"
              aria-label="삭제"
            >
              {#if deletingId === c.id}
                <svg class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"></path>
                </svg>
              {:else}
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              {/if}
            </button>
          </div>
        </div>
      {/each}
    </div>
  {/if}
</div>
