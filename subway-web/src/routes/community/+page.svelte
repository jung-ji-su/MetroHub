<script>
  const LINES = [
    { id: '1', label: '1호선',    color: '#0052A4' },
    { id: '2', label: '2호선',    color: '#00A84D' },
    { id: '3', label: '3호선',    color: '#EF7C1C' },
    { id: '4', label: '4호선',    color: '#00A5DE' },
    { id: '5', label: '5호선',    color: '#996CAC' },
    { id: '6', label: '6호선',    color: '#CD7C2F' },
    { id: '7', label: '7호선',    color: '#747F00' },
    { id: '8', label: '8호선',    color: '#E6186C' },
    { id: '9', label: '9호선',    color: '#BDB092' },
    { id: '신분당', label: '신분당선', color: '#D31145' },
    { id: '수인분당', label: '수인분당선', color: '#F5A200' },
    { id: '경의중앙', label: '경의중앙선', color: '#77C4A3' },
    { id: '공항', label: '공항철도', color: '#0090D2' },
  ];
</script>

<!-- 헤더 -->
<header class="px-5 pt-12 pb-4 sticky top-0 z-40" style="background: #dbeafe; border-bottom: 1px solid #93c5fd;">
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
          <span class="text-white text-sm font-bold leading-none">{line.id.length <= 2 ? line.id : line.id[0]}</span>
        </div>
        <span class="text-xs font-semibold" style="color: {line.color};">{line.label}</span>
      </a>
    {/each}
  </div>
</div>
