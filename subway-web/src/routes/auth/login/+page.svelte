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

<div class="min-h-[80vh] flex items-center justify-center px-4">
  <div class="bg-white rounded-2xl shadow-md p-8 w-full max-w-sm">
    <h1 class="text-2xl font-bold text-gray-800 mb-6 text-center">로그인</h1>

    {#if error}
      <div class="bg-red-50 border border-red-200 text-red-600 rounded-lg px-4 py-2 mb-4 text-sm">{error}</div>
    {/if}

    <form onsubmit={(e) => { e.preventDefault(); submit(); }} class="space-y-4">
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">이메일</label>
        <input bind:value={email} type="email" required
          class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder="example@email.com"
        />
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700 mb-1">비밀번호</label>
        <input bind:value={password} type="password" required
          class="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>
      <button type="submit" disabled={loading}
        class="w-full bg-blue-600 hover:bg-blue-700 text-white py-2.5 rounded-lg font-medium transition-colors disabled:opacity-50"
      >
        {loading ? '로그인 중...' : '로그인'}
      </button>
    </form>

    <p class="text-center text-sm text-gray-500 mt-4">
      계정이 없으신가요?
      <a href="/auth/register" class="text-blue-600 hover:underline">회원가입</a>
    </p>
  </div>
</div>
