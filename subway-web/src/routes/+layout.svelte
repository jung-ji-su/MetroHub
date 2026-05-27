<script>
  import './layout.css';
  import { onMount } from 'svelte';
  import { user, auth, token } from '$lib/stores';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';
  import { connectSSE, disconnectSSE } from '$lib/sseStore';
  import {
    connectNotificationSSE,
    disconnectNotificationSSE,
    notifications,
    unreadCount,
    markAllRead,
  } from '$lib/notificationStore';

  let showNotifPanel = $state(false);

  onMount(() => {
    connectSSE();
    if ($token) connectNotificationSSE($token);
    return () => {
      disconnectSSE();
      disconnectNotificationSSE();
    };
  });

  // 로그인/로그아웃 시 알림 SSE 재연결
  $effect(() => {
    const t = $token;
    if (t) {
      connectNotificationSSE(t);
    } else {
      disconnectNotificationSSE();
    }
  });

  let { children } = $props();

  function logout() {
    auth.logout();
    showNotifPanel = false;
    goto('/');
  }

  function toggleNotifPanel() {
    showNotifPanel = !showNotifPanel;
    if (showNotifPanel) markAllRead();
  }

  const tabs = [
    { href: '/',           label: '홈'      },
    { href: '/community',  label: '커뮤니티' },
    { href: '/complaints', label: '민원'    },
    { href: '/my',         label: 'MY'     },
  ];

  function isActive(href) {
    if (href === '/') return $page.url.pathname === '/';
    return $page.url.pathname.startsWith(href);
  }

  const isAuthPage = $derived($page.url.pathname.startsWith('/auth'));

  function formatNotifTime(iso) {
    if (!iso) return '';
    const d = new Date(iso);
    return d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
  }
</script>

<div class="max-w-[430px] mx-auto bg-gray-50 min-h-screen relative">
  <main class:pb-[72px]={!isAuthPage}>
    {@render children()}
  </main>

  <!-- 벨 아이콘 (로그인 시, 인증 페이지 제외) -->
  {#if $user && !isAuthPage}
    <button
      onclick={toggleNotifPanel}
      class="fixed top-[52px] right-[calc(50%-215px+16px)] z-50 w-9 h-9 flex items-center justify-center rounded-full bg-white shadow-sm active:bg-gray-50 transition-colors"
      style="max-right: calc(50% + 215px - 16px);"
    >
      <div class="relative">
        <svg class="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round"
            d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0" />
        </svg>
        {#if $unreadCount > 0}
          <span class="absolute -top-1 -right-1 w-4 h-4 bg-red-500 text-white text-[9px] font-bold rounded-full flex items-center justify-center leading-none">
            {$unreadCount > 9 ? '9+' : $unreadCount}
          </span>
        {/if}
      </div>
    </button>

    <!-- 알림 패널 -->
    {#if showNotifPanel}
      <!-- 백드롭 -->
      <button
        onclick={() => showNotifPanel = false}
        class="fixed inset-0 z-40 bg-black/20"
        aria-label="닫기"
      ></button>

      <!-- 패널 -->
      <div class="fixed top-[86px] left-1/2 -translate-x-1/2 w-full max-w-[430px] z-50 px-3">
        <div class="bg-white rounded-2xl shadow-xl overflow-hidden" style="max-height: 65vh;">
          <!-- 패널 헤더 -->
          <div class="flex items-center justify-between px-4 py-3 border-b border-gray-100">
            <span class="text-[15px] font-bold text-gray-900">알림</span>
            <button
              onclick={() => showNotifPanel = false}
              class="w-7 h-7 flex items-center justify-center rounded-full bg-gray-100 active:bg-gray-200"
            >
              <svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          <!-- 알림 목록 -->
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
  {/if}

  <!-- 하단 탭바 -->
  {#if !isAuthPage}
  <nav class="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[430px] bg-white z-50"
       style="border-top: 1px solid #f3f4f6; padding-bottom: env(safe-area-inset-bottom);">
    <div class="flex">
      {#each tabs as tab}
        {@const active = isActive(tab.href)}
        <a href={tab.href}
           class="flex-1 flex flex-col items-center justify-center gap-[3px] h-[56px] transition-colors
                  {active ? 'text-blue-600' : 'text-gray-400'}"
        >
          {#if tab.label === '홈'}
            <svg class="w-[22px] h-[22px]" fill="none" stroke="currentColor" viewBox="0 0 24 24"
                 stroke-width={active ? 2.5 : 1.8}>
              <path stroke-linecap="round" stroke-linejoin="round"
                d="M3 12l9-9 9 9M5 10v9a1 1 0 001 1h4v-5h4v5h4a1 1 0 001-1v-9" />
            </svg>
          {:else if tab.label === '커뮤니티'}
            <svg class="w-[22px] h-[22px]" fill="none" stroke="currentColor" viewBox="0 0 24 24"
                 stroke-width={active ? 2.5 : 1.8}>
              <path stroke-linecap="round" stroke-linejoin="round"
                d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
          {:else if tab.label === '민원'}
            <svg class="w-[22px] h-[22px]" fill="none" stroke="currentColor" viewBox="0 0 24 24"
                 stroke-width={active ? 2.5 : 1.8}>
              <path stroke-linecap="round" stroke-linejoin="round"
                d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
            </svg>
          {:else}
            <svg class="w-[22px] h-[22px]" fill="none" stroke="currentColor" viewBox="0 0 24 24"
                 stroke-width={active ? 2.5 : 1.8}>
              <path stroke-linecap="round" stroke-linejoin="round"
                d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
            </svg>
          {/if}
          <span class="text-[10px] font-semibold leading-none">{tab.label}</span>
        </a>
      {/each}
    </div>
  </nav>
  {/if}
</div>
