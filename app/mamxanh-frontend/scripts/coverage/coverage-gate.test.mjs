import test from 'node:test';
import assert from 'node:assert/strict';
import { evaluateCoverage, formatSummary, METRICS } from './coverage-gate.mjs';
import { preserveFailure } from './process-result.mjs';

function summary(overrides = {}) {
  return {
    total: Object.fromEntries(METRICS.map((metric) => [metric, { covered: 81, total: 100 }])),
    ...overrides,
  };
}

test('accepts every FE metric strictly above 80%', () => {
  assert.equal(evaluateCoverage(summary()).passed, true);
});

test('rejects 80% and every value below 80%', () => {
  for (const metric of METRICS) {
    for (const covered of [79, 80]) {
      const result = summary();
      result.total[metric] = { covered, total: 100 };
      assert.equal(evaluateCoverage(result).passed, false, `${metric} at ${covered}%`);
    }
  }
});

test('uses exact covered counts when a passing ratio displays as 80.0%', () => {
  const result = summary();
  result.total.lines = { covered: 8001, total: 10000 };
  const evaluation = evaluateCoverage(result);
  assert.equal(evaluation.passed, true);
  assert.match(formatSummary(evaluation), /80\.0% \| >80% \| PASS/);
});

test('fails closed for missing, malformed and zero-total metric reports', () => {
  assert.throws(() => evaluateCoverage({ total: {} }), /missing or invalid/);
  const zero = summary();
  zero.total.functions = { covered: 0, total: 0 };
  assert.throws(() => evaluateCoverage(zero), /missing or invalid/);
  const impossible = summary();
  impossible.total.statements = { covered: 101, total: 100 };
  assert.throws(() => evaluateCoverage(impossible), /missing or invalid/);
});

test('keeps a browser test failure after a successful coverage report', () => {
  const browserFailure = preserveFailure(null, 'Playwright E2E', { status: 1 });
  assert.match(browserFailure.message, /Playwright E2E exited with code 1/);
  assert.equal(preserveFailure(browserFailure, 'NYC coverage report', { status: 0 }), browserFailure);
});
