import { defineConfig, devices } from '@playwright/test';

const previewUrl = 'http://127.0.0.1:4173';

export default defineConfig({
  testDir: './tests/e2e',
  retries: 0,
  reporter: [['html', { open: 'never' }]],
  use: {
    baseURL: previewUrl,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  webServer: {
    command: 'npm run preview -- --host 127.0.0.1 --port 4173 --strictPort',
    url: previewUrl,
    reuseExistingServer: false,
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
