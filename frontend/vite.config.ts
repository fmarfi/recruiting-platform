/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Dev: /api/v1/* goes to the Prism mock (default) or the real backend (API_TARGET=http://localhost:8080).
// The mock serves paths without the /api/v1 base, so the prefix is stripped unless API_TARGET is set.
const target = process.env.API_TARGET;

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    fs: { allow: ['..'] }, // theme reads ../contracts/design-tokens.json
    proxy: {
      '/api/v1': target
        ? { target, changeOrigin: true }
        : {
            target: 'http://127.0.0.1:4010',
            changeOrigin: true,
            rewrite: (p) => p.replace(/^\/api\/v1/, ''),
            // Prism has no sessions: pretend every request carries the mock session cookie.
            headers: { Cookie: 'JSESSIONID=mock' },
          },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
  },
});
