import { readFile, writeFile } from 'node:fs/promises';

export const METRICS = ['lines', 'statements', 'functions', 'branches'];

export function evaluateCoverage(summary) {
  const results = METRICS.map((metric) => {
    const data = summary?.total?.[metric];
    if (!data || !Number.isSafeInteger(data.covered) || !Number.isSafeInteger(data.total)
      || data.covered < 0 || data.total <= 0 || data.covered > data.total) {
      throw new Error(`Coverage report has a missing or invalid ${metric} counter.`);
    }

    const covered = BigInt(data.covered);
    const total = BigInt(data.total);
    return {
      metric,
      covered: data.covered,
      total: data.total,
      percent: Number((data.covered / data.total * 100).toFixed(1)),
      passed: covered * 5n > total * 4n,
    };
  });
  return { passed: results.every((result) => result.passed), results };
}

export function formatSummary(evaluation) {
  const rows = evaluation.results.map(({ metric, covered, total, percent, passed }) =>
    `| ${metric[0].toUpperCase()}${metric.slice(1)} | ${covered}/${total} | ${percent.toFixed(1)}% | >80% | ${passed ? 'PASS' : 'FAIL'} |`);
  return [
    '## Frontend — Playwright E2E coverage',
    '',
    '| Metric | Covered/total | Coverage | Required | Gate |',
    '| --- | ---: | ---: | ---: | :---: |',
    ...rows,
    '',
  ].join('\n');
}

export async function checkCoverage(summaryPath, outputPath) {
  const contents = await readFile(summaryPath, 'utf8');
  const evaluation = evaluateCoverage(JSON.parse(contents));
  const markdown = formatSummary(evaluation);
  await writeFile(outputPath, markdown, 'utf8');
  process.stdout.write(markdown);
  if (!evaluation.passed) throw new Error('Frontend coverage must be strictly greater than 80% for every metric.');
  return evaluation;
}
