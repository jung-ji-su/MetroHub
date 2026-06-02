<script>
  import { api } from '$lib/api';
  import { auth } from '$lib/stores';
  import { goto } from '$app/navigation';

  let email    = $state('');
  let password = $state('');
  let error    = $state('');
  let loading  = $state(false);

  async function submit() {
    error = ''; loading = true;
    try {
      const data = await api.login({ email, password });
      auth.login(data);
      goto('/');
    } catch (e) {
      error = e.message;
    } finally {
      loading = false;
    }
  }
</script>

<div class="min-h-screen bg-white flex flex-col px-6">
  <!-- 상단 여백 + 로고 -->
  <div class="flex-1 flex flex-col justify-center max-w-sm w-full mx-auto">
    <div class="mb-10">
      <p class="text-xs text-gray-400 font-semibold tracking-widest mb-1">METROHUB</p>
      <h1 class="text-[28px] font-bold text-gray-900 leading-tight">로그인</h1>
      <p class="text-sm text-gray-400 mt-1">계속하려면 로그인해 주세요</p>
    </div>

    {#if error}
      <div class="bg-red-50 text-red-500 text-sm rounded-2xl px-4 py-3 mb-5">{error}</div>
    {/if}

    <form onsubmit={(e) => { e.preventDefault(); submit(); }} class="space-y-3">
      <div>
        <label class="block text-xs font-semibold text-gray-500 mb-1.5 tracking-wide">이메일</label>
        <input
          bind:value={email}
          type="email"
          required
          autocomplete="email"
          placeholder="example@email.com"
          disabled={loading}
          class="w-full bg-gray-100 rounded-2xl px-4 py-4 text-[15px] text-gray-900 placeholder-gray-400
                 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors disabled:opacity-60"
        />
      </div>

      <div>
        <label class="block text-xs font-semibold text-gray-500 mb-1.5 tracking-wide">비밀번호</label>
        <input
          bind:value={password}
          type="password"
          required
          autocomplete="current-password"
          placeholder="비밀번호를 입력하세요"
          disabled={loading}
          class="w-full bg-gray-100 rounded-2xl px-4 py-4 text-[15px] text-gray-900 placeholder-gray-400
                 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors disabled:opacity-60"
        />
      </div>

      <div class="pt-2">
        <button
          type="submit"
          disabled={loading || !email || !password}
          class="w-full bg-blue-600 text-white text-[15px] font-bold py-4 rounded-2xl
                 disabled:opacity-40 active:bg-blue-700 transition-colors"
        >
          {loading ? '로그인 중...' : '로그인'}
        </button>
      </div>
    </form>

    <p class="text-center text-sm text-gray-400 mt-6">
      계정이 없으신가요?
      <a href="/auth/register" class="text-blue-600 font-semibold ml-1">회원가입</a>
    </p>
  </div>
</div>
