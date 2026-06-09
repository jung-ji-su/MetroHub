<script>
  import { onMount } from 'svelte';

  const STORAGE_KEY = 'metrohub_onboarded';

  let visible = $state(false);
  let step = $state(0);

  const slides = [
    {
      emoji: '🚇',
      title: '서울 지하철 실시간 정보',
      desc: '혼잡도와 열차 위치를\n실시간으로 확인하세요',
      bg: '#EFF6FF',
      accent: '#3B82F6',
    },
    {
      emoji: '⭐',
      title: '즐겨찾기 & 경로 등록',
      desc: '자주 이용하는 역과 경로를\n저장해 빠르게 확인하세요',
      bg: '#FFFBEB',
      accent: '#F59E0B',
    },
    {
      emoji: '🔔',
      title: '혼잡도 알림 구독',
      desc: '로그인 후 노선·역을 구독하면\n혼잡도 급등 시 알림을 드려요',
      bg: '#F0FDF4',
      accent: '#22C55E',
    },
  ];

  function finish() {
    localStorage.setItem(STORAGE_KEY, '1');
    visible = false;
  }

  onMount(() => {
    if (!localStorage.getItem(STORAGE_KEY)) {
      visible = true;
    }
  });
</script>

{#if visible}
  <div class="fixed inset-0 z-[100] flex items-end justify-center"
       style="background: rgba(0,0,0,0.5);">
    <div class="w-full max-w-[430px] bg-white rounded-t-3xl pb-10 pt-2 overflow-hidden"
         style="animation: slideUp 0.3s ease-out;">
      <!-- 드래그 핸들 -->
      <div class="flex justify-center mb-4 mt-3">
        <div class="w-10 h-1 bg-gray-200 rounded-full"></div>
      </div>

      <!-- 슬라이드 컨텐츠 -->
      <div class="px-6">
        <div class="rounded-3xl py-10 flex flex-col items-center text-center mb-6"
             style="background: {slides[step].bg};">
          <span class="text-6xl mb-4">{slides[step].emoji}</span>
          <p class="text-[18px] font-black text-gray-900 mb-2">{slides[step].title}</p>
          <p class="text-[14px] text-gray-500 leading-relaxed whitespace-pre-line">{slides[step].desc}</p>
        </div>

        <!-- 도트 인디케이터 -->
        <div class="flex justify-center gap-2 mb-6">
          {#each slides as _, i}
            <div class="transition-all rounded-full"
                 style="width: {i === step ? '20px' : '8px'}; height: 8px; background: {i === step ? slides[step].accent : '#E5E7EB'};"></div>
          {/each}
        </div>

        <!-- 버튼 -->
        {#if step < slides.length - 1}
          <button
            onclick={() => step++}
            class="w-full py-4 rounded-2xl text-[15px] font-black text-white active:opacity-80 transition-opacity"
            style="background: {slides[step].accent};">
            다음
          </button>
        {:else}
          <button
            onclick={finish}
            class="w-full py-4 rounded-2xl text-[15px] font-black text-white active:opacity-80 transition-opacity"
            style="background: {slides[step].accent};">
            시작하기
          </button>
        {/if}

        <button
          onclick={finish}
          class="w-full mt-3 py-2.5 text-[13px] font-semibold text-gray-400 active:text-gray-600 transition-colors">
          건너뛰기
        </button>
      </div>
    </div>
  </div>
{/if}

<style>
  @keyframes slideUp {
    from { transform: translateY(100%); }
    to   { transform: translateY(0); }
  }
</style>
