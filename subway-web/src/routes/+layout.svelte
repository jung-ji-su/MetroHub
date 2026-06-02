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
  } from '$lib/notificationStore';

  onMount(() => {
    connectSSE();
    if ($token) connectNotificationSSE($token);
    return () => {
      disconnectSSE();
      disconnectNotificationSSE();
    };
  });

  // 로그인/로그아웃 시 알림 SSE 재연결 — 기존 연결 먼저 정리 후 신규 연결
  $effect(() => {
    const t = $token;
    disconnectNotificationSSE();
    if (t) connectNotificationSSE(t);
  });

  let { children } = $props();

  function logout() {
    auth.logout();
    goto('/');
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


</script>

<div class="max-w-[430px] mx-auto bg-gray-50 min-h-screen relative">
  <main class:pb-[72px]={!isAuthPage}>
    {@render children()}
  </main>

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
