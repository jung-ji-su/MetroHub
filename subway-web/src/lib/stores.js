import { writable, derived } from 'svelte/store';
import { browser } from '$app/environment';

function createAuthStore() {
  const initial = browser ? JSON.parse(localStorage.getItem('metrohub_auth') || 'null') : null;
  const { subscribe, set } = writable(initial);

  return {
    subscribe,
    login(data) {
      if (browser) localStorage.setItem('metrohub_auth', JSON.stringify(data));
      set(data);
    },
    logout() {
      if (browser) localStorage.removeItem('metrohub_auth');
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

export const auth      = createAuthStore();
export const token     = derived(auth, ($auth) => $auth?.token ?? null);
export const user      = derived(auth, ($auth) => $auth ? { email: $auth.email, nickname: $auth.nickname } : null);
export const favorites = createFavoritesStore();
