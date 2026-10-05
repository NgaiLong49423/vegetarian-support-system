import { spawnSync } from 'node:child_process';
import { mkdir, rm } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { checkCoverage } from './coverage/coverage-gate.mjs';
import { preserveFailure } from './coverage/process-result.mjs';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const executable = (path) => resolve(root, 'node_modules', ...path.split('/'));
const run = (label, args, env = process.env) => {
  process.stdout.write(`\n> ${label}\n`);
  return spawnSync(process.execPath, args, { cwd: root, env, stdio: 'inherit' });
};

await rm(resolve(root, '.nyc_output'), { recursive: true, force: true });
await rm(resolve(root, 'coverage'), { recursive: true, force: true });
await mkdir(resolve(root, 'coverage'), { recursive: true });

let failure;
const build = run('vite build (coverage instrumentation)', [executable('vite/bin/vite.js'), 'build'], {
  ...process.env,
  VITE_COVERAGE: 'true',
});
failure = preserveFailure(failure, 'Instrumented frontend build', build);
if (!failure) {
  const browser = run('Playwright E2E', [executable('@playwright/test/cli.js'), 'test']);
  failure = preserveFailure(failure, 'Playwright E2E', browser);
}

const report = run('NYC coverage report', [
  executable('nyc/bin/nyc.js'), 'report', '--check-coverage=false',
  '--reporter=text-summary', '--reporter=html', '--reporter=lcov', '--reporter=json-summary',
]);
failure = preserveFailure(failure, 'NYC report', report);

try {
  await checkCoverage(resolve(root, 'coverage/coverage-summary.json'), resolve(root, 'coverage/strict-summary.md'));
} catch (error) {
  failure ??= error;
  process.stderr.write(`Coverage gate unavailable or failed: ${error.message}\n`);
}

if (failure) {
  process.stderr.write(`${failure.message}\n`);
  process.exitCode = 1;
}
