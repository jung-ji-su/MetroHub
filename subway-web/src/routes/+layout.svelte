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
  import Onboarding from '$lib/Onboarding.svelte';

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

  const baseTabs = [
    { href: '/',           label: '홈'      },
    { href: '/community',  label: '커뮤니티' },
    { href: '/complaints', label: '민원'    },
    { href: '/my',         label: 'MY'     },
  ];

  const tabs = $derived(
    $user?.nickname === 'dev'
      ? [...baseTabs, { href: '/admin', label: '관리' }]
      : baseTabs
  );

  function isActive(href) {
    if (href === '/') return $page.url.pathname === '/';
    return $page.url.pathname.startsWith(href);
  }

  const isAuthPage = $derived($page.url.pathname.startsWith('/auth'));


</script>

<Onboarding />

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
          {:else if tab.label === 'MY'}
            <svg class="w-[22px] h-[22px]" fill="none" stroke="currentColor" viewBox="0 0 24 24"
                 stroke-width={active ? 2.5 : 1.8}>
              <path stroke-linecap="round" stroke-linejoin="round"
                d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
            </svg>
          {:else}
            <svg class="w-[22px] h-[22px]" fill="none" stroke="currentColor" viewBox="0 0 24 24"
                 stroke-width={active ? 2.5 : 1.8}>
              <path stroke-linecap="round" stroke-linejoin="round"
                d="M10.343 3.94c.09-.542.56-.94 1.11-.94h1.093c.55 0 1.02.398 1.11.94l.149.894c.07.424.384.764.78.93.398.164.855.142 1.205-.108l.737-.527a1.125 1.125 0 011.45.12l.773.774c.39.389.44 1.002.12 1.45l-.527.737c-.25.35-.272.806-.107 1.204.165.397.505.71.93.78l.893.15c.543.09.94.56.94 1.109v1.094c0 .55-.397 1.02-.94 1.11l-.893.149c-.425.07-.765.383-.93.78-.165.398-.143.854.107 1.204l.527.738c.32.447.269 1.06-.12 1.45l-.774.773a1.125 1.125 0 01-1.449.12l-.738-.527c-.35-.25-.806-.272-1.203-.107-.397.165-.71.505-.781.929l-.149.894c-.09.542-.56.94-1.11.94h-1.094c-.55 0-1.019-.398-1.11-.94l-.148-.894c-.071-.424-.384-.764-.781-.93-.398-.164-.854-.142-1.204.108l-.738.527c-.447.32-1.06.269-1.45-.12l-.773-.774a1.125 1.125 0 01-.12-1.45l.527-.737c.25-.35.273-.806.108-1.204-.165-.397-.505-.71-.93-.78l-.894-.15c-.542-.09-.94-.56-.94-1.109v-1.094c0-.55.398-1.02.94-1.11l.894-.149c.424-.07.765-.383.93-.78.165-.398.143-.854-.107-1.204l-.527-.738a1.125 1.125 0 01.12-1.45l.773-.773a1.125 1.125 0 011.45-.12l.737.527c.35.25.807.272 1.204.107.397-.165.71-.505.78-.929l.15-.894z" />
              <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
            </svg>
          {/if}
          <span class="text-[10px] font-semibold leading-none">{tab.label}</span>
        </a>
      {/each}
    </div>
  </nav>
  {/if}
</div>
