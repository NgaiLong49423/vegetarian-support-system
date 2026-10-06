export function preserveFailure(previous, label, result) {
  if (!result.error && result.status === 0) return previous;
  return previous ?? result.error ?? new Error(`${label} exited with code ${result.status}.`);
}
