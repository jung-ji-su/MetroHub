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
  } = $props();

  // ── canvas constants ──────────────────────────────────────────────────
  const W = 340, MX = 26, TX = W - 2 * MX;
  const AVG_S = 90;

  // ── 2호선 loop data ───────────────────────────────────────────────────
  const LOOP_STS = [
    '시청','을지로입구','을지로3가','을지로4가','동대문역사문화공원',
    '신당','상왕십리','왕십리','한양대','뚝섬','성수','건대입구','구의',
    '강변','잠실나루','잠실','잠실새내','종합운동장','삼성','선릉','역삼',
    '강남','교대','서초','방배','사당','낙성대','서울대입구','봉천','신림',
    '신대방','구로디지털단지','대림','신도림','문래','영등포구청','당산',
    '합정','홍대입구','신촌','이대','아현','충정로',
  ];
  const CX = 175, CY = 210, RX = 110, RY = 155;

  // ── line shape detection ──────────────────────────────────────────────
  const isLoop = $derived(lineCode === '1002');

  // ── snake layout helpers ──────────────────────────────────────────────
  function snakePerRow(n) {
    if (n <= 8)  return n;
    if (n <= 16) return Math.ceil(n / 2);
    if (n <= 30) return 10;
    if (n <= 50) return 12;
    return 14;
  }

  const perRow = $derived(isLoop ? LOOP_STS.length : snakePerRow(stations.length));
  const rowH   = $derived(stations.length <= 14 ? 74 : 66);

  // ── coordinate maps ───────────────────────────────────────────────────
  function coords2Gosen() {
    const m = {};
    const N = LOOP_STS.length;
    LOOP_STS.forEach((st, i) => {
      const a = -Math.PI / 2 + (i / N) * 2 * Math.PI;
      m[st] = { x: CX + RX * Math.cos(a), y: CY + RY * Math.sin(a), loop: true, loopIdx: i };
    });
    // 성수지선: 성수(i=10) → 용답·신답·용두·신설동 (SE direction)
    const sC = m['성수'];
    if (sC) ['용답','신답','용두','신설동'].forEach((st, i) => {
      m[st] = { x: sC.x + (i+1)*12, y: sC.y + (i+1)*24, loop: false };
    });
    // 신정지선: 신도림(i=33) → 도림천·양천구청·신정네거리·까치산 (SW direction)
    const dC = m['신도림'];
    if (dC) ['도림천','양천구청','신정네거리','까치산'].forEach((st, i) => {
      m[st] = { x: dC.x - (i+1)*10, y: dC.y + (i+1)*28, loop: false };
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
        y: 30 + row * rh,
        row, pos, goR,
      };
    });
    return m;
  }

  const coords = $derived.by(() => isLoop ? coords2Gosen() : coordsSnake());

  // ── canvas height ─────────────────────────────────────────────────────
  const canvasH = $derived.by(() => {
    if (isLoop) {
      const ys = Object.values(coords).map(c => c.y);
      return Math.max(...ys) + 42;
    }
    const rows = Math.ceil(stations.length / perRow);
    return 30 + rows * rowH + 40;
  });

  // ── SVG track path ────────────────────────────────────────────────────
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
        const bx = prevRow % 2 === 0 ? pC.x + 22 : pC.x - 22;
        d += ` C ${bx},${pC.y} ${bx},${c.y} ${c.x},${c.y}`;
      } else {
        d += ` L ${c.x} ${c.y}`;
      }
    });
    return d;
  }

  const mainPath = $derived(isLoop ? loopPath() : snakePath());

  // 2호선 branch paths (rendered on top of main loop)
  const branchPaths = $derived.by(() => {
    if (!isLoop) return [];
    const result = [];
    for (const branch of [
      ['성수','용답','신답','용두','신설동'],
      ['신도림','도림천','양천구청','신정네거리','까치산'],
    ]) {
      const valid = branch.filter(s => coords[s]);
      if (valid.length >= 2) {
        result.push(valid.map((s, i) => `${i===0?'M':'L'} ${coords[s].x} ${coords[s].y}`).join(' '));
      }
    }
    return result;
  });

  // ── train position interpolation ──────────────────────────────────────
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
        prevSt = up ? LOOP_STS[(li + 1) % N] : LOOP_STS[(li - 1 + N) % N];
      }
    } else {
      const pi = up ? idx + 1 : idx - 1;
      if (pi >= 0 && pi < stations.length) prevSt = stations[pi];
    }

    if (!prevSt) return { x: cC.x, y: cC.y };
    const pC = coords[prevSt];
    if (!pC) return { x: cC.x, y: cC.y };

    const t = Math.min(1, Math.max(0, (train.etaSeconds ?? 0) / AVG_S));
    return { x: cC.x + (pC.x - cC.x) * t, y: cC.y + (pC.y - cC.y) * t };
  }

  // ── label placement ───────────────────────────────────────────────────
  function labelPos(st) {
    const c = coords[st];
    if (!c) return null;

    if (c.loop) {
      const dx = c.x - CX, dy = c.y - CY;
      const len = Math.sqrt(dx*dx + dy*dy) || 1;
      const OFF = 15;
      return {
        x: c.x + dx/len * OFF,
        y: c.y + dy/len * OFF,
        anchor: dx > 8 ? 'start' : dx < -8 ? 'end' : 'middle',
        baseline: dy <= 0 ? 'auto' : 'hanging',
      };
    }
    // snake: above for even rows, below for odd rows
    const above = c.row % 2 === 0;
    return { x: c.x, y: c.y + (above ? -10 : 12), anchor: 'middle', baseline: above ? 'auto' : 'hanging' };
  }

  const labelFs = $derived(perRow > 12 ? 7 : perRow > 8 ? 8 : 9);
