<script>
  import { LINES } from '$lib/lineStations';
</script>

<!-- 헤더 -->
<header class="px-5 pt-12 pb-4 sticky top-0 z-40" style="background: #ffffff; border-bottom: 1px solid #f3f4f6;">
  <p class="text-xs text-gray-400 font-medium tracking-wide">METROHUB</p>
  <h1 class="text-xl font-bold text-gray-900 leading-tight">커뮤니티</h1>
</header>

<div class="px-4 pt-5 pb-4">
  <p class="text-sm text-gray-400 mb-4">이용하는 호선을 선택하세요</p>

  <div class="grid grid-cols-3 gap-3">
    {#each LINES as line}
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
</div>
