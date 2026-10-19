import { defineConfig, devices } from '@playwright/test';

// BASE_URL: where the UI runs. Default is the QA agent's Vite port (docs/context/wave-1b.md).
// Start the UI yourself and set E2E_NO_SERVER=1, or let this config start Vite from ../../frontend.
// Against the real stack (login, data): start Postgres + Spring, then run with API_TARGET=http://localhost:<spring port>.
const baseURL = process.env.BASE_URL ?? 'http://localhost:5176';
const port = new URL(baseURL).port || '80';

export default defineConfig({
  testDir: './specs',
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL,
    trace: 'retain-on-failure',
    locale: 'en-US',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: process.env.E2E_NO_SERVER
    ? undefined
    : {
        command: `npx vite --port ${port} --strictPort`,
        cwd: '../../frontend',
        url: baseURL,
        reuseExistingServer: true,
        timeout: 60_000,
        env: process.env.API_TARGET ? { API_TARGET: process.env.API_TARGET } : {},
      },
});
