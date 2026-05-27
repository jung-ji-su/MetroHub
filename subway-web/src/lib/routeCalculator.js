import { LINE_STATIONS, LINE_META } from './lineStations.js';

// 역명 → 노선코드 목록
const STATION_LINES = {};
for (const [code, stations] of Object.entries(LINE_STATIONS)) {
  for (const s of stations) {
    if (!STATION_LINES[s]) STATION_LINES[s] = [];
    if (!STATION_LINES[s].includes(code)) STATION_LINES[s].push(code);
  }
}

export const ALL_STATIONS = Object.keys(STATION_LINES).sort();

export function getStationLines(name) {
  return STATION_LINES[name] ?? [];
}

// 검색어로 역 목록 필터링 (최대 12개)
export function searchStations(query) {
  if (!query?.trim()) return [];
  const q = query.trim();
  return ALL_STATIONS
    .filter(s => s.includes(q))
    .slice(0, 12)
    .map(s => ({ name: s, lines: STATION_LINES[s] }));
}

// 0-1 BFS: 같은 노선 이동 = 비용 0, 환승 = 비용 1
// 반환: [{ line, lineName, lineColor, stations: [] }] 세그먼트 배열, 또는 null
export function findRoute(from, to) {
  if (!from || !to || from === to) return null;
  if (!STATION_LINES[from] || !STATION_LINES[to]) return null;

  const dist = new Map();
  const prev = new Map();
  const deque = [];

  for (const line of STATION_LINES[from]) {
    const key = `${from}:${line}`;
    dist.set(key, 0);
    prev.set(key, null);
    deque.push({ station: from, line, transfers: 0 });
  }

  while (deque.length > 0) {
    const { station, line, transfers } = deque.shift();
    const key = `${station}:${line}`;
    if ((dist.get(key) ?? Infinity) < transfers) continue;

    if (station === to) return buildSegments(prev, key);

    const ls = LINE_STATIONS[line];
    if (!ls) continue;
    const idx = ls.indexOf(station);
    if (idx < 0) continue;

    // 인접 역 (비용 0) → 앞에 삽입
    for (const delta of [-1, 1]) {
      const ni = idx + delta;
      if (ni < 0 || ni >= ls.length) continue;
      const ns = ls[ni];
      const nk = `${ns}:${line}`;
      if (transfers < (dist.get(nk) ?? Infinity)) {
        dist.set(nk, transfers);
        prev.set(nk, { key, transfer: false });
        deque.unshift({ station: ns, line, transfers });
      }
    }

    // 환승 (비용 1) → 뒤에 삽입
    for (const otherLine of (STATION_LINES[station] ?? [])) {
      if (otherLine === line) continue;
      const ok = `${station}:${otherLine}`;
      if (transfers + 1 < (dist.get(ok) ?? Infinity)) {
        dist.set(ok, transfers + 1);
        prev.set(ok, { key, transfer: true });
        deque.push({ station, line: otherLine, transfers: transfers + 1 });
      }
    }
  }
  return null;
}

function buildSegments(prev, finalKey) {
  const raw = [];
  let k = finalKey;
  while (k != null) {
    const [station, line] = k.split(':');
    raw.push({ station, line });
    const p = prev.get(k);
    k = p ? p.key : null;
  }
  raw.reverse();

  const segs = [];
  let cur = null;
  for (const { station, line } of raw) {
    if (!cur || cur.line !== line) {
      if (cur) segs.push(cur);
      cur = {
        line,
        lineName: LINE_META[line]?.name ?? line,
        lineColor: LINE_META[line]?.color ?? '#6B7280',
        stations: [station],
      };
    } else {
      cur.stations.push(station);
    }
  }
  if (cur) segs.push(cur);
  return segs;
}
