<script>
  import { api } from '$lib/api';
  import { token, user } from '$lib/stores';
  import { goto } from '$app/navigation';
  import { onMount } from 'svelte';

  let complaints = $state([]);
  let loading    = $state(true);
  let error      = $state('');

  const STATUS_MAP = {
    'RECEIVED':    { label: '접수완료', cls: 'bg-blue-100 text-blue-700'   },
    'IN_PROGRESS': { label: '처리중',   cls: 'bg-yellow-100 text-yellow-700' },
    'COMPLETED':   { label: '처리완료', cls: 'bg-green-100 text-green-700'  },
    'REJECTED':    { label: '반려',     cls: 'bg-red-100 text-red-700'      },
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
    return new Date(dt).toLocaleDateString('ko-KR', { year: 'numeric', month: 'short', day: 'numeric' });
  }
</script>

<div class="max-w-3xl mx-auto px-4 py-8">
  <div class="flex items-center justify-between mb-6">
    <h1 class="text-2xl font-bold text-gray-800">내 민원 목록</h1>
    <a href="/complaints" class="text-sm bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg transition-colors">
      민원 접수
    </a>
  </div>

  {#if error}
    <div class="bg-red-50 border border-red-200 text-red-600 rounded-lg px-4 py-3 mb-4 text-sm">{error}</div>
  {/if}

  {#if loading}
    <div class="text-center py-12 text-gray-400">불러오는 중...</div>
  {:else if complaints.length === 0}
    <div class="text-center py-16 text-gray-400">
      <p class="text-lg mb-2">접수한 민원이 없습니다</p>
      <a href="/complaints" class="text-blue-600 hover:underline text-sm">민원 접수하러 가기</a>
    </div>
  {:else}
    <div class="space-y-3">
      {#each complaints as c}
        {@const status = STATUS_MAP[c.status] ?? { label: c.status, cls: 'bg-gray-100 text-gray-600' }}
        <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
          <div class="flex items-start justify-between mb-2">
            <div>
              <span class="font-medium text-gray-800">{c.category}</span>
              <span class="text-gray-400 mx-2">·</span>
              <span class="text-gray-600 text-sm">{c.stationName}</span>
            </div>
            <span class="text-xs px-2 py-0.5 rounded-full font-medium {status.cls}">{status.label}</span>
          </div>
          <p class="text-sm text-gray-600 line-clamp-2">{c.content}</p>
          <p class="text-xs text-gray-400 mt-2">{formatDate(c.createdAt)}</p>
        </div>
      {/each}
    </div>
  {/if}
</div>
