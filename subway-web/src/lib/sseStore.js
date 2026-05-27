import { writable } from 'svelte/store';

const API_BASE = import.meta.env.VITE_API_BASE || '';

export const sseConnected         = writable(false);
export const trendingStations     = writable([]);   // [{ stationName, score }]
export const lineAlerts           = writable({});    // lineNumber → { alertType, message, severity }
export const latestCongestionLine = writable(null); // 가장 최근 갱신된 노선코드

let eventSource    = null;
let reconnectTimer = null;
let retryCount     = 0;
const MAX_RETRIES  = 8;
const BASE_DELAY   = 3000; // 3·6·12·24·48·90·90·90초 (최대 90초)

// 경고 자동해제 타이머 목록 — disconnectSSE 시 일괄 정리
const alertTimers = [];

export function connectSSE() {
  if (eventSource) return;
  retryCount = 0;

  function connect() {
    eventSource = new EventSource(`${API_BASE}/api/sse/stream`);

    eventSource.addEventListener('connected', () => {
      retryCount = 0;
      sseConnected.set(true);
    });

    eventSource.addEventListener('congestion.updated', (e) => {
      try {
        const data = JSON.parse(e.data);
        latestCongestionLine.set(data.lineNumber);
      } catch (_) {}
    });

    eventSource.addEventListener('line.alert', (e) => {
      try {
        const data = JSON.parse(e.data);
        lineAlerts.update(prev => ({ ...prev, [data.lineNumber]: data }));
        // 5분 후 자동 해제 — 타이머를 목록에 등록해 disconnect 시 정리
        const t = setTimeout(() => {
          lineAlerts.update(prev => {
            const next = { ...prev };
            delete next[data.lineNumber];
            return next;
          });
        }, 5 * 60 * 1000);
        alertTimers.push(t);
      } catch (_) {}
    });

    eventSource.addEventListener('trending.updated', (e) => {
      try {
        const data = JSON.parse(e.data);
        trendingStations.set(data.stations ?? []);
      } catch (_) {}
    });

    eventSource.onerror = () => {
      sseConnected.set(false);
      eventSource?.close();
      eventSource = null;

      if (retryCount >= MAX_RETRIES) return; // 최대 재시도 후 중단

      const delay = Math.min(BASE_DELAY * Math.pow(2, retryCount), 90_000);
      retryCount++;
      reconnectTimer = setTimeout(connect, delay);
    };
  }

  connect();
}

export function disconnectSSE() {
  clearTimeout(reconnectTimer);
  // 누적된 경고 타이머 전부 정리
  for (const t of alertTimers.splice(0)) clearTimeout(t);
  eventSource?.close();
  eventSource = null;
  sseConnected.set(false);
}

export function dismissAlert(lineNumber) {
  lineAlerts.update(prev => {
    const next = { ...prev };
    delete next[lineNumber];
    return next;
  });
}
