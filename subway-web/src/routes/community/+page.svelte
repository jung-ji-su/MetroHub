<script>
  import { LINES } from '$lib/lineStations';

  let searchInput = $state('');

  const filtered = $derived(
    searchInput.trim()
      ? LINES.filter(l =>
          l.label.includes(searchInput.trim()) ||
          l.short.includes(searchInput.trim()) ||
          String(l.id).includes(searchInput.trim())
        )
      : LINES
  );
</script>

<!-- 헤더 -->
<header class="px-5 pt-12 pb-4 sticky top-0 z-40" style="background: #ffffff; border-bottom: 1px solid #f3f4f6;">
  <p class="text-xs text-gray-400 font-medium tracking-wide">METROHUB</p>
  <h1 class="text-xl font-bold text-gray-900 leading-tight">커뮤니티</h1>

  <!-- 검색바 -->
  <div class="mt-3 relative">
    <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none"
         fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
      <path stroke-linecap="round" stroke-linejoin="round"
        d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 15.803a7.5 7.5 0 0010.607 0z" />
    </svg>
    <input
      bind:value={searchInput}
      type="text"
      placeholder="호선 검색 (예: 2호선)"
      class="w-full bg-gray-100 rounded-2xl pl-9 pr-4 py-2.5 text-[14px] text-gray-900 placeholder-gray-400
             focus:outline-none focus:ring-2 focus:ring-blue-400 focus:bg-white transition-colors"
    />
    {#if searchInput}
      <button
        onclick={() => searchInput = ''}
        class="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 flex items-center justify-center rounded-full bg-gray-300 text-white">
        <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="3">
          <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    {/if}
  </div>
</header>

<div class="px-4 pt-4 pb-4">
  {#if filtered.length === 0}
    <div class="flex flex-col items-center justify-center pt-12 text-center">
      <p class="text-gray-500 font-semibold">'{searchInput}' 검색 결과가 없어요</p>
      <p class="text-sm text-gray-400 mt-1">다른 호선명으로 검색해보세요</p>
    </div>
  {:else}
    <div class="grid grid-cols-3 gap-3">
      {#each filtered as line}
        <a
          href="/community/{line.id}"
          class="flex flex-col items-center justify-center rounded-2xl py-5 active:opacity-80 transition-opacity"
          style="background-color: {line.color}15; border: 1.5px solid {line.color}30;"
        >
          <div
            class="w-9 h-9 rounded-full flex items-center justify-center mb-2"
            style="background-color: {line.color};"
          >
            <span class="text-white text-sm font-bold leading-none">{line.short}</span>
          </div>
          <span class="text-xs font-semibold" style="color: {line.color};">{line.label}</span>
        </a>
      {/each}
    </div>
  {/if}
</div>
