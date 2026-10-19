// Helpers for RFC 7807 bodies returned by openapi-fetch.
/** Message of an RFC 7807 body returned by openapi-fetch (`error`), or a fallback. */
export function problemText(error: unknown, fallback: string): string {
  const e = error as { detail?: string; title?: string } | undefined;
  return e?.detail ?? e?.title ?? fallback;
}
export function problemCode(error: unknown): string | undefined {
  return (error as { code?: string } | undefined)?.code;
}

