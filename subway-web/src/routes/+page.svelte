<script>
  import { api } from '$lib/api';

  let stationName = $state('');
  let results     = $state([]);
  let loading     = $state(false);
  let error       = $state('');

  const LINE_NAMES = {
    '1001': '1호선', '1002': '2호선', '1003': '3호선', '1004': '4호선',
    '1005': '5호선', '1006': '6호선', '1007': '7호선', '1008': '8호선',
    '1009': '9호선', '1075': '신분당선', '1077': '수인분당선',
    '1063': '경의중앙선', '1065': '공항철도', '1094': '경강선',
  };

  const MAJOR_STATIONS = ['서울역', '강남', '홍대입구', '잠실', '신촌', '건대입구', '사당', '신림'];

  function congestionInfo(level) {
    if (level == null) return { label: '정보없음', cls: 'bg-gray-100 text-gray-500' };
    if (level <= 30)   return { label: '여유',     cls: 'bg-green-100 text-green-700' };
    if (level <= 60)   return { label: '보통',     cls: 'bg-yellow-100 text-yellow-700' };
    if (level <= 80)   return { label: '혼잡',     cls: 'bg-orange-100 text-orange-700' };
    return               { label: '매우혼잡',     cls: 'bg-red-100 text-red-700' };
  }

  async function search() {
    if (!stationName.trim()) return;
    loading = true; error = ''; results = [];
    try {
      results = await api.congestionByStation(stationName.trim());
      if (results.length === 0) error = '해당 역의 실시간 데이터가 없습니다.';
    } catch (e) {
      error = e.message;
    } finally {
      loading = false;
    }
  }

  function quickSearch(name) {
    stationName = name;
    search();
  }
</script>

<div class="max-w-5xl mx-auto px-4 py-8">
  <div class="text-center mb-8">
    <h1 class="text-3xl font-bold text-gray-800 mb-2">실시간 혼잡도 조회</h1>
    <p class="text-gray-500">역 이름으로 현재 혼잡도를 확인하세요</p>
  </div>

  <div class="flex gap-2 mb-4">
    <input
      bind:value={stationName}
      onkeydown={(e) => e.key === 'Enter' && search()}
      placeholder="역 이름 입력 (예: 강남)"
      class="flex-1 border border-gray-300 rounded-lg px-4 py-2.5 focus:outline-none focus:ring-2 focus:ring-blue-500"
    />
    <button
      onclick={search}
      disabled={loading}
      class="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2.5 rounded-lg font-medium transition-colors disabled:opacity-50"
    >
      {loading ? '검색 중...' : '검색'}
    </button>
  </div>

  <div class="flex flex-wrap gap-2 mb-8">
    {#each MAJOR_STATIONS as name}
      <button
        onclick={() => quickSearch(name)}
        class="text-sm bg-white border border-gray-200 hover:border-blue-400 hover:text-blue-600 px-3 py-1 rounded-full transition-colors"
      >
        {name}
      </button>
    {/each}
  </div>

  {#if error}
    <div class="bg-red-50 border border-red-200 text-red-600 rounded-lg px-4 py-3 mb-4">{error}</div>
  {/if}

  {#if results.length > 0}
    <h2 class="text-lg font-semibold text-gray-700 mb-3">{stationName} 혼잡도</h2>
    <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
      {#each results as item}
        {@const ci = congestionInfo(item.congestionLevel)}
        <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4">
          <div class="flex items-center justify-between mb-2">
            <span class="font-semibold text-gray-800">{LINE_NAMES[item.lineNumber] ?? item.lineNumber}</span>
            <span class="text-xs px-2 py-0.5 rounded-full font-medium {ci.cls}">{ci.label}</span>
          </div>
          {#if item.arrivalMessage}
            <p class="text-sm text-gray-700 font-medium">{item.arrivalMessage}</p>
          {/if}
          {#if item.trainNo}
            <p class="text-xs text-gray-400 mt-0.5">열차번호: {item.trainNo}</p>
          {/if}
          <p class="text-xs text-gray-400 mt-1">{new Date(item.updatedAt + 'Z').toLocaleTimeString('ko-KR')} 기준</p>
        </div>
      {/each}
    </div>
  {/if}
</div>