</script>

<svg
  viewBox="0 0 {W} {canvasH}"
  style="width: 100%; height: auto; display: block; overflow: visible;"
  role="img"
  aria-label="{lineCode} 실시간 노선도"
>
  <!-- main track -->
  <path
    d={mainPath}
    fill="none"
    stroke={lineColor}
    stroke-width="5"
    stroke-linecap="round"
    stroke-linejoin="round"
  />

  <!-- 2호선 branch tracks -->
  {#each branchPaths as bp}
    <path
      d={bp}
      fill="none"
      stroke={lineColor}
      stroke-width="4.5"
      stroke-linecap="round"
      stroke-linejoin="round"
      opacity="0.75"
    />
  {/each}

  <!-- stations: labels first (behind circles), then circles -->
  {#each stations as st}
    {@const c = coords[st]}
    {@const lp = labelPos(st)}
    {#if c && lp}
      <text
        x={lp.x}
        y={lp.y}
        text-anchor={lp.anchor}
        dominant-baseline={lp.baseline}
        font-size={labelFs}
        fill="#374151"
        font-weight="500"
        font-family="'Pretendard', 'Noto Sans KR', system-ui, sans-serif"
      >{st}</text>
    {/if}
  {/each}
  {#each stations as st}
    {@const c = coords[st]}
    {#if c}
      <circle cx={c.x} cy={c.y} r="5" fill="white" stroke={lineColor} stroke-width="2" />
    {/if}
  {/each}

  <!-- trains -->
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
        aria-label="열차 {train.trainNo} {train.direction} {train.destination ?? ''}"
      >
        {#if sel}
          <circle cx={pos.x} cy={pos.y} r="17" fill={tc} opacity="0.16" />
        {/if}

        <!-- train body (top-view subway car) -->
        <rect x={pos.x-9}  y={pos.y-13} width="18" height="26" rx="4" fill={tc} />
        <rect x={pos.x-6}  y={pos.y-11} width="12" height="6"  rx="2" fill="white" opacity="0.6" />
        <rect x={pos.x-6}  y={pos.y-3}  width="12" height="4"  rx="1.5" fill="white" opacity="0.4" />
        <rect x={pos.x-6}  y={pos.y+3}  width="12" height="4"  rx="1.5" fill="white" opacity="0.3" />

        <!-- selection ring -->
        {#if sel}
          <rect x={pos.x-11} y={pos.y-15} width="22" height="30" rx="6"
                fill="none" stroke="white" stroke-width="2.5" />
        {/if}
      </g>
    {/if}
  {/each}
</svg>
