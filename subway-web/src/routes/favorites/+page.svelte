<script>
  import { favorites } from '$lib/stores';
  import { LINE_META, LINE_STATIONS } from '$lib/lineStations';
  import { api } from '$lib/api';
  import { onMount } from 'svelte';
  import StationSearch from '$lib/StationSearch.svelte';

  let congestionMap = $state({});
  let searchInput = $state('');
  let showSearch = $state(false);

  function getLinesForStation(name) {
    return Object.entries(LINE_STATIONS)
      .filter(([, stations]) => stations.includes(name))
      .map(([code]) => ({ code, ...LINE_META[code] }));
  }

  async function fetchCongestion(name) {
    congestionMap = { ...congestionMap, [name]: { loading: true, data: [] } };
    try {
      const data = await api.congestionByStation(name);
      congestionMap = { ...congestionMap, [name]: { loading: false, data: Array.isArray(data) ? data : [data].filter(Boolean) } };
    } catch (_) {
      congestionMap = { ...congestionMap, [name]: { loading: false, data: [] } };
    }
  }

  function congestionColor(level) {
    if (!level) return '#e5e7eb';
    if (level >= 90) return '#FCA5A5';
    if (level >= 80) return '#FCD34D';
    if (level >= 60) return '#86EFAC';
    return '#BAE6FD';
  }

  function congestionLabel(level) {
    if (!level) return '정보없음';
    if (level >= 90) return '매우혼잡';
    if (level >= 80) return '혼잡';
    if (level >= 60) return '보통';
    return '여유';
  }

  function addFav(name) {
    if (!name) return;
    favorites.add(name);
    fetchCongestion(name);
    showSearch = false;
    searchInput = '';
  }

  function removeFav(name) {
    favorites.remove(name);
    const next = { ...congestionMap };
    delete next[name];
    congestionMap = next;
  }

  onMount(() => {
    for (const s of $favorites) fetchCongestion(s);
  });
</script>

<div class="min-h-screen bg-gray-50">
  <header class="px-5 pt-12 pb-4 sticky top-0 z-40"
          style="background: #ffffff; border-bottom: 1px solid #f3f4f6;">
    <div class="flex items-center gap-3">
      <a href="javascript:history.back()"
         class="w-9 h-9 flex items-center justify-center rounded-full bg-gray-100 active:bg-gray-200 flex-shrink-0">
        <svg class="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
        </svg>
      </a>
      <h1 class="text-[17px] font-bold text-gray-900 flex-1">즐겨찾기</h1>
      <button
        onclick={() => showSearch = !showSearch}
        class="w-9 h-9 flex items-center justify-center rounded-full bg-gray-100 active:bg-gray-200 flex-shrink-0">
        <svg class="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
        </svg>
      </button>
    </div>

    {#if showSearch}
      <div class="pt-3">
        <StationSearch
          bind:value={searchInput}
          placeholder="역 이름 입력 후 선택"
          onselect={(name) => addFav(name)}
        />
      </div>
    {/if}
  </header>

  <div class="px-4 py-4 space-y-3">
    {#if $favorites.length === 0}
      <div class="flex flex-col items-center justify-center pt-16 text-center">
        <div class="w-16 h-16 bg-yellow-50 rounded-full flex items-center justify-center mb-4">
          <svg class="w-8 h-8 text-yellow-300" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.5">
            <path stroke-linecap="round" stroke-linejoin="round"
              d="M11.48 3.499a.562.562 0 011.04 0l2.125 5.111a.563.563 0 00.475.345l5.518.442c.499.04.701.663.321.988l-4.204 3.602a.563.563 0 00-.182.557l1.285 5.385a.562.562 0 01-.84.61l-4.725-2.885a.563.563 0 00-.586 0L6.982 20.54a.562.562 0 01-.84-.61l1.285-5.386a.562.562 0 00-.182-.557l-4.204-3.602a.562.562 0 01.321-.988l5.518-.442a.563.563 0 00.475-.345L11.48 3.5z" />
          </svg>
        </div>
        <p class="text-gray-700 font-semibold mb-1">즐겨찾기 역이 없어요</p>
        <p class="text-sm text-gray-400 mb-5">자주 이용하는 역을 추가해보세요</p>
        <button
          onclick={() => showSearch = true}
          class="bg-blue-600 text-white text-sm font-bold px-5 py-3 rounded-2xl active:bg-blue-700 transition-colors">
          + 역 추가하기
        </button>
      </div>

    {:else}
      {#each $favorites as name}
        {@const lines = getLinesForStation(name)}
        {@const entry = congestionMap[name]}
        <a href="/station/{encodeURIComponent(name)}"
           class="block bg-white rounded-2xl shadow-sm overflow-hidden active:opacity-80 transition-opacity">
          <div class="px-4 pt-4 pb-3">
            <div class="flex items-start justify-between gap-2">
              <div class="min-w-0">
                <p class="text-[16px] font-bold text-gray-900">{name}역</p>
                <div class="flex flex-wrap gap-1 mt-1">
                  {#each lines as line}
                    <span class="text-[10px] font-bold px-1.5 py-0.5 rounded-full text-white"
                          style="background: {line.color};">{line.name}</span>
                  {/each}
                </div>
              </div>
              <button
                onclick={(e) => { e.preventDefault(); e.stopPropagation(); removeFav(name); }}
                class="w-8 h-8 flex items-center justify-center rounded-full bg-gray-100 active:bg-red-50 flex-shrink-0 mt-0.5">
                <svg class="w-4 h-4 text-gray-400 active:text-red-400" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M11.48 3.499a.562.562 0 011.04 0l2.125 5.111a.563.563 0 00.475.345l5.518.442c.499.04.701.663.321.988l-4.204 3.602a.563.563 0 00-.182.557l1.285 5.385a.562.562 0 01-.84.61l-4.725-2.885a.563.563 0 00-.586 0L6.982 20.54a.562.562 0 01-.84-.61l1.285-5.386a.562.562 0 00-.182-.557l-4.204-3.602a.562.562 0 01.321-.988l5.518-.442a.563.563 0 00.475-.345L11.48 3.5z" />
                </svg>
              </button>
            </div>

            <!-- 혼잡도 미리보기 -->
            {#if entry?.loading}
              <div class="mt-3 flex gap-2">
                {#each [1,2] as _}
                  <div class="h-7 w-20 bg-gray-100 rounded-xl animate-pulse"></div>
                {/each}
              </div>
            {:else if entry?.data?.length > 0}
              <div class="mt-3 flex flex-wrap gap-1.5">
                {#each entry.data as c}
                  {@const meta = LINE_META[c.lineNumber]}
                  {@const col = congestionColor(c.congestionLevel)}
                  <span class="flex items-center gap-1.5 text-[12px] font-bold px-2.5 py-1 rounded-xl"
                        style="background:{col}30; border: 1px solid {col}; color: #374151;">
                    {#if meta}
                      <span class="w-2 h-2 rounded-full flex-shrink-0" style="background:{meta.color}"></span>
                    {/if}
                    {congestionLabel(c.congestionLevel)}
                  </span>
                {/each}
              </div>
            {/if}
          </div>

          <!-- 상세 보기 버튼 영역 -->
          <div class="px-4 pb-3 flex items-center gap-1 text-blue-500">
            <span class="text-[12px] font-bold">역 상세 보기</span>
            <svg class="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M8.25 4.5l7.5 7.5-7.5 7.5" />
            </svg>
          </div>
        </a>
      {/each}
    {/if}
  </div>
</div>
