/** Server problem detail text, if the body looks like ProblemDetail. */
export function problemText(error: unknown, fallback: string): string {
  const e = error as { detail?: string; title?: string } | undefined;
  return e?.detail ?? e?.title ?? fallback;
}
