<script>
  import { searchStations } from '$lib/routeCalculator';
  import { LINE_META } from '$lib/lineStations';

  let {
    value      = $bindable(''),
    placeholder = '역 이름 입력 (예: 강남)',
    onselect,
    class: cls = '',
  } = $props();

  let results      = $state([]);
  let showDropdown = $state(false);
  let inputEl;

  function onInput() {
    results = searchStations(value);
    showDropdown = results.length > 0 && value.length > 0;
  }

  function select(station) {
    value = station.name;
    showDropdown = false;
    onselect?.(station.name);
  }

  function onBlur() {
    setTimeout(() => { showDropdown = false; }, 150);
  }
</script>

<div class="relative {cls}">
  <input
    bind:this={inputEl}
    bind:value
    {placeholder}
    oninput={onInput}
    onblur={onBlur}
    autocomplete="off"
    class="w-full bg-gray-100 rounded-2xl px-4 py-4 text-[15px] text-gray-900 placeholder-gray-400
           focus:outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors"
  />
  {#if showDropdown}
    <div class="absolute z-50 w-full bg-white rounded-2xl shadow-xl mt-1 overflow-hidden"
         style="border: 1px solid #e5e7eb;">
      {#each results as station}
        <button
          onmousedown={() => select(station)}
          class="w-full flex items-center justify-between px-4 py-3 hover:bg-gray-50 active:bg-blue-50 text-left"
          style="border-bottom: 1px solid #f9fafb;"
        >
          <span class="text-[14px] font-semibold text-gray-900">🚉 {station.name}</span>
          <div class="flex gap-1 flex-shrink-0 ml-2">
            {#each station.lines as line}
              <span
                class="text-[10px] font-bold px-1.5 py-0.5 rounded-full text-white leading-none"
                style="background-color: {LINE_META[line]?.color ?? '#6B7280'};"
              >{LINE_META[line]?.name ?? line}</span>
            {/each}
          </div>
        </button>
      {/each}
    </div>
  {/if}
</div>
