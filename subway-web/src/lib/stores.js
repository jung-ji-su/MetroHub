import { writable, derived } from 'svelte/store';
import { browser } from '$app/environment';

function createAuthStore() {
  const initial = browser ? JSON.parse(localStorage.getItem('metrohub_auth') || 'null') : null;
  const { subscribe, set, update } = writable(initial);

  return {
    subscribe,
    login(data) {
      if (browser) localStorage.setItem('metrohub_auth', JSON.stringify(data));
      set(data);
    },
    logout() {
      if (browser) {
        const stored = JSON.parse(localStorage.getItem('metrohub_auth') || 'null');
        // refresh token 서버측 무효화 (fire-and-forget)
        if (stored?.refreshToken) {
          fetch('/api/users/logout', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken: stored.refreshToken })
          }).catch(() => {});
        }
        localStorage.removeItem('metrohub_auth');
      }
      set(null);
    }
  };
}

function createPerUserStore(storeName, initialValue, buildActions) {
  const { subscribe, set, update } = writable(initialValue);
  let currentKey = null;

  if (browser) {
    auth.subscribe($auth => {
      const nickname = $auth?.nickname ?? null;
      currentKey = nickname ? `metrohub_${storeName}_${nickname}` : null;
      const data = currentKey
        ? JSON.parse(localStorage.getItem(currentKey) || JSON.stringify(initialValue))
        : initialValue;
      set(data);
    });
  }

  function save(list) {
    if (browser && currentKey) localStorage.setItem(currentKey, JSON.stringify(list));
  }

  return { subscribe, ...buildActions(update, save) };
}

function createFavoritesStore() {
  return createPerUserStore('favorites', [], (update, save) => ({
    add(station) {
      update(list => {
        if (list.includes(station)) return list;
        const next = [...list, station];
        save(next);
        return next;
      });
    },
    remove(station) {
      update(list => {
        const next = list.filter(s => s !== station);
        save(next);
        return next;
      });
    }
  }));
}

function createRoutesStore() {
  return createPerUserStore('routes', [], (update, save) => ({
    add(from, to) {
      update(list => {
        const next = [...list, { id: Date.now(), from, to }];
        save(next);
        return next;
      });
    },
    remove(id) {
      update(list => {
        const next = list.filter(r => r.id !== id);
        save(next);
        return next;
      });
    }
  }));
}

export const auth      = createAuthStore();
export const token     = derived(auth, ($auth) => $auth?.token ?? null);
export const user      = derived(auth, ($auth) => $auth ? { nickname: $auth.nickname, role: $auth.role } : null);
export const favorites = createFavoritesStore();
export const routes    = createRoutesStore();
