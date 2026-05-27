<script>
  const LINES = [
    { id: '1', label: '1호선',   color: 'bg-blue-800'   },
    { id: '2', label: '2호선',   color: 'bg-green-500'  },
    { id: '3', label: '3호선',   color: 'bg-orange-500' },
    { id: '4', label: '4호선',   color: 'bg-sky-400'    },
    { id: '5', label: '5호선',   color: 'bg-purple-600' },
    { id: '6', label: '6호선',   color: 'bg-amber-700'  },
    { id: '7', label: '7호선',   color: 'bg-olive-600 bg-yellow-700' },
    { id: '8', label: '8호선',   color: 'bg-pink-500'   },
    { id: '9', label: '9호선',   color: 'bg-yellow-600' },
  ];
</script>

<div class="max-w-5xl mx-auto px-4 py-8">
  <div class="text-center mb-8">
    <h1 class="text-3xl font-bold text-gray-800 mb-2">호선별 커뮤니티</h1>
    <p class="text-gray-500">호선을 선택해 게시판을 확인하세요</p>
  </div>

  <div class="grid grid-cols-3 sm:grid-cols-4 lg:grid-cols-5 gap-4">
    {#each LINES as line}
      <a href="/community/{line.id}"
        class="flex flex-col items-center justify-center {line.color} text-white rounded-2xl py-6 shadow hover:scale-105 transition-transform"
      >
        <span class="text-2xl font-bold">{line.id}</span>
        <span class="text-sm mt-1">{line.label}</span>
      </a>
    {/each}
  </div>
</div>
