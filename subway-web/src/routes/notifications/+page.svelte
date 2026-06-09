<script>
  import { onMount } from 'svelte';
  import { token, user } from '$lib/stores';
  import { api } from '$lib/api';
  import { notifications, unreadCount, markAllRead } from '$lib/notificationStore';
  import { goto } from '$app/navigation';

  let apiNotifs = $state([]);
  let loading = $state(true);
  let page = $state(0);
  let hasMore = $state(true);
  let loadingMore = $state(false);

  function notifIcon(type) {
    if (type === 'CONGESTION_ALERT') return '🚨';
    if (type === 'LINE_ALERT')       return '🚇';
    if (type === 'STATION_ALERT')    return '📍';
    return '🔔';
  }

  function formatTime(iso) {
    if (!iso) return '';
    const d = new Date(iso);
    const now = new Date();
    const diffMs = now - d;
    const diffMin = Math.floor(diffMs / 60000);
    if (diffMin < 1)   return '방금';
    if (diffMin < 60)  return `${diffMin}분 전`;
    const diffH = Math.floor(diffMin / 60);
    if (diffH < 24)    return `${diffH}시간 전`;
    return d.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' });
  }

  async function loadNotifications(reset = false) {
    if (!$token) return;
    if (reset) { page = 0; hasMore = true; }
    if (!hasMore) return;

    if (reset) loading = true;
    else loadingMore = true;

    try {
      const data = await api.getMyNotifications($token, page);
      const items = Array.isArray(data?.content) ? data.content : (Array.isArray(data) ? data : []);
      if (reset) {
        apiNotifs = items;
      } else {
        apiNotifs = [...apiNotifs, ...items];
      }
      hasMore = items.length >= 20;
      page++;
    } catch (_) {
      hasMore = false;
    } finally {
      loading = false;
      loadingMore = false;
    }
  }

  // SSE 실시간 알림을 상단에 합쳐 보여주기 위해 merge
  const allNotifs = $derived.by(() => {
    const sseSet = new Set($notifications.map(n => n.id).filter(Boolean));
    const apiFiltered = apiNotifs.filter(n => !sseSet.has(n.id));
    return [...$notifications, ...apiFiltered];
  });

  onMount(async () => {
    if (!$token) return;
    await loadNotifications(true);
    markAllRead($token);
    unreadCount.set(0);
  });
</script>

<div class="min-h-screen bg-gray-50">
  <header class="px-5 pt-12 pb-4 sticky top-0 z-40"
          style="background: #ffffff; border-bottom: 1px solid #f3f4f6;">
    <div class="flex items-center gap-3">
      <a href="javascript:history.back()"
         class="w-9 h-9 flex items-center justify-center rounded-full bg-gray-100 active:bg-gray-200 flex-shrink-0">
        <svg class="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
        </svg>
      </a>
      <h1 class="text-[17px] font-bold text-gray-900 flex-1">알림</h1>
    </div>
  </header>

  <div class="px-4 py-4">
    {#if !$token}
      <div class="flex flex-col items-center justify-center pt-16 text-center">
        <div class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
          <svg class="w-8 h-8 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round"
              d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0" />
          </svg>
        </div>
        <p class="text-gray-700 font-semibold mb-1">로그인이 필요해요</p>
        <p class="text-sm text-gray-400 mb-6">로그인하면 알림을 받을 수 있어요</p>
        <a href="/auth/login"
           class="bg-blue-600 text-white text-sm font-bold px-6 py-3 rounded-2xl active:bg-blue-700 transition-colors">
          로그인
        </a>
      </div>

    {:else if loading}
      <div class="space-y-3">
        {#each [1,2,3,4,5] as _}
          <div class="bg-white rounded-2xl p-4 shadow-sm flex items-start gap-3">
            <div class="w-10 h-10 bg-gray-100 rounded-full animate-pulse flex-shrink-0"></div>
            <div class="flex-1 space-y-2">
              <div class="h-3.5 bg-gray-100 rounded-lg w-1/2 animate-pulse"></div>
              <div class="h-3 bg-gray-100 rounded-lg w-3/4 animate-pulse"></div>
              <div class="h-2.5 bg-gray-100 rounded-lg w-1/4 animate-pulse"></div>
            </div>
          </div>
        {/each}
      </div>

    {:else if allNotifs.length === 0}
      <div class="flex flex-col items-center justify-center pt-16 text-center">
        <div class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
          <svg class="w-8 h-8 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round"
              d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0" />
          </svg>
        </div>
        <p class="text-gray-600 font-semibold mb-1">아직 알림이 없어요</p>
        <p class="text-sm text-gray-400">혼잡도가 급등하면 알림을 보내드려요</p>
        <a href="/my" class="mt-4 text-sm font-bold text-blue-500 bg-blue-50 px-4 py-2 rounded-2xl active:bg-blue-100 transition-colors">
          알림 구독 설정하기
        </a>
      </div>

    {:else}
      <div class="space-y-2">
        {#each allNotifs as notif}
          <div class="bg-white rounded-2xl shadow-sm p-4 flex items-start gap-3">
            <div class="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 text-xl"
                 style="background: #EFF6FF;">
              {notifIcon(notif.type)}
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-[14px] font-bold text-gray-900 leading-snug">
                {notif.title ?? '알림'}
              </p>
              <p class="text-[12px] text-gray-500 mt-0.5 leading-relaxed">
                {notif.body ?? notif.message ?? ''}
              </p>
              <p class="text-[11px] text-gray-300 mt-1.5">{formatTime(notif.createdAt)}</p>
            </div>
          </div>
        {/each}
      </div>

      {#if hasMore}
        <button
          onclick={() => loadNotifications()}
          disabled={loadingMore}
          class="mt-4 w-full py-3.5 bg-white rounded-2xl shadow-sm text-[14px] font-bold text-gray-500 active:bg-gray-50 disabled:opacity-40 transition-colors"
        >
          {loadingMore ? '불러오는 중...' : '더 보기'}
        </button>
      {/if}
    {/if}
  </div>
</div>
