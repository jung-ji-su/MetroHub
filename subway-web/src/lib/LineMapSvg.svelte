<script>
  import { LINE_META } from './lineStations.js';

  let {
    stations = [],
    trains = [],
    lineCode = '',
    lineColor = '#6B7280',
    selectedTrainNo = null,
    onTrainClick = () => {},
    useMock = false,
    focusStation = null,
  } = $props();

  // ── SVG 캔버스 크기 ───────────────────────────────────────────────────
  const W = 500, MX = 38, TX = W - 2 * MX;  // 캔버스 너비, 좌우 마진, 트랙 너비
  const AVG_S = 90;

  // ── 2호선 루프 데이터 ─────────────────────────────────────────────────
  const LOOP_STS = [
    '시청','을지로입구','을지로3가','을지로4가','동대문역사문화공원',
    '신당','상왕십리','왕십리','한양대','뚝섬','성수','건대입구','구의',
    '강변','잠실나루','잠실','잠실새내','종합운동장','삼성','선릉','역삼',
    '강남','교대','서초','방배','사당','낙성대','서울대입구','봉천','신림',
    '신대방','구로디지털단지','대림','신도림','문래','영등포구청','당산',
    '합정','홍대입구','신촌','이대','아현','충정로',
  ];
  // 루프 타원 파라미터 (500x450 캔버스 기준)
  const CX = 250, CY = 218, RX = 178, RY = 188;

  // ── 형태 판별 ─────────────────────────────────────────────────────────
  const isLoop = $derived(lineCode === '1002');

  // ── 스네이크 레이아웃 ─────────────────────────────────────────────────
  function snakePerRow(n) {
    if (n <= 8)  return n;
    if (n <= 16) return Math.ceil(n / 2);
    if (n <= 30) return 10;
    if (n <= 50) return 12;
    return 14;
  }

  const perRow = $derived(isLoop ? LOOP_STS.length : snakePerRow(stations.length));
  const rowH   = $derived(stations.length <= 14 ? 80 : 70);

  // ── 좌표 계산 ─────────────────────────────────────────────────────────
  function coords2Gosen() {
    const m = {};
    const N = LOOP_STS.length;
    LOOP_STS.forEach((st, i) => {
      const a = -Math.PI / 2 + (i / N) * 2 * Math.PI;
      m[st] = { x: CX + RX * Math.cos(a), y: CY + RY * Math.sin(a), loop: true, loopIdx: i };
    });
    // 성수지선: 성수(i=10) → 용답·신답·용두·신설동
    const sC = m['성수'];
    if (sC) ['용답','신답','용두','신설동'].forEach((st, i) => {
      m[st] = { x: sC.x + (i+1)*15, y: sC.y + (i+1)*32, loop: false };
    });
    // 신정지선: 신도림(i=33) → 도림천·양천구청·신정네거리·까치산
    const dC = m['신도림'];
    if (dC) ['도림천','양천구청','신정네거리','까치산'].forEach((st, i) => {
      m[st] = { x: dC.x - (i+1)*10, y: dC.y + (i+1)*40, loop: false };
    });
    return m;
  }

  function coordsSnake() {
    const pr = perRow, rh = rowH;
    const m = {};
    stations.forEach((st, i) => {
      const row  = Math.floor(i / pr);
      const pos  = i % pr;
      const rowN = Math.min(pr, stations.length - row * pr);
      const goR  = row % 2 === 0;
      const frac = rowN > 1 ? pos / (rowN - 1) : 0.5;
      m[st] = {
        x: goR ? MX + frac * TX : (W - MX) - frac * TX,
        y: 36 + row * rh,
        row, pos, goR,
      };
    });
    return m;
  }

  const coords = $derived.by(() => isLoop ? coords2Gosen() : coordsSnake());

  // ── 캔버스 높이 ───────────────────────────────────────────────────────
  const svgH = $derived.by(() => {
    if (isLoop) {
      const ys = Object.values(coords).map(c => c.y);
      return Math.max(...ys) + 48;
    }
    const rows = Math.ceil(stations.length / perRow);
    return 36 + rows * rowH + 44;
  });

  // ── SVG 트랙 경로 ─────────────────────────────────────────────────────
  function loopPath() {
    const pts = LOOP_STS.filter(s => coords[s]).map(s => `${coords[s].x},${coords[s].y}`);
    return pts.length ? `M ${pts.join(' L ')} Z` : '';
  }

  function snakePath() {
    const pr = perRow;
    let d = '';
    stations.forEach((st, i) => {
      const c = coords[st];
      if (!c) return;
      if (i === 0) { d = `M ${c.x} ${c.y}`; return; }
      const pos = i % pr;
      if (pos === 0) {
        const pC = coords[stations[i-1]];
        if (!pC) return;
        const prevRow = Math.floor((i-1) / pr);
        const bx = prevRow % 2 === 0 ? pC.x + 24 : pC.x - 24;
        d += ` C ${bx},${pC.y} ${bx},${c.y} ${c.x},${c.y}`;
      } else {
        d += ` L ${c.x} ${c.y}`;
      }
    });
    return d;
  }

  const mainPath = $derived(isLoop ? loopPath() : snakePath());

  const branchPaths = $derived.by(() => {
    if (!isLoop) return [];
    const result = [];
    for (const branch of [
      ['성수','용답','신답','용두','신설동'],
      ['신도림','도림천','양천구청','신정네거리','까치산'],
    ]) {
      const valid = branch.filter(s => coords[s]);
      if (valid.length >= 2)
        result.push(valid.map((s, i) => `${i===0?'M':'L'} ${coords[s].x} ${coords[s].y}`).join(' '));
    }
    return result;
  });

  // ── 열차 위치 보간 ────────────────────────────────────────────────────
  function isUpTrain(train) {
    return train.direction === (LINE_META[lineCode]?.dirLabel[0] ?? '상행');
  }

  function trainXY(train) {
    const idx = stations.indexOf(train.currentStation);
    if (idx < 0) return null;
    const cC = coords[train.currentStation];
    if (!cC) return null;
    const up = isUpTrain(train);
    let prevSt = null;
    if (isLoop) {
      const li = LOOP_STS.indexOf(train.currentStation);
      if (li >= 0) {
        const N = LOOP_STS.length;
        prevSt = up ? LOOP_STS[(li+1)%N] : LOOP_STS[(li-1+N)%N];
      }
    } else {
      const pi = up ? idx+1 : idx-1;
      if (pi >= 0 && pi < stations.length) prevSt = stations[pi];
    }
    if (!prevSt) return { x: cC.x, y: cC.y };
    const pC = coords[prevSt];
    if (!pC) return { x: cC.x, y: cC.y };
    const t = Math.min(1, Math.max(0, (train.etaSeconds ?? 0) / AVG_S));
    return { x: cC.x + (pC.x - cC.x) * t, y: cC.y + (pC.y - cC.y) * t };
  }

  // ── 레이블 위치 ───────────────────────────────────────────────────────
  function labelPos(st) {
    const c = coords[st];
    if (!c) return null;
    if (c.loop) {
      const dx = c.x - CX, dy = c.y - CY;
      const len = Math.sqrt(dx*dx + dy*dy) || 1;
      const OFF = 18;
      return {
        x: c.x + dx/len * OFF, y: c.y + dy/len * OFF,
        anchor: dx > 10 ? 'start' : dx < -10 ? 'end' : 'middle',
        baseline: dy <= 0 ? 'auto' : 'hanging',
      };
    }
    const above = c.row % 2 === 0;
    return { x: c.x, y: c.y + (above ? -11 : 13), anchor: 'middle', baseline: above ? 'auto' : 'hanging' };
  }

  const labelFs = $derived(isLoop ? 10 : (perRow > 12 ? 8 : perRow > 8 ? 9 : 10));

  // ── 팬·줌 상태 ────────────────────────────────────────────────────────
  let scale = $state(1);
  let tx = $state(0), ty = $state(0);
  let container = $state(null);

  function initView() {
    if (!container) return;
    const cw = container.clientWidth  || 390;
    const ch = container.clientHeight || 420;
    const s  = Math.min((cw - 4) / W, (ch - 4) / svgH) * 0.95;
    scale = s;
    tx = (cw - W * s) / 2;
    ty = (ch - svgH * s) / 2;
  }

  // 캔버스 초기화: 컨테이너 마운트 + 노선 변경 시
  $effect(() => {
    stations; lineCode; coords; svgH;
    if (container) requestAnimationFrame(initView);
  });

  // focusStation: 해당 역으로 뷰 이동
  $effect(() => {
    const st = focusStation;
    if (!st || !container) return;
    const c = coords[st];
    if (!c) return;
    const cw = container.clientWidth  || 390;
    const ch = container.clientHeight || 420;
    tx = cw / 2 - c.x * scale;
    ty = ch / 2 - c.y * scale;
  });

  // ── 터치 핸들러 (팬 + 핀치줌) ─────────────────────────────────────────
  let _dragging = false;
  let _lastX = 0, _lastY = 0, _lastDist = 0;

  function onTouchStart(e) {
    if (e.touches.length === 2) {
      _lastDist = dist2(e.touches);
      _dragging  = false;
    } else {
      _dragging = true;
      _lastX = e.touches[0].clientX;
      _lastY = e.touches[0].clientY;
    }
  }

  function onTouchMove(e) {
    if (e.touches.length === 2) {
      const d = dist2(e.touches);
      if (_lastDist > 0) {
        const ratio    = d / _lastDist;
        const newScale = Math.min(6, Math.max(0.3, scale * ratio));
        const rect     = container.getBoundingClientRect();
        const mx = (e.touches[0].clientX + e.touches[1].clientX) / 2 - rect.left;
        const my = (e.touches[0].clientY + e.touches[1].clientY) / 2 - rect.top;
        tx = mx - ((mx - tx) / scale) * newScale;
        ty = my - ((my - ty) / scale) * newScale;
        scale = newScale;
      }
      _lastDist = d;
    } else if (_dragging && e.touches.length === 1) {
      tx += e.touches[0].clientX - _lastX;
      ty += e.touches[0].clientY - _lastY;
      _lastX = e.touches[0].clientX;
      _lastY = e.touches[0].clientY;
    }
  }

  function onTouchEnd(e) {
    if (e.touches.length < 2) _lastDist = 0;
    if (e.touches.length === 0) _dragging = false;
  }

  function onWheel(e) {
    e.preventDefault();
    const delta    = e.deltaY > 0 ? 0.88 : 1.12;
    const newScale = Math.min(6, Math.max(0.3, scale * delta));
    const rect     = container.getBoundingClientRect();
    const mx = e.clientX - rect.left;
    const my = e.clientY - rect.top;
    tx = mx - ((mx - tx) / scale) * newScale;
    ty = my - ((my - ty) / scale) * newScale;
    scale = newScale;
  }

  function dist2(touches) {
    return Math.hypot(
      touches[0].clientX - touches[1].clientX,
      touches[0].clientY - touches[1].clientY,
    );
  }
