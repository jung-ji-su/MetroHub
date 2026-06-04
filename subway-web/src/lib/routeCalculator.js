import { LINE_STATIONS, LINE_META } from './lineStations.js';

// 노선별 평균 역간 이동 시간 (초)
const INTER_STATION_SECS = {
  '1001': 130,  // 1호선 (경부/경인 외곽 구간 많음)
  '1002': 120,  // 2호선
  '1003': 130,  // 3호선
  '1004': 130,  // 4호선
  '1005': 130,  // 5호선
  '1006': 120,  // 6호선
  '1007': 130,  // 7호선
  '1008': 120,  // 8호선
  '1009': 130,  // 9호선
  '1063': 200,  // 경의중앙선 (역 간격 넓음)
  '1065': 300,  // 공항철도
  '1067': 240,  // 경춘선
  '1069': 120,  // 인천1호선
  '1071': 120,  // 인천2호선
  '1073':  90,  // 의정부경전철
  '1074': 120,  // 김포골드라인
  '1075': 150,  // 신분당선
  '1077': 150,  // 수인분당선
  '1079':  90,  // 에버라인
  '1081': 210,  // 경강선
  '1092':  90,  // 우이신설선
  '1093': 150,  // 서해선
  '1094':  90,  // 신림선
  '1021': 300,  // GTX-A
};
const TRANSFER_SECS = 180;  // 환승 평균 3분

// 세그먼트 배열 → 총 소요 분 (승차 + 환승 시간 합산)
export function calcRouteMins(segments) {
  if (!segments?.length) return 0;
  const rideSecs   = segments.reduce((s, g) => s + (g.durationSecs ?? 0), 0);
  const xferSecs   = (segments.length - 1) * TRANSFER_SECS;
  return Math.max(1, Math.round((rideSecs + xferSecs) / 60));
}

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
  for (const seg of segs) {
    seg.durationSecs = (seg.stations.length - 1) * (INTER_STATION_SECS[seg.line] ?? 120);
  }
  return segs;
}
