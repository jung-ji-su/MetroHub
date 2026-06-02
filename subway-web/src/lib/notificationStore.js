import { writable } from 'svelte/store';

const NOTIFICATION_BASE = import.meta.env.VITE_NOTIFICATION_BASE || '';

export const notifications       = writable([]);  // [{ id, type, title, body, createdAt }]
export const unreadCount         = writable(0);
export const notifConnected      = writable(false);
export const notifRetryExhausted = writable(false);
export const notifAuthError      = writable(false); // 401 — 알림 SSE 중단 (로그아웃은 하지 않음)

let eventSource    = null;
let reconnectTimer = null;
let retryCount     = 0;
const MAX_RETRIES  = 5;
const BASE_DELAY   = 5000;

let savedToken = null;

export function connectNotificationSSE(token) {
  if (!token || eventSource) return;
  retryCount = 0;
  savedToken = token;
  notifRetryExhausted.set(false);
  notifAuthError.set(false);

  async function connect() {
    // 단회용 streamToken 발급 (JWT를 SSE URL에 노출하지 않음)
    let streamToken;
    try {
      const res = await fetch(
        `${NOTIFICATION_BASE}/api/notifications/stream-token`,
        { method: 'POST', headers: { Authorization: `Bearer ${savedToken}` } }
      );
      if (res.status === 401) {
        notifAuthError.set(true);
        return;
      }
      if (!res.ok) throw new Error('stream-token 발급 실패');
      const data = await res.json();
      streamToken = data.streamToken;
    } catch (_) {
      // 네트워크 에러는 무시하고 재시도 흐름으로
    }

    if (!streamToken) {
      const delay = Math.min(BASE_DELAY * Math.pow(2, retryCount), 90000);
      retryCount++;
      reconnectTimer = setTimeout(() => connect(), delay);
      return;
    }

    const url = `${NOTIFICATION_BASE}/api/notifications/stream?streamToken=${encodeURIComponent(streamToken)}`;
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
  connectNotificationSSE(savedToken);
}

export function disconnectNotificationSSE() {
  clearTimeout(reconnectTimer);
  eventSource?.close();
  eventSource = null;
  notifConnected.set(false);
}

export async function markAllRead(token) {
  unreadCount.set(0);
  if (!token) return;
  try {
    const { api } = await import('./api');
    await api.markAllNotificationsRead(token);
  } catch (_) {}
}

export function clearNotifications() {
  notifications.set([]);
  unreadCount.set(0);
}
