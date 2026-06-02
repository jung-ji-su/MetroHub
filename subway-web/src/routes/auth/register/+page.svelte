<script>
  import { api } from '$lib/api';
  import { goto } from '$app/navigation';

  let email    = $state('');
  let nickname = $state('');
  let password = $state('');
  let error    = $state('');
  let loading  = $state(false);

  async function submit() {
    error = ''; loading = true;
    try {
      await api.register({ email, password, nickname });
      goto('/auth/login');
    } catch (e) {
      error = e.message;
    } finally {
      loading = false;
    }
  }
</script>

<div class="min-h-screen bg-white flex flex-col px-6">
  <div class="flex-1 flex flex-col justify-center max-w-sm w-full mx-auto">
    <!-- 상단 뒤로가기 -->
    <div class="mb-10">
      <a href="/auth/login" class="flex items-center gap-1.5 text-gray-400 text-sm mb-6 active:opacity-70">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
        </svg>
        로그인으로 돌아가기
      </a>
      <p class="text-xs text-gray-400 font-semibold tracking-widest mb-1">METROHUB</p>
      <h1 class="text-[28px] font-bold text-gray-900 leading-tight">회원가입</h1>
      <p class="text-sm text-gray-400 mt-1">빠르게 가입하고 시작해보세요</p>
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
        <label class="block text-xs font-semibold text-gray-500 mb-1.5 tracking-wide">닉네임</label>
        <input
          bind:value={nickname}
          type="text"
          required
          autocomplete="username"
          placeholder="사용할 닉네임"
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
          minlength="6"
          autocomplete="new-password"
          placeholder="6자 이상"
          disabled={loading}
          class="w-full bg-gray-100 rounded-2xl px-4 py-4 text-[15px] text-gray-900 placeholder-gray-400
                 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors disabled:opacity-60"
        />
      </div>

      <div class="pt-2">
        <button
          type="submit"
          disabled={loading || !email || !nickname || !password}
          class="w-full bg-blue-600 text-white text-[15px] font-bold py-4 rounded-2xl
                 disabled:opacity-40 active:bg-blue-700 transition-colors"
        >
          {loading ? '가입 중...' : '가입하기'}
        </button>
      </div>
    </form>

    <p class="text-center text-sm text-gray-400 mt-6">
      이미 계정이 있으신가요?
      <a href="/auth/login" class="text-blue-600 font-semibold ml-1">로그인</a>
    </p>
  </div>
</div>
