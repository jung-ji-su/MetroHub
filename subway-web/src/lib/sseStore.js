import { writable } from 'svelte/store';

const API_BASE = import.meta.env.VITE_API_BASE || '';

export const sseConnected       = writable(false);
export const trendingStations   = writable([]);   // [{ stationName, score }]
export const lineAlerts         = writable({});    // lineNumber → { alertType, message, severity }
export const latestCongestionLine = writable(null); // 가장 최근 갱신된 노선코드

let eventSource = null;
let reconnectTimer = null;

export function connectSSE() {
  if (eventSource) return;

  function connect() {
    eventSource = new EventSource(`${API_BASE}/api/sse/stream`);

    eventSource.addEventListener('connected', () => {
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
        // 5분 후 자동 해제
        setTimeout(() => {
          lineAlerts.update(prev => {
            const next = { ...prev };
            delete next[data.lineNumber];
            return next;
          });
        }, 5 * 60 * 1000);
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
      reconnectTimer = setTimeout(connect, 5000); // 5초 후 재연결
    };
  }

  connect();
}

export function disconnectSSE() {
  clearTimeout(reconnectTimer);
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
