<script>
  // short: 아이콘 원 안에 표시할 짧은 텍스트
  const LINES = [
    { id: '1',      label: '1호선',      color: '#0052A4', short: '1'  },
    { id: '2',      label: '2호선',      color: '#00A84D', short: '2'  },
    { id: '3',      label: '3호선',      color: '#EF7C1C', short: '3'  },
    { id: '4',      label: '4호선',      color: '#00A5DE', short: '4'  },
    { id: '5',      label: '5호선',      color: '#996CAC', short: '5'  },
    { id: '6',      label: '6호선',      color: '#CD7C2F', short: '6'  },
    { id: '7',      label: '7호선',      color: '#747F00', short: '7'  },
    { id: '8',      label: '8호선',      color: '#E6186C', short: '8'  },
    { id: '9',      label: '9호선',      color: '#BDB092', short: '9'  },
    { id: '신분당',   label: '신분당선',   color: '#D31145', short: '신'  },
    { id: '수인분당', label: '수인분당선', color: '#F5A200', short: '수'  },
    { id: '경의중앙', label: '경의중앙선', color: '#77C4A3', short: '경'  },
    { id: '공항',   label: '공항철도',    color: '#0090D2', short: '공'  },
    { id: '경춘',   label: '경춘선',      color: '#10934F', short: '경'  },
    { id: '인천1',  label: '인천1호선',   color: '#7CA8D5', short: '인1' },
    { id: '인천2',  label: '인천2호선',   color: '#ED8B00', short: '인2' },
    { id: '의정부', label: '의정부경전철', color: '#FDA600', short: '의'  },
    { id: '김포골드', label: '김포골드라인', color: '#A17800', short: '김' },
    { id: '에버',   label: '에버라인',    color: '#55A43F', short: '에'  },
    { id: '경강',   label: '경강선',      color: '#0054A6', short: '경'  },
    { id: '우이신설', label: '우이신설선', color: '#B0CE18', short: '우'  },
    { id: '서해',   label: '서해선',      color: '#81A914', short: '서'  },
    { id: '신림',   label: '신림선',      color: '#6789CA', short: '신'  },
    { id: 'gtx-a',  label: 'GTX-A',      color: '#9A60B3', short: 'G'   },
  ];
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
