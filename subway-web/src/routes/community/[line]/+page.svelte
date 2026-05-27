<script>
  import { page } from '$app/stores';
  import { api } from '$lib/api';
  import { token, user } from '$lib/stores';
  import { goto } from '$app/navigation';

  const line = $derived($page.params.line);

  let posts      = $state([]);
  let loading    = $state(true);
  let error      = $state('');
  let currentPage = $state(0);

  let showForm   = $state(false);
  let title      = $state('');
  let content    = $state('');
  let isAlert    = $state(false);
  let submitting = $state(false);

  $effect(() => {
    loadPosts(line, 0);
  });

  async function loadPosts(ln, p) {
    loading = true; error = '';
    try {
      posts = await api.getPosts(ln, p);
      currentPage = p;
    } catch (e) {
      error = e.message;
    } finally {
      loading = false;
    }
  }

  async function submitPost() {
    if (!$token) { goto('/auth/login'); return; }
    submitting = true;
    try {
      await api.createPost({ lineNumber: line, title, content, alert: isAlert }, $token);
      title = ''; content = ''; isAlert = false; showForm = false;
      await loadPosts(line, 0);
    } catch (e) {
      error = e.message;
    } finally {
      submitting = false;
    }
  }

  function formatDate(dt) {
    return new Date(dt).toLocaleDateString('ko-KR', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  }
</script>

<div class="max-w-3xl mx-auto px-4 py-8">
  <div class="flex items-center justify-between mb-6">
    <div>
      <a href="/community" class="text-sm text-gray-400 hover:text-blue-500">← 커뮤니티</a>
      <h1 class="text-2xl font-bold text-gray-800 mt-1">{line}호선 게시판</h1>
    </div>
    {#if $user}
      <button onclick={() => showForm = !showForm}
        class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors">
        {showForm ? '취소' : '글쓰기'}
      </button>
    {:else}
      <a href="/auth/login" class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors">
        로그인 후 작성
      </a>
    {/if}
  </div>

  {#if showForm}
    <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4 mb-6">
      <input bind:value={title} placeholder="제목" class="w-full border-b border-gray-200 pb-2 mb-3 focus:outline-none focus:border-blue-400 font-medium" />
      <textarea bind:value={content} placeholder="내용을 입력하세요" rows="4"
        class="w-full resize-none focus:outline-none text-sm text-gray-700"></textarea>
      <div class="flex items-center justify-between mt-3 pt-3 border-t border-gray-100">
        <label class="flex items-center gap-2 text-sm text-gray-600 cursor-pointer">
          <input type="checkbox" bind:checked={isAlert} class="rounded" />
          알림 게시글
        </label>
        <button onclick={submitPost} disabled={submitting || !title || !content}
          class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-1.5 rounded-lg text-sm disabled:opacity-50">
          {submitting ? '등록 중...' : '등록'}
        </button>
      </div>
    </div>
  {/if}

  {#if error}
    <div class="bg-red-50 border border-red-200 text-red-600 rounded-lg px-4 py-3 mb-4 text-sm">{error}</div>
  {/if}

  {#if loading}
    <div class="text-center py-12 text-gray-400">불러오는 중...</div>
  {:else if posts.length === 0}
    <div class="text-center py-12 text-gray-400">아직 게시글이 없습니다.</div>
  {:else}
    <div class="space-y-3">
      {#each posts as post}
        <a href="/community/{line}/{post.id}"
          class="block bg-white rounded-xl shadow-sm border border-gray-100 p-4 hover:border-blue-200 transition-colors">
          <div class="flex items-start justify-between">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-2 mb-1">
                {#if post.alert}
                  <span class="text-xs bg-red-100 text-red-600 px-2 py-0.5 rounded-full font-medium">알림</span>
                {/if}
                <h3 class="font-medium text-gray-800 truncate">{post.title}</h3>
              </div>
              <p class="text-sm text-gray-400">{post.authorNickname} · {formatDate(post.createdAt)}</p>
            </div>
          </div>
        </a>
      {/each}
    </div>

    <div class="flex justify-center gap-3 mt-6">
      {#if currentPage > 0}
        <button onclick={() => loadPosts(line, currentPage - 1)}
          class="px-4 py-2 border border-gray-200 rounded-lg text-sm hover:bg-gray-50">이전</button>
      {/if}
      {#if posts.length === 10}
        <button onclick={() => loadPosts(line, currentPage + 1)}
          class="px-4 py-2 border border-gray-200 rounded-lg text-sm hover:bg-gray-50">다음</button>
      {/if}
    </div>
  {/if}
</div>
