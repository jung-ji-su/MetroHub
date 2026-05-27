<script>
  import { page } from '$app/stores';
  import { api } from '$lib/api';
  import { token, user } from '$lib/stores';
  import { goto } from '$app/navigation';

  const LINE_COLORS = {
    '1': '#0052A4', '2': '#00A84D', '3': '#EF7C1C', '4': '#00A5DE',
    '5': '#996CAC', '6': '#CD7C2F', '7': '#747F00', '8': '#E6186C',
    '9': '#BDB092', '신분당': '#D31145', '수인분당': '#F5A200',
    '경의중앙': '#77C4A3', '공항': '#0090D2',
  };

  const line = $derived($page.params.line);
  const lineColor = $derived(LINE_COLORS[line] ?? '#6B7280');
  const lineLabel = $derived(line.length <= 2 && !isNaN(line) ? `${line}호선` : `${line}선`);

  let posts       = $state([]);
  let loading     = $state(true);
  let error       = $state('');
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
    const d = new Date(dt);
    const now = new Date();
    const diff = (now - d) / 1000;
    if (diff < 60)   return '방금 전';
    if (diff < 3600) return `${Math.floor(diff / 60)}분 전`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`;
    return d.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' });
  }
</script>

<!-- 헤더 -->
<header class="px-5 pt-12 pb-4 sticky top-0 z-40" style="background: #ffffff; border-bottom: 1px solid #f3f4f6;">
  <div class="flex items-center gap-3">
    <a href="/community" class="w-9 h-9 flex items-center justify-center rounded-full bg-gray-100 active:bg-gray-200 transition-colors flex-shrink-0">
      <svg class="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
        <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
      </svg>
    </a>
    <div class="flex items-center gap-2">
      <div class="w-3 h-3 rounded-full flex-shrink-0" style="background-color: {lineColor};"></div>
      <h1 class="text-[17px] font-bold text-gray-900">{lineLabel} 게시판</h1>
    </div>
  </div>
</header>

<div class="px-4 pt-4 pb-4">
  {#if error}
    <div class="bg-red-50 rounded-2xl px-4 py-3 text-sm text-red-500 mb-4">{error}</div>
  {/if}

  {#if loading}
    <div class="space-y-3">
      {#each [1,2,3] as _}
        <div class="bg-white rounded-2xl p-4 shadow-sm">
          <div class="h-4 bg-gray-100 rounded-lg w-3/4 mb-3 animate-pulse"></div>
          <div class="h-3 bg-gray-100 rounded-lg w-1/3 animate-pulse"></div>
        </div>
      {/each}
    </div>

  {:else if posts.length === 0}
    <div class="flex flex-col items-center justify-center pt-16 pb-8 text-center">
      <div class="w-14 h-14 rounded-full flex items-center justify-center mb-4" style="background-color: {lineColor}15;">
        <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.5" style="color: {lineColor};">
          <path stroke-linecap="round" stroke-linejoin="round" d="M7.5 8.25h9m-9 3H12m-9.75 1.51c0 1.6 1.123 2.994 2.707 3.227 1.129.166 2.27.293 3.423.379.35.026.67.21.865.501L12 21l2.755-4.133a1.14 1.14 0 01.865-.501 48.172 48.172 0 003.423-.379c1.584-.233 2.707-1.626 2.707-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0012 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018z" />
        </svg>
      </div>
      <p class="text-gray-700 font-semibold">아직 게시글이 없어요</p>
      <p class="text-sm text-gray-400 mt-1">첫 번째 글을 작성해보세요</p>
    </div>

  {:else}
    <div class="space-y-2">
      {#each posts as post}
        <a
          href="/community/{line}/{post.id}"
          class="block bg-white rounded-2xl shadow-sm p-4 active:bg-gray-50 transition-colors"
        >
          <div class="flex items-start gap-2 mb-1.5">
            {#if post.alert}
              <span class="flex-shrink-0 text-xs font-bold px-2 py-0.5 rounded-full text-white" style="background-color: {lineColor};">공지</span>
            {/if}
            <p class="font-semibold text-gray-900 text-[15px] leading-snug truncate">{post.title}</p>
          </div>
          <div class="flex items-center justify-between mt-1">
            <p class="text-xs text-gray-400">{post.authorNickname} · {formatDate(post.createdAt)}</p>
            {#if post.likeCount > 0}
              <div class="flex items-center gap-1 text-xs text-gray-400">
                <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
                </svg>
                {post.likeCount}
              </div>
            {/if}
          </div>
        </a>
      {/each}
    </div>

    <!-- 페이지네이션 -->
    {#if currentPage > 0 || posts.length === 10}
      <div class="flex gap-2 mt-4">
        {#if currentPage > 0}
          <button
            onclick={() => loadPosts(line, currentPage - 1)}
            class="flex-1 bg-white rounded-2xl py-3 text-sm font-semibold text-gray-600 shadow-sm active:bg-gray-50"
          >이전</button>
        {/if}
        {#if posts.length === 10}
          <button
            onclick={() => loadPosts(line, currentPage + 1)}
            class="flex-1 bg-white rounded-2xl py-3 text-sm font-semibold text-gray-600 shadow-sm active:bg-gray-50"
          >다음</button>
        {/if}
      </div>
    {/if}
  {/if}
</div>

<!-- 글쓰기 FAB -->
{#if !showForm}
  {#if $user}
    <button
      onclick={() => showForm = true}
      class="fixed bottom-[84px] right-4 w-14 h-14 rounded-full text-white shadow-lg flex items-center justify-center active:opacity-80 transition-opacity z-30"
      style="background-color: {lineColor};"
    >
      <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
        <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
      </svg>
    </button>
  {:else}
    <a
      href="/auth/login"
      class="fixed bottom-[84px] right-4 w-14 h-14 rounded-full text-white shadow-lg flex items-center justify-center active:opacity-80 transition-opacity z-30"
      style="background-color: {lineColor};"
    >
      <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
        <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
      </svg>
    </a>
  {/if}
{/if}

<!-- 글쓰기 바텀시트 -->
{#if showForm}
  <div
    class="fixed inset-0 bg-black/40 z-40"
    onclick={() => showForm = false}
    role="presentation"
  ></div>
  <div class="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-[430px] bg-white rounded-t-3xl z-50 px-5 pt-5 pb-8"
       style="padding-bottom: max(2rem, env(safe-area-inset-bottom));">
    <div class="flex items-center justify-between mb-4">
      <h3 class="text-[16px] font-bold text-gray-900">글쓰기</h3>
      <button onclick={() => showForm = false} class="text-gray-400 active:opacity-70">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
        </svg>
      </button>
    </div>

    <input
      bind:value={title}
      placeholder="제목"
      class="w-full bg-gray-100 rounded-2xl px-4 py-3 text-[15px] text-gray-900 placeholder-gray-400
             focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors mb-3"
    />
    <textarea
      bind:value={content}
      placeholder="내용을 입력하세요"
      rows="4"
      class="w-full bg-gray-100 rounded-2xl px-4 py-3 text-sm text-gray-900 placeholder-gray-400
             focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors resize-none mb-3"
    ></textarea>

    <div class="flex items-center justify-between">
      <label class="flex items-center gap-2 text-sm text-gray-600 cursor-pointer">
        <div
          onclick={() => isAlert = !isAlert}
          role="checkbox"
          aria-checked={isAlert}
          class="w-5 h-5 rounded-md border-2 flex items-center justify-center transition-colors cursor-pointer"
          style="background-color: {isAlert ? lineColor : 'transparent'}; border-color: {isAlert ? lineColor : '#D1D5DB'};"
        >
          {#if isAlert}
            <svg class="w-3 h-3 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="3">
              <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5" />
            </svg>
          {/if}
        </div>
        공지 게시글
      </label>

      <button
        onclick={submitPost}
        disabled={submitting || !title.trim() || !content.trim()}
        class="text-white text-sm font-bold px-6 py-2.5 rounded-2xl disabled:opacity-40 active:opacity-80 transition-opacity"
        style="background-color: {lineColor};"
      >
        {submitting ? '등록 중...' : '등록'}
      </button>
    </div>
  </div>
{/if}
