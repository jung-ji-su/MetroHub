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

function createFavoritesStore() {
  const initial = browser ? JSON.parse(localStorage.getItem('metrohub_favorites') || '[]') : [];
  const { subscribe, update } = writable(initial);

  function save(list) {
    if (browser) localStorage.setItem('metrohub_favorites', JSON.stringify(list));
  }

  return {
    subscribe,
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
  };
}

function createRoutesStore() {
  const initial = browser ? JSON.parse(localStorage.getItem('metrohub_routes') || '[]') : [];
  const { subscribe, update, set } = writable(initial);

  function save(list) {
    if (browser) localStorage.setItem('metrohub_routes', JSON.stringify(list));
  }

  return {
    subscribe,
    add(from, to) {
      update(list => {
        const id = Date.now();
        const next = [...list, { id, from, to }];
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
    },
  };
}

export const auth      = createAuthStore();
export const token     = derived(auth, ($auth) => $auth?.token ?? null);
export const user      = derived(auth, ($auth) => $auth ? { nickname: $auth.nickname } : null);
export const favorites = createFavoritesStore();
export const routes    = createRoutesStore();
