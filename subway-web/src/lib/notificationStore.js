import { writable } from 'svelte/store';

const NOTIFICATION_BASE = import.meta.env.VITE_NOTIFICATION_BASE || '';

export const notifications       = writable([]);  // [{ id, type, title, body, createdAt }]
export const unreadCount         = writable(0);
export const notifConnected      = writable(false);
export const notifRetryExhausted = writable(false);
export const notifAuthError      = writable(false); // 401 발생 시 — layout에서 로그아웃 처리

let eventSource    = null;
let reconnectTimer = null;
let retryCount     = 0;
const MAX_RETRIES  = 5;
const BASE_DELAY   = 5000;

let savedToken   = null;
let onAuthErrCb  = null;

export function connectNotificationSSE(token, { onAuthError } = {}) {
  if (!token || eventSource) return;
  retryCount = 0;
  savedToken = token;
  onAuthErrCb = onAuthError ?? null;
  notifRetryExhausted.set(false);
  notifAuthError.set(false);

  async function connect() {
    // EventSource는 4xx 상태코드를 구분 못함 → fetch로 사전 인증 체크
    try {
      const res = await fetch(
        `${NOTIFICATION_BASE}/api/notifications/my?page=0&size=1`,
        { headers: { Authorization: `Bearer ${savedToken}` } }
      );
      if (res.status === 401) {
        notifAuthError.set(true);
        onAuthErrCb?.();
        return;
      }
    } catch (_) {
      // 네트워크 에러는 무시하고 SSE 연결 시도 (오프라인 상태 등)
    }

    const url = `${NOTIFICATION_BASE}/api/notifications/stream?token=${encodeURIComponent(savedToken)}`;
    eventSource = new EventSource(url);

    eventSource.addEventListener('connected', () => {
      retryCount = 0;
      notifConnected.set(true);
      notifRetryExhausted.set(false);
    });

    eventSource.addEventListener('notification', (e) => {
      try {
        const data = JSON.parse(e.data);
        notifications.update(prev => [{ ...data, createdAt: new Date().toISOString() }, ...prev].slice(0, 50));
        unreadCount.update(n => n + 1);
      } catch (_) {}
    });

    eventSource.onerror = () => {
      notifConnected.set(false);
      eventSource?.close();
      eventSource = null;

      if (retryCount >= MAX_RETRIES) {
        notifRetryExhausted.set(true);
        return;
      }

      const delay = Math.min(BASE_DELAY * Math.pow(2, retryCount), 90000);
      retryCount++;
      reconnectTimer = setTimeout(() => connect(), delay);
    };
  }

  connect();
}

export function retryNotificationSSE() {
  if (!savedToken) return;
  disconnectNotificationSSE();
  connectNotificationSSE(savedToken, { onAuthError: onAuthErrCb ?? undefined });
}

export function disconnectNotificationSSE() {
  clearTimeout(reconnectTimer);
  eventSource?.close();
  eventSource = null;
  notifConnected.set(false);
}

export function markAllRead() {
  unreadCount.set(0);
}

export function clearNotifications() {
  notifications.set([]);
  unreadCount.set(0);
}
