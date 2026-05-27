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

  const line   = $derived($page.params.line);
  const postId = $derived($page.params.id);
  const lineColor = $derived(LINE_COLORS[line] ?? '#6B7280');

  let post       = $state(null);
  let comments   = $state([]);
  let loading    = $state(true);
  let error      = $state('');
  let likeCount  = $state(0);
  let likedByMe  = $state(false);
  let liking     = $state(false);

  let commentContent = $state('');
  let submitting     = $state(false);

  $effect(() => {
    loadPost(postId);
  });

  async function loadPost(id) {
    loading = true; error = '';
    try {
      [post, comments] = await Promise.all([api.getPost(id), api.getComments(id)]);
      likeCount = post.likeCount ?? 0;
      likedByMe = post.likedByMe ?? false;
      // 로그인 상태면 내 좋아요 상태 반영
      if ($token) {
        try {
          const ls = await api.getLikeStatus(id, $token);
          likeCount = ls.likeCount;
          likedByMe = ls.liked;
        } catch (_) {}
      }
    } catch (e) {
      error = e.message;
    } finally {
      loading = false;
    }
  }

  async function toggleLike() {
    if (!$token) { goto('/auth/login'); return; }
    if (liking) return;
    liking = true;
    try {
      const result = await api.toggleLike(postId, $token);
      likeCount = result.likeCount;
      likedByMe = result.liked;
    } catch (e) {
      error = e.message;
    } finally {
      liking = false;
    }
  }

  async function submitComment() {
    if (!$token) { goto('/auth/login'); return; }
    if (!commentContent.trim()) return;
    submitting = true;
    try {
      await api.createComment(postId, { content: commentContent }, $token);
      commentContent = '';
      comments = await api.getComments(postId);
    } catch (e) {
      error = e.message;
    } finally {
      submitting = false;
    }
  }

  function formatDate(dt) {
    return new Date(dt).toLocaleDateString('ko-KR', {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  }
</script>

<!-- 헤더 -->
<header class="px-5 pt-12 pb-4 sticky top-0 z-40" style="background: #dbeafe; border-bottom: 1px solid #93c5fd;">
  <div class="flex items-center gap-3">
    <a href="/community/{line}" class="w-9 h-9 flex items-center justify-center rounded-full bg-gray-100 active:bg-gray-200 transition-colors flex-shrink-0">
      <svg class="w-5 h-5 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24" stroke-width="2.5">
        <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 19.5L8.25 12l7.5-7.5" />
      </svg>
    </a>
    <div class="flex items-center gap-2">
      <div class="w-3 h-3 rounded-full flex-shrink-0" style="background-color: {lineColor};"></div>
      <h1 class="text-[17px] font-bold text-gray-900 truncate">
        {line.length <= 2 && !isNaN(line) ? `${line}호선` : `${line}선`} 게시판
      </h1>
    </div>
  </div>
</header>

<div class="px-4 pt-4 pb-4">
  {#if error}
    <div class="bg-red-50 rounded-2xl px-4 py-3 text-sm text-red-500 mb-4">{error}</div>
  {/if}

  {#if loading}
    <div class="bg-white rounded-2xl shadow-sm p-5 mb-4">
      <div class="h-5 bg-gray-100 rounded-lg w-2/3 mb-4 animate-pulse"></div>
      <div class="h-3 bg-gray-100 rounded-lg w-1/3 mb-6 animate-pulse"></div>
      <div class="space-y-2">
        <div class="h-3 bg-gray-100 rounded-lg animate-pulse"></div>
        <div class="h-3 bg-gray-100 rounded-lg w-5/6 animate-pulse"></div>
        <div class="h-3 bg-gray-100 rounded-lg w-4/6 animate-pulse"></div>
      </div>
    </div>

  {:else if post}
    <!-- 게시글 본문 -->
    <div class="bg-white rounded-2xl shadow-sm overflow-hidden mb-4"
         style="border-left: 4px solid {lineColor};">
      <div class="p-5">
        <div class="flex items-start gap-2 mb-2">
          {#if post.alert}
            <span class="flex-shrink-0 text-xs font-bold px-2 py-0.5 rounded-full text-white" style="background-color: {lineColor};">공지</span>
          {/if}
          <h1 class="text-[18px] font-bold text-gray-900 leading-snug">{post.title}</h1>
        </div>
        <p class="text-xs text-gray-400 mb-4">{post.authorNickname} · {formatDate(post.createdAt)}</p>
        <div class="text-[15px] text-gray-700 whitespace-pre-wrap leading-relaxed border-t border-gray-100 pt-4 mb-4">
          {post.content}
        </div>
        <!-- 좋아요 버튼 -->
        <div class="flex items-center gap-2 pt-3 border-t border-gray-100">
          <button
            onclick={toggleLike}
            disabled={liking}
            class="flex items-center gap-1.5 px-4 py-2 rounded-full text-sm font-semibold transition-colors disabled:opacity-40"
            class:text-white={likedByMe}
            class:text-gray-500={!likedByMe}
            class:bg-gray-100={!likedByMe}
            style={likedByMe ? `background-color: ${lineColor};` : ''}
          >
            <svg class="w-4 h-4" fill={likedByMe ? 'currentColor' : 'none'} stroke="currentColor" viewBox="0 0 24 24" stroke-width="2">
              <path stroke-linecap="round" stroke-linejoin="round" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
            </svg>
            {likeCount}
          </button>
        </div>
      </div>
    </div>

    <!-- 댓글 섹션 -->
    <div class="mb-4">
      <p class="text-sm font-bold text-gray-900 px-1 mb-3">댓글 {comments.length}개</p>

      {#if $user}
        <div class="bg-white rounded-2xl shadow-sm p-3 mb-3 flex gap-2">
          <input
            bind:value={commentContent}
            placeholder="댓글을 입력하세요"
            onkeydown={(e) => e.key === 'Enter' && !e.shiftKey && submitComment()}
            class="flex-1 bg-gray-100 rounded-xl px-3 py-2.5 text-sm text-gray-900 placeholder-gray-400
                   focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors"
          />
          <button
            onclick={submitComment}
            disabled={submitting || !commentContent.trim()}
            class="text-white text-sm font-semibold px-4 rounded-xl disabled:opacity-40 active:opacity-80 transition-opacity flex-shrink-0"
            style="background-color: {lineColor};"
          >
            {submitting ? '...' : '등록'}
          </button>
        </div>
      {:else}
        <a
          href="/auth/login"
          class="block text-center text-sm font-semibold py-3.5 rounded-2xl mb-3"
          style="background-color: {lineColor}15; color: {lineColor};"
        >
          로그인 후 댓글을 작성할 수 있습니다
        </a>
      {/if}

      {#if comments.length === 0}
        <p class="text-sm text-gray-400 text-center py-6">아직 댓글이 없어요</p>
      {:else}
        <div class="space-y-2">
          {#each comments as comment}
            <div class="bg-white rounded-2xl shadow-sm p-4">
              <p class="text-xs font-bold text-gray-700 mb-1.5">{comment.authorNickname}</p>
              <p class="text-sm text-gray-600 whitespace-pre-wrap leading-relaxed">{comment.content}</p>
              <p class="text-[11px] text-gray-400 mt-2">{formatDate(comment.createdAt)}</p>
            </div>
          {/each}
        </div>
      {/if}
    </div>
  {/if}
</div>
