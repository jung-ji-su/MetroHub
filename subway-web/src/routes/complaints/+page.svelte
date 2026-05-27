<script>
  import { api } from '$lib/api';
  import { token, user } from '$lib/stores';
  import { goto } from '$app/navigation';

  let category    = $state('');
  let stationName = $state('');
  let content     = $state('');
  let error       = $state('');
  let success     = $state('');
  let loading     = $state(false);

  const CATEGORIES = ['시설 파손', '청결 불량', '안전 위협', '직원 불친절', '운행 지연', '기타'];
  const STATIONS   = ['서울역', '강남', '홍대입구', '잠실', '신촌', '건대입구', '사당', '신림', '수원', '인천'];

  async function submit() {
    if (!$token) { goto('/auth/login'); return; }
    error = ''; success = ''; loading = true;
    try {
      await api.createComplaint({ category, stationName, content }, $token);
      success = '민원이 접수되었습니다. 내 민원 목록에서 진행상황을 확인할 수 있습니다.';
      category = ''; stationName = ''; content = '';
    } catch (e) {
      error = e.message;
    } finally {
      loading = false;
    }
  }
</script>

<div class="max-w-2xl mx-auto px-4 py-8">
  <div class="mb-6 flex items-center justify-between">
    <h1 class="text-2xl font-bold text-gray-800">민원 접수</h1>
    <a href="/complaints/my" class="text-sm text-blue-600 hover:underline">내 민원 목록 →</a>
  </div>

  {#if !$user}
    <div class="bg-yellow-50 border border-yellow-200 rounded-lg px-4 py-4 text-center">
      <p class="text-yellow-700 mb-2">민원 접수는 로그인 후 이용할 수 있습니다.</p>
      <a href="/auth/login" class="text-blue-600 font-medium hover:underline">로그인하기</a>
    </div>
  {:else}
    {#if success}
      <div class="bg-green-50 border border-green-200 text-green-700 rounded-lg px-4 py-3 mb-4 text-sm">{success}</div>
    {/if}
    {#if error}
      <div class="bg-red-50 border border-red-200 text-red-600 rounded-lg px-4 py-3 mb-4 text-sm">{error}</div>
    {/if}

    <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6 space-y-5">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1.5">민원 유형</label>
        <select bind:value={category} class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white">
          <option value="">선택하세요</option>
          {#each CATEGORIES as cat}
            <option value={cat}>{cat}</option>
          {/each}
        </select>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1.5">해당 역</label>
        <select bind:value={stationName} class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 bg-white">
          <option value="">선택하세요</option>
          {#each STATIONS as s}
            <option value={s}>{s}</option>
          {/each}
        </select>
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1.5">내용</label>
        <textarea bind:value={content} rows="5" placeholder="민원 내용을 상세하게 입력해주세요"
          class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none text-sm">
        </textarea>
      </div>

      <button onclick={submit} disabled={loading || !category || !stationName || !content}
        class="w-full bg-blue-600 hover:bg-blue-700 text-white py-2.5 rounded-lg font-medium transition-colors disabled:opacity-50">
        {loading ? '접수 중...' : '민원 접수하기'}
      </button>
    </div>
  {/if}
</div>
