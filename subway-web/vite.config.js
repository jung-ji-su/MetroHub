import tailwindcss from '@tailwindcss/vite';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig({
  plugins: [
    tailwindcss(),
    sveltekit(),
    VitePWA({
      registerType: 'autoUpdate',
      // manifest.json은 static/ 에 직접 작성했으므로 injectManifest 불필요
      manifest: false,
      workbox: {
        // 캐시할 파일 패턴
        globPatterns: ['**/*.{js,css,html,ico,png,svg,woff,woff2}'],
        // SPA 라우팅: 매칭되지 않는 경로는 / 로 폴백
        navigateFallback: '/',
        // API 호출은 서비스 워커 캐시에서 제외
        navigateFallbackDenylist: [/^\/api\//, /^\/actuator\//],
        runtimeCaching: [
          {
            // API 응답: 네트워크 우선, 실패 시 캐시 사용 (최대 10초 대기)
            urlPattern: /\/api\//,
            handler: 'NetworkFirst',
            options: {
              cacheName: 'api-cache',
              networkTimeoutSeconds: 10,
              expiration: { maxEntries: 50, maxAgeSeconds: 60 },
            },
          },
          {
            // 정적 에셋: 캐시 우선
            urlPattern: /\.(?:png|svg|ico|woff2?)$/,
            handler: 'CacheFirst',
            options: {
              cacheName: 'static-assets',
              expiration: { maxEntries: 60, maxAgeSeconds: 60 * 60 * 24 * 30 },
            },
          },
        ],
      },
    }),
  ],
});
