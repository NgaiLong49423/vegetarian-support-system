import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests/integration',
  testMatch: '**/*.spec.ts',
  retries: 0,
  reporter: 'list',
  timeout: 120_000,
  use: {
    baseURL: process.env.MAMXANX_E2E_FRONTEND_URL ?? 'http://localhost:5173',
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium-real-stack', use: { ...devices['Desktop Chrome'] } }],
});
