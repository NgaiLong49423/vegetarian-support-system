import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { expect, test as base } from '@playwright/test';

type IstanbulCoverage = Record<string, unknown>;

declare global {
  interface Window {
    __coverage__?: IstanbulCoverage;
    __pwCollectCoverage?: (coverage: IstanbulCoverage) => Promise<void>;
  }
}

export const test = base.extend({
  context: async ({ context }, use, testInfo) => {
    let snapshot = 0;
    const pendingWrites = new Set<Promise<void>>();
    const coverageDirectory = path.join(process.cwd(), '.nyc_output');
    const safeTestId = testInfo.testId.replace(/[^a-zA-Z0-9_-]/g, '_');

    await context.exposeBinding('__pwCollectCoverage', async (_source, coverage: IstanbulCoverage) => {
      const filename = `${safeTestId}-${testInfo.workerIndex}-${testInfo.retry}-${snapshot++}.json`;
      const write = mkdir(coverageDirectory, { recursive: true })
        .then(() => writeFile(path.join(coverageDirectory, filename), JSON.stringify(coverage)));
      pendingWrites.add(write);
      try {
        await write;
      } finally {
        pendingWrites.delete(write);
      }
    });

    await context.addInitScript(() => {
      window.addEventListener('beforeunload', () => {
        const coverage = window.__coverage__;
        if (coverage && window.__pwCollectCoverage) {
          void window.__pwCollectCoverage(coverage).catch(() => undefined);
        }
      });
    });

    await use(context);

    for (const page of context.pages()) {
      try {
        await page.evaluate(async () => {
          if (window.__coverage__ && window.__pwCollectCoverage) {
            await window.__pwCollectCoverage(window.__coverage__);
          }
        });
      } catch {
        // A page may already be closing; its beforeunload snapshot is still collected.
      }
    }
    await Promise.all(pendingWrites);
  },
});

export { expect };
