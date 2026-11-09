import { defineConfig, devices } from '@playwright/test';

const baseURL = process.env.MAMXANH_AI_AGENT_BASE_URL ?? 'http://localhost:5173';

export default defineConfig({
  testDir: './tests/e2e',
  testMatch: 'ai-agent-live.spec.ts',
  workers: 1,
  fullyParallel: false,
  retries: 0,
  timeout: 150_000,
  expect: { timeout: 15_000 },
  outputDir: process.env.MAMXANH_AI_AGENT_EVIDENCE_DIR
    ?? '../../.agents/outputs/ai-chatbot-agent-test-scenarios/evidence/live-playwright-results',
  reporter: [['list'], ['html', { outputFolder: 'playwright-agent-live-report', open: 'never' }]],
  use: {
    ...devices['Desktop Chrome'],
    baseURL,
    screenshot: 'only-on-failure',
    trace: 'off',
  },
});
