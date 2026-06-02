import { writable, get } from 'svelte/store';

const NOTIFICATION_BASE = import.meta.env.VITE_NOTIFICATION_BASE || '';

export const notifications       = writable([]);  // [{ id, type, title, body, createdAt }]
export const unreadCount         = writable(0);
export const notifConnected      = writable(false);
export const notifRetryExhausted = writable(false);

let eventSource    = null;
let reconnectTimer = null;
let retryCount     = 0;
const MAX_RETRIES  = 5;   // 최대 5회 시도 후 중단
const BASE_DELAY   = 5000; // 초기 5초, 이후 10·20·40·80초 (max 90초)

let savedToken = null;

export function connectNotificationSSE(token) {
  if (!token || eventSource) return;
  retryCount = 0;
  savedToken = token;
  notifRetryExhausted.set(false);

  function connect() {
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
  connectNotificationSSE(savedToken);
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
