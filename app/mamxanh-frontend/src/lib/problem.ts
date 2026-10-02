import { isAxiosError } from 'axios';

export type FieldError = { field: string; message: string };

/** Error body returned by the backend (`application/problem+json`, docs/api/API.md section 4). */
export type ProblemDetails = {
  status: number;
  code: string;
  title?: string;
  detail?: string;
  errors?: FieldError[];
};

/** Returns the ProblemDetail of a failed request, or null for network errors and unexpected bodies. */
export function toProblem(error: unknown): ProblemDetails | null {
  if (!isAxiosError(error) || !error.response) return null;
  const data = error.response.data as Partial<ProblemDetails> | undefined;
  if (!data || typeof data.code !== 'string') return null;
  return { ...data, status: error.response.status, code: data.code };
}

/** Seconds from the `Retry-After` header of a 429 response, or null when absent. */
export function retryAfterSeconds(error: unknown): number | null {
  if (!isAxiosError(error)) return null;
  const seconds = Number(error.response?.headers?.['retry-after']);
  return Number.isFinite(seconds) && seconds > 0 ? seconds : null;
}

/** Groups field errors by field name, joining several messages for the same field. */
export function fieldMessages(problem: ProblemDetails): Record<string, string> {
  const grouped: Record<string, string> = {};
  for (const { field, message } of problem.errors ?? []) {
    grouped[field] = grouped[field] ? `${grouped[field]} ${message}` : message;
  }
  return grouped;
}
