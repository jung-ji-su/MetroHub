<script>
  import { api } from '$lib/api';
  import { token } from '$lib/stores';
  import { goto } from '$app/navigation';
  import { onMount } from 'svelte';
  import { LINE_META_SHORT as LINE_META } from '$lib/lineStations';

  let posts       = $state([]);
  let loading     = $state(true);
  let error       = $state('');
  let currentPage = $state(0);

  onMount(async () => {
    if (!$token) { goto('/auth/login'); return; }
    await loadPosts(0);
  });

  async function loadPosts(p) {
    loading = true; error = '';
    try {
      posts = await api.myPosts($token, p);
      currentPage = p;
    } catch (e) {
      error = e instanceof Error ? e.message : String(e);
    } finally {
      loading = false;
    }
  }

  function formatDate(dt) {
    const d = new Date(dt);
    const now = new Date();
    const diff = (now - d) / 1000;
    if (diff < 60)    return '방금 전';
    if (diff < 3600)  return `${Math.floor(diff / 60)}분 전`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`;
    return d.toLocaleDateString('ko-KR', { month: 'short', day: 'numeric' });
  }
</script>

<!-- 헤더 -->
<header class="px-5 pt-12 pb-4 sticky top-0 z-40" style="background: #ffffff; border-bottom: 1px solid #f3f4f6;">
  <div class="flex items-center gap-3">
    <a href="/my" class="w-9 h-9 flex items-center justify-center rounded-full bg-gray-100 active:bg-gray-200 transition-colors flex-shrink-0">
      <svg class="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
        <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
      </svg>
    </a>
    <h1 class="text-[17px] font-bold text-gray-900">내 게시글</h1>
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
          <div class="flex justify-between mb-3">
            <div class="h-4 bg-gray-100 rounded-lg w-1/3 animate-pulse"></div>
            <div class="h-5 bg-gray-100 rounded-full w-16 animate-pulse"></div>
          </div>
          <div class="h-3 bg-gray-100 rounded-lg w-5/6 mb-2 animate-pulse"></div>
          <div class="h-3 bg-gray-100 rounded-lg w-1/4 animate-pulse"></div>
        </div>
      {/each}
    </div>

  {:else if posts.length === 0}
    <div class="flex flex-col items-center justify-center pt-16 pb-8 text-center">
      <div class="w-14 h-14 bg-gray-100 rounded-full flex items-center justify-center mb-4">
        <svg class="w-7 h-7 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="1.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M7.5 8.25h9m-9 3H12m-9.75 1.51c0 1.6 1.123 2.994 2.707 3.227 1.129.166 2.27.293 3.423.379.35.026.67.21.865.501L12 21l2.755-4.133a1.14 1.14 0 01.865-.501 48.172 48.172 0 003.423-.379c1.584-.233 2.707-1.626 2.707-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0012 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018z" />
        </svg>
      </div>
      <p class="text-gray-700 font-semibold mb-1">작성한 게시글이 없어요</p>
      <p class="text-sm text-gray-400 mb-5">커뮤니티에서 첫 글을 남겨보세요</p>
      <a href="/community" class="bg-blue-600 text-white text-sm font-bold px-6 py-3 rounded-2xl active:bg-blue-700 transition-colors">
        커뮤니티 가기
      </a>
    </div>

  {:else}
    <div class="space-y-3">
      {#each posts as post}
        {@const meta = LINE_META[post.lineNumber]}
        <a
          href="/community/{post.lineNumber}/{post.id}"
          class="block bg-white rounded-2xl shadow-sm p-4 active:bg-gray-50 transition-colors"
          style="border-left: 3px solid {meta?.color ?? '#6B7280'};"
        >
          <div class="flex items-start justify-between gap-2 mb-1.5">
            <div class="flex items-center gap-1.5 min-w-0">
              {#if post.alert}
                <span class="flex-shrink-0 text-[10px] font-bold px-1.5 py-0.5 rounded-full text-white"
                      style="background-color: {meta?.color ?? '#6B7280'};">공지</span>
              {/if}
              <p class="font-semibold text-gray-900 text-[15px] leading-snug truncate">{post.title}</p>
            </div>
            <span class="flex-shrink-0 text-[10px] font-bold px-2 py-0.5 rounded-full text-white"
                  style="background-color: {meta?.color ?? '#6B7280'};">
              {meta?.label ?? post.lineNumber}
            </span>
          </div>
          <div class="flex items-center justify-between mt-1">
            <p class="text-xs text-gray-400">{formatDate(post.createdAt)}</p>
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
    {#if currentPage > 0 || posts.length === 20}
      <div class="flex gap-2 mt-4">
        {#if currentPage > 0}
          <button
            onclick={() => loadPosts(currentPage - 1)}
            class="flex-1 bg-white rounded-2xl py-3 text-sm font-semibold text-gray-600 shadow-sm active:bg-gray-50"
          >이전</button>
        {/if}
        {#if posts.length === 20}
          <button
            onclick={() => loadPosts(currentPage + 1)}
            class="flex-1 bg-white rounded-2xl py-3 text-sm font-semibold text-gray-600 shadow-sm active:bg-gray-50"
          >다음</button>
        {/if}
      </div>
    {/if}
  {/if}
</div>
