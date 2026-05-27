<script>
  import { user, auth, token } from '$lib/stores';
  import { goto } from '$app/navigation';
  import { api } from '$lib/api';
  import { onMount } from 'svelte';

  function logout() {
    auth.logout();
    goto('/');
  }

  const initial = $derived($user?.nickname?.[0]?.toUpperCase() ?? '?');

  const MENU_ITEMS = [
    {
      label: '내 민원 목록',
      href: '/complaints/my',
      icon: `<path stroke-linecap="round" stroke-linejoin="round" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />`,
    },
    {
      label: '커뮤니티',
      href: '/community',
      icon: `<path stroke-linecap="round" stroke-linejoin="round" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />`,
    },
  ];

  // 노선 구독 설정
  const LINE_OPTIONS = [
    { code: '1001', name: '1호선', color: '#0052A4' },
    { code: '1002', name: '2호선', color: '#00A84D' },
    { code: '1003', name: '3호선', color: '#EF7C1C' },
    { code: '1004', name: '4호선', color: '#00A5DE' },
    { code: '1005', name: '5호선', color: '#996CAC' },
    { code: '1006', name: '6호선', color: '#CD7C2F' },
    { code: '1007', name: '7호선', color: '#747F00' },
    { code: '1008', name: '8호선', color: '#E6186C' },
    { code: '1009', name: '9호선', color: '#BDB092' },
  ];

  let subscriptions = $state([]);
  let subLoading    = $state(false);
  let toggling      = $state(new Set());

  onMount(async () => {
    if ($token) await loadSubscriptions();
  });

  async function loadSubscriptions() {
    subLoading = true;
    try {
      subscriptions = await api.getSubscriptions($token);
    } catch (_) {
    } finally {
      subLoading = false;
    }
  }

  function isSubscribed(lineCode) {
    return subscriptions.some(s => s.subType === 'LINE' && s.subValue === lineCode);
  }

  function getSubscriptionId(lineCode) {
    return subscriptions.find(s => s.subType === 'LINE' && s.subValue === lineCode)?.id;
  }

  async function toggleSubscription(lineCode) {
    if (toggling.has(lineCode)) return;
    toggling = new Set([...toggling, lineCode]);
    try {
      if (isSubscribed(lineCode)) {
        const id = getSubscriptionId(lineCode);
        await api.deleteSubscription(id, $token);
      } else {
        await api.addSubscription({ type: 'LINE', value: lineCode }, $token);
      }
      await loadSubscriptions();
    } catch (_) {
    } finally {
      toggling = new Set([...toggling].filter(c => c !== lineCode));
    }
  }
</script>

<!-- 헤더 -->
<header class="px-5 pt-12 pb-4" style="background: #dbeafe; border-bottom: 1px solid #93c5fd;">
  <p class="text-xs text-gray-400 font-medium tracking-wide">METROHUB</p>
  <h1 class="text-xl font-bold text-gray-900 leading-tight">MY</h1>
</header>

<div class="px-4 pt-5 pb-4">
  {#if $user}
    <!-- 프로필 카드 -->
    <div class="bg-white rounded-3xl shadow-sm p-5 mb-5 flex items-center gap-4">
      <div class="w-14 h-14 bg-blue-600 rounded-full flex items-center justify-center flex-shrink-0">
        <span class="text-white text-xl font-bold">{initial}</span>
      </div>
      <div class="min-w-0">
        <p class="font-bold text-gray-900 text-[17px] truncate">{$user.nickname}</p>
        <p class="text-sm text-gray-400 truncate mt-0.5">{$user.email}</p>
      </div>
    </div>

    <!-- 메뉴 목록 -->
    <div class="bg-white rounded-3xl shadow-sm overflow-hidden mb-5">
      {#each MENU_ITEMS as item, i}
        <a
          href={item.href}
          class="flex items-center gap-3 px-5 py-4 active:bg-gray-50 transition-colors"
          class:border-t={i > 0}
          style={i > 0 ? 'border-top: 1px solid #f3f4f6;' : ''}
        >
          <div class="w-8 h-8 bg-gray-100 rounded-full flex items-center justify-center flex-shrink-0">
            <svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
              {@html item.icon}
            </svg>
          </div>
          <span class="flex-1 text-[15px] font-semibold text-gray-800">{item.label}</span>
          <svg class="w-4 h-4 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
          </svg>
        </a>
      {/each}
    </div>

    <!-- 알림 구독 설정 -->
    <div class="mb-5">
      <div class="flex items-center gap-2 px-1 mb-3">
        <svg class="w-4 h-4 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round"
            d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0" />
        </svg>
        <h2 class="text-[14px] font-bold text-gray-700">노선 알림 구독</h2>
        <span class="text-xs text-gray-400">혼잡도 급등 시 알림</span>
      </div>

      {#if subLoading}
        <div class="bg-white rounded-2xl p-4 shadow-sm">
          <div class="grid grid-cols-3 gap-2">
            {#each [1,2,3,4,5,6] as _}
              <div class="h-10 bg-gray-100 rounded-xl animate-pulse"></div>
            {/each}
          </div>
        </div>
      {:else}
        <div class="bg-white rounded-2xl shadow-sm p-3">
          <div class="grid grid-cols-3 gap-2">
            {#each LINE_OPTIONS as line}
              {@const subscribed = isSubscribed(line.code)}
              {@const loading = toggling.has(line.code)}
              <button
                onclick={() => toggleSubscription(line.code)}
                disabled={loading}
                class="flex items-center justify-center gap-1.5 py-2.5 px-2 rounded-xl text-[13px] font-bold transition-all active:scale-95 disabled:opacity-50"
                style={subscribed
                  ? `background-color: ${line.color}18; color: ${line.color}; border: 1.5px solid ${line.color}40;`
                  : 'background-color: #f9fafb; color: #9ca3af; border: 1.5px solid #f3f4f6;'}
              >
                {#if subscribed}
                  <svg class="w-3 h-3 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="3">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
                  </svg>
                {/if}
                {line.name}
              </button>
            {/each}
          </div>
        </div>
      {/if}
    </div>

    <!-- 로그아웃 -->
    <button
      onclick={logout}
      class="w-full bg-white rounded-3xl shadow-sm px-5 py-4 flex items-center gap-3 active:bg-gray-50 transition-colors"
    >
      <div class="w-8 h-8 bg-red-50 rounded-full flex items-center justify-center flex-shrink-0">
        <svg class="w-4 h-4 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15m3 0l3-3m0 0l-3-3m3 3H9" />
        </svg>
      </div>
      <span class="flex-1 text-left text-[15px] font-semibold text-red-400">로그아웃</span>
    </button>

  {:else}
    <!-- 비로그인 상태 -->
    <div class="flex flex-col items-center justify-center pt-12 pb-8 text-center">
      <div class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4">
        <svg class="w-8 h-8 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
        </svg>
      </div>
      <p class="text-gray-700 font-semibold mb-1">로그인이 필요해요</p>
      <p class="text-sm text-gray-400 mb-6">로그인하면 민원 접수와<br>커뮤니티를 이용할 수 있어요</p>
      <div class="flex gap-3">
        <a href="/auth/login"
           class="bg-blue-600 text-white text-sm font-bold px-6 py-3 rounded-2xl active:bg-blue-700 transition-colors">
          로그인
        </a>
        <a href="/auth/register"
           class="bg-gray-100 text-gray-700 text-sm font-bold px-6 py-3 rounded-2xl active:bg-gray-200 transition-colors">
          회원가입
        </a>
      </div>
    </div>
  {/if}
</div>
