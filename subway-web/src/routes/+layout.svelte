<script>
  import './layout.css';
  import { user, auth } from '$lib/stores';
  import { goto } from '$app/navigation';
  import { page } from '$app/stores';

  let { children } = $props();

  function logout() {
    auth.logout();
    goto('/');
  }

  const navLinks = [
    { href: '/',            label: '혼잡도' },
    { href: '/community',   label: '커뮤니티' },
    { href: '/complaints',  label: '민원' },
  ];
</script>

<nav class="bg-blue-700 text-white shadow-lg">
  <div class="max-w-5xl mx-auto px-4 py-3 flex items-center justify-between">
    <a href="/" class="text-xl font-bold tracking-tight">🚇 MetroHub</a>

    <div class="flex items-center gap-6 text-sm">
      {#each navLinks as link}
        <a
          href={link.href}
          class="hover:text-blue-200 transition-colors"
          class:font-semibold={$page.url.pathname === link.href}
          class:text-blue-200={$page.url.pathname === link.href}
        >
          {link.label}
        </a>
      {/each}

      {#if $user}
        <span class="text-blue-300">{$user.nickname}</span>
        <button
          onclick={logout}
          class="bg-blue-600 hover:bg-blue-500 px-3 py-1 rounded transition-colors"
        >
          로그아웃
        </button>
      {:else}
        <a href="/auth/login"
          class="bg-white text-blue-700 hover:bg-blue-50 px-3 py-1 rounded font-medium transition-colors"
        >
          로그인
        </a>
      {/if}
    </div>
  </div>
</nav>

<main class="min-h-screen bg-gray-50">
  {@render children()}
</main>
