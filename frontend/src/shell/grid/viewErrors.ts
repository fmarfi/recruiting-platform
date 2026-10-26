/** Why a view operation failed, mapped from the /views problem responses. */
export type ViewErrorCode = 'DUPLICATE' | 'LIMIT' | 'CONFLICT' | 'FORBIDDEN' | 'NOT_FOUND' | 'VALIDATION' | 'UNAUTHORIZED' | 'UNAVAILABLE';

export class ViewStoreError extends Error {
  constructor(
    readonly code: ViewErrorCode,
    readonly status = 0,
  ) {
    super(code);
    this.name = 'ViewStoreError';
  }
}

interface ProblemBody {
  code?: string;
  errors?: { field?: string; code?: string }[];
}

/** Pure mapping of an HTTP status and ProblemDetail body (ADR 0010: 409 DUPLICATE, 400 LIMIT, 409 VERSION_CONFLICT). */
export function mapViewError(status: number, body: unknown): ViewStoreError {
  const b = (body ?? {}) as ProblemBody;
  if (status === 400) return new ViewStoreError(b.errors?.some((e) => e.code === 'LIMIT') ? 'LIMIT' : 'VALIDATION', status);
  if (status === 401) return new ViewStoreError('UNAUTHORIZED', status);
  if (status === 403) return new ViewStoreError('FORBIDDEN', status);
  if (status === 404) return new ViewStoreError('NOT_FOUND', status);
  if (status === 409) return new ViewStoreError(b.code === 'DUPLICATE' ? 'DUPLICATE' : 'CONFLICT', status);
  return new ViewStoreError('UNAVAILABLE', status);
}

export const viewErrorCode = (e: unknown): ViewErrorCode | null => (e instanceof ViewStoreError ? e.code : null);