</script>

<!-- 컨테이너: 고정 높이, 오버플로우 클립, touch-action none -->
<div class="relative rounded-2xl overflow-hidden" style="height: 430px; background: #f8fafc; touch-action: none;"
     bind:this={container}
     ontouchstart={onTouchStart}
     ontouchmove={onTouchMove}
     ontouchend={onTouchEnd}
     onwheel={onWheel}
>
  <!-- 변환 레이어 -->
  <div style="position: absolute; top: 0; left: 0; transform-origin: 0 0; transform: translate({tx}px, {ty}px) scale({scale}); will-change: transform;">
    <svg
      width={W}
      height={svgH}
      viewBox="0 0 {W} {svgH}"
      style="display: block; overflow: visible;"
      role="img"
      aria-label="{lineCode} 실시간 노선도"
    >
      <!-- 메인 트랙 -->
      <path d={mainPath} fill="none" stroke={lineColor} stroke-width="6"
            stroke-linecap="round" stroke-linejoin="round" />

      <!-- 2호선 분기 트랙 -->
      {#each branchPaths as bp}
        <path d={bp} fill="none" stroke={lineColor} stroke-width="5"
              stroke-linecap="round" stroke-linejoin="round" opacity="0.75" />
      {/each}

      <!-- 레이블 (원 아래에 그려지도록 먼저) -->
      {#each stations as st}
        {@const c = coords[st]}
        {@const lp = labelPos(st)}
        {#if c && lp}
          <text
            x={lp.x} y={lp.y}
            text-anchor={lp.anchor}
            dominant-baseline={lp.baseline}
            font-size={labelFs}
            fill="#374151"
            font-weight="600"
            font-family="'Pretendard', 'Noto Sans KR', system-ui, sans-serif"
          >{st}</text>
        {/if}
      {/each}

      <!-- 역 원 -->
      {#each stations as st}
        {@const c = coords[st]}
        {#if c}
          <circle cx={c.x} cy={c.y} r="6" fill="white" stroke={lineColor} stroke-width="2.5" />
        {/if}
      {/each}

      <!-- 열차 -->
      {#each trains as train (train.trainNo)}
        {@const pos = trainXY(train)}
        {#if pos}
          {@const sel = train.trainNo === selectedTrainNo}
          {@const tc  = useMock ? '#F59E0B' : (train.express ? '#EF4444' : lineColor)}
          <g
            role="button"
            tabindex="0"
            onclick={() => onTrainClick(train)}
            onkeydown={(e) => e.key === 'Enter' && onTrainClick(train)}
            style="cursor: pointer;"
            aria-label="열차 {train.trainNo}"
          >
            {#if sel}
              <circle cx={pos.x} cy={pos.y} r="20" fill={tc} opacity="0.15" />
            {/if}
            <!-- 열차 차량 바디 -->
            <rect x={pos.x-11} y={pos.y-15} width="22" height="30" rx="5" fill={tc} />
            <rect x={pos.x-7.5} y={pos.y-13} width="15" height="7"  rx="2.5" fill="white" opacity="0.65" />
            <rect x={pos.x-7.5} y={pos.y-4}  width="15" height="5"  rx="2"   fill="white" opacity="0.45" />
            <rect x={pos.x-7.5} y={pos.y+3}  width="15" height="5"  rx="2"   fill="white" opacity="0.35" />
            {#if sel}
              <rect x={pos.x-13} y={pos.y-17} width="26" height="34" rx="7"
                    fill="none" stroke="white" stroke-width="2.5" />
            {/if}
          </g>
        {/if}
      {/each}
    </svg>
  </div>

  <!-- 피트뷰 리셋 버튼 -->
  <button
    onclick={initView}
    class="absolute bottom-3 right-3 bg-white rounded-full shadow-md active:bg-gray-50 transition-colors flex items-center justify-center"
    style="width: 36px; height: 36px; border: 1px solid #e5e7eb;"
    title="전체 보기"
  >
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#6B7280" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <path d="M3 9V5a2 2 0 012-2h4M15 3h4a2 2 0 012 2v4M3 15v4a2 2 0 002 2h4M15 21h4a2 2 0 002-2v-4"/>
    </svg>
  </button>

  <!-- 줌 힌트 (처음 한 번만) -->
  <div class="absolute bottom-3 left-3 flex items-center gap-1.5 bg-black/40 rounded-full px-2.5 py-1 pointer-events-none"
       style="backdrop-filter: blur(4px);">
    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5">
      <circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>
      <path d="M11 8v6M8 11h6"/>
    </svg>
    <span style="font-size: 10px; color: white; font-weight: 600; line-height: 1;">핀치로 확대</span>
  </div>
</div>
