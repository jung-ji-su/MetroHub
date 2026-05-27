<script>
  import { api } from '$lib/api';
  import { favorites } from '$lib/stores';

  const LINE_NAMES = {
    '1001': '1호선', '1002': '2호선', '1003': '3호선', '1004': '4호선',
    '1005': '5호선', '1006': '6호선', '1007': '7호선', '1008': '8호선',
    '1009': '9호선', '1075': '신분당선', '1077': '수인분당선',
    '1063': '경의중앙선', '1065': '공항철도',
  };

  const LINE_COLORS = {
    '1001': 'bg-blue-800',   '1002': 'bg-green-500',  '1003': 'bg-orange-500',
    '1004': 'bg-sky-400',    '1005': 'bg-purple-600',  '1006': 'bg-amber-700',
    '1007': 'bg-yellow-600', '1008': 'bg-pink-500',    '1009': 'bg-yellow-500',
    '1075': 'bg-red-500',    '1077': 'bg-yellow-400',  '1063': 'bg-teal-500',
    '1065': 'bg-sky-600',
  };

  // 역별 결과를 담는 맵: { stationName: { loading, error, data[] } }
  let resultsMap = $state({});
  let addInput   = $state('');
  let searchInput = $state('');

  function congestionInfo(level) {
    if (level == null) return { label: '정보없음', cls: 'bg-gray-100 text-gray-500' };
    if (level <= 30)   return { label: '여유',     cls: 'bg-green-100 text-green-700' };
    if (level <= 60)   return { label: '보통',     cls: 'bg-yellow-100 text-yellow-700' };
    if (level <= 80)   return { label: '혼잡',     cls: 'bg-orange-100 text-orange-700' };
    return               { label: '매우혼잡',     cls: 'bg-red-100 text-red-700' };
  }

  async function fetchStation(stationName) {
    resultsMap = { ...resultsMap, [stationName]: { loading: true, error: '', data: [] } };
    try {
      const data = await api.congestionByStation(stationName.trim());
      resultsMap = { ...resultsMap, [stationName]: { loading: false, error: '', data } };
    } catch (e) {
      resultsMap = { ...resultsMap, [stationName]: { loading: false, error: e.message, data: [] } };
    }
  }

  async function fetchAllFavorites() {
    for (const s of $favorites) {
      fetchStation(s);
    }
  }

  function addFavorite() {
    const name = addInput.trim();
    if (!name) return;
    favorites.add(name);
    addInput = '';
    fetchStation(name);
  }

  function removeFavorite(name) {
    favorites.remove(name);
    const next = { ...resultsMap };
    delete next[name];
    resultsMap = next;
  }

  async function searchAndAdd() {
    const name = searchInput.trim();
    if (!name) return;
    await fetchStation(name);
  }

  function formatTime(dt) {
    if (!dt) return '';
    return new Date(dt + 'Z').toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
  }
</script>

