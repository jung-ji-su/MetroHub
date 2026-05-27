import { writable, get } from 'svelte/store';

const NOTIFICATION_BASE = import.meta.env.VITE_NOTIFICATION_BASE || '';

export const notifications  = writable([]);  // [{ id, type, title, body, createdAt }]
export const unreadCount    = writable(0);
export const notifConnected = writable(false);

let eventSource    = null;
let reconnectTimer = null;

export function connectNotificationSSE(token) {
  if (!token || eventSource) return;

  function connect() {
    const url = `${NOTIFICATION_BASE}/api/notifications/stream?token=${encodeURIComponent(token)}`;
    eventSource = new EventSource(url);

    eventSource.addEventListener('connected', () => {
      notifConnected.set(true);
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
      reconnectTimer = setTimeout(() => connect(), 10000);
    };
  }

  connect();
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
