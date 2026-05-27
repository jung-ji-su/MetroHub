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

export const auth = createAuthStore();
export const token = derived(auth, ($auth) => $auth?.token ?? null);
export const user  = derived(auth, ($auth) => $auth ? { email: $auth.email, nickname: $auth.nickname } : null);
