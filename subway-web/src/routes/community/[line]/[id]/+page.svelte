<script>
  import { page } from '$app/stores';
  import { api } from '$lib/api';
  import { token, user } from '$lib/stores';
  import { goto } from '$app/navigation';

  const line   = $derived($page.params.line);
  const postId = $derived($page.params.id);

  let post     = $state(null);
  let comments = $state([]);
  let loading  = $state(true);
  let error    = $state('');

  let commentContent = $state('');
  let submitting     = $state(false);

  $effect(() => {
    loadPost(postId);
  });

  async function loadPost(id) {
    loading = true; error = '';
    try {
      [post, comments] = await Promise.all([api.getPost(id), api.getComments(id)]);
    } catch (e) {
      error = e.message;
    } finally {
      loading = false;
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
    return new Date(dt).toLocaleDateString('ko-KR', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  }
</script>

<div class="max-w-3xl mx-auto px-4 py-8">
  <a href="/community/{line}" class="text-sm text-gray-400 hover:text-blue-500">← {line}호선 게시판</a>

  {#if error}
    <div class="bg-red-50 border border-red-200 text-red-600 rounded-lg px-4 py-3 mt-4 text-sm">{error}</div>
  {/if}

  {#if loading}
    <div class="text-center py-12 text-gray-400">불러오는 중...</div>
  {:else if post}
    <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6 mt-4">
      <div class="flex items-center gap-2 mb-2">
        {#if post.alert}
          <span class="text-xs bg-red-100 text-red-600 px-2 py-0.5 rounded-full font-medium">알림</span>
        {/if}
        <h1 class="text-xl font-bold text-gray-800">{post.title}</h1>
      </div>
      <p class="text-sm text-gray-400 mb-4">{post.authorNickname} · {formatDate(post.createdAt)}</p>
      <div class="text-gray-700 whitespace-pre-wrap leading-relaxed border-t border-gray-100 pt-4">
        {post.content}
      </div>
    </div>

    <!-- 댓글 -->
    <div class="mt-6">
      <h2 class="font-semibold text-gray-700 mb-3">댓글 {comments.length}개</h2>

      {#if $user}
        <div class="bg-white rounded-xl border border-gray-200 p-3 mb-4">
          <textarea bind:value={commentContent} placeholder="댓글을 입력하세요" rows="2"
            class="w-full resize-none focus:outline-none text-sm text-gray-700"></textarea>
          <div class="flex justify-end mt-2">
            <button onclick={submitComment} disabled={submitting || !commentContent.trim()}
              class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-1.5 rounded-lg text-sm disabled:opacity-50">
              {submitting ? '등록 중...' : '댓글 등록'}
            </button>
          </div>
        </div>
      {:else}
        <a href="/auth/login" class="block text-center text-sm text-blue-600 bg-blue-50 rounded-lg py-3 mb-4 hover:bg-blue-100">
          로그인 후 댓글을 작성할 수 있습니다
        </a>
      {/if}

      <div class="space-y-3">
        {#each comments as comment}
          <div class="bg-white rounded-xl border border-gray-100 p-4">
            <p class="text-sm font-medium text-gray-700 mb-1">{comment.authorNickname}</p>
            <p class="text-sm text-gray-600 whitespace-pre-wrap">{comment.content}</p>
            <p class="text-xs text-gray-400 mt-1">{formatDate(comment.createdAt)}</p>
          </div>
        {/each}
      </div>
    </div>
  {/if}
</div>