<div class="max-w-3xl mx-auto px-4 py-8 space-y-8">

  <!-- 내 경로 섹션 -->
  <section>
    <div class="flex items-center justify-between mb-3">
      <h2 class="text-xl font-bold text-gray-800">내 경로</h2>
      {#if $favorites.length > 0}
        <button
          onclick={fetchAllFavorites}
          class="bg-blue-600 hover:bg-blue-700 text-white text-sm px-4 py-1.5 rounded-lg transition-colors font-medium"
        >
          전체 조회
        </button>
      {/if}
    </div>

    <!-- 즐겨찾기 역 목록 -->
    {#if $favorites.length > 0}
      <div class="flex flex-wrap gap-2 mb-3">
        {#each $favorites as name}
          <div class="flex items-center gap-1 bg-blue-50 border border-blue-200 text-blue-700 text-sm px-3 py-1 rounded-full">
            <button onclick={() => fetchStation(name)} class="hover:text-blue-900 font-medium">{name}</button>
            <button onclick={() => removeFavorite(name)} class="text-blue-400 hover:text-red-500 ml-1">×</button>
          </div>
        {/each}
      </div>
    {:else}
      <p class="text-sm text-gray-400 mb-3">아래에서 자주 이용하는 역을 추가하세요.</p>
    {/if}

    <!-- 역 추가 입력 -->
    <div class="flex gap-2">
      <input
        bind:value={addInput}
        onkeydown={(e) => e.key === 'Enter' && addFavorite()}
        placeholder="역 이름 추가 (예: 강남)"
        class="flex-1 border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
      />
      <button
        onclick={addFavorite}
        disabled={!addInput.trim()}
        class="bg-blue-600 hover:bg-blue-700 text-white text-sm px-4 py-2 rounded-lg disabled:opacity-40 transition-colors"
      >
        + 추가
      </button>
    </div>
  </section>

  <!-- 직접 검색 섹션 -->
  <section>
    <h2 class="text-xl font-bold text-gray-800 mb-3">직접 검색</h2>
    <div class="flex gap-2">
      <input
        bind:value={searchInput}
        onkeydown={(e) => e.key === 'Enter' && searchAndAdd()}
        placeholder="역 이름 입력 (예: 홍대입구)"
        class="flex-1 border border-gray-300 rounded-lg px-4 py-2.5 focus:outline-none focus:ring-2 focus:ring-blue-500"
      />
      <button
        onclick={searchAndAdd}
        disabled={!searchInput.trim()}
        class="bg-blue-600 hover:bg-blue-700 text-white px-5 py-2.5 rounded-lg font-medium transition-colors disabled:opacity-40"
      >
        조회
      </button>
    </div>
    <p class="text-xs text-gray-400 mt-1.5">조회 결과는 10분간 캐시됩니다.</p>
  </section>

  <!-- 결과 섹션 -->
  {#if Object.keys(resultsMap).length > 0}
    <section class="space-y-6">
      {#each Object.entries(resultsMap) as [station, result]}
        <div>
          <div class="flex items-center gap-2 mb-2">
            <h3 class="font-bold text-gray-800 text-lg">{station}</h3>
            {#if !$favorites.includes(station)}
              <button
                onclick={() => { favorites.add(station); }}
                class="text-xs text-blue-500 hover:text-blue-700 border border-blue-300 hover:border-blue-500 px-2 py-0.5 rounded-full transition-colors"
              >
                + 내 경로 추가
              </button>
            {/if}
            <button
              onclick={() => fetchStation(station)}
              class="text-xs text-gray-400 hover:text-gray-600 ml-auto"
            >
              새로고침
            </button>
          </div>

          {#if result.loading}
            <div class="text-sm text-gray-400 py-4 text-center">조회 중...</div>
          {:else if result.error}
            <div class="bg-red-50 border border-red-200 text-red-600 rounded-lg px-4 py-2 text-sm">{result.error}</div>
          {:else if result.data.length === 0}
            <div class="text-sm text-gray-400 py-4 text-center">현재 운행 정보가 없습니다.</div>
          {:else}
            <div class="grid gap-2 sm:grid-cols-2">
              {#each result.data as item}
                {@const ci = congestionInfo(item.congestionLevel)}
                {@const lineCls = LINE_COLORS[item.lineNumber] ?? 'bg-gray-500'}
                <div class="bg-white rounded-xl border border-gray-100 shadow-sm p-3 flex items-start gap-3">
                  <span class="text-white text-xs font-bold px-2 py-1 rounded-lg {lineCls} whitespace-nowrap mt-0.5">
                    {LINE_NAMES[item.lineNumber] ?? item.lineNumber}
                  </span>
                  <div class="flex-1 min-w-0">
                    {#if item.arrivalMessage}
                      <p class="text-sm font-medium text-gray-800 truncate">{item.arrivalMessage}</p>
                    {/if}
                    <div class="flex items-center gap-2 mt-0.5">
                      <span class="text-xs px-1.5 py-0.5 rounded-full font-medium {ci.cls}">{ci.label}</span>
                      {#if item.trainNo}
                        <span class="text-xs text-gray-400">열차 {item.trainNo}</span>
                      {/if}
                    </div>
                    <p class="text-xs text-gray-400 mt-0.5">{formatTime(item.updatedAt)} 기준</p>
                  </div>
                </div>
              {/each}
            </div>
          {/if}
        </div>
      {/each}
    </section>
  {/if}
</div>
