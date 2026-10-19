const RECENT_KEY = 'search.recent';

/** Last 5 searches, kept in the browser only. Storage may be blocked, so every access is guarded. */
export function loadRecent(): string[] {
  try {
    const v = JSON.parse(localStorage.getItem(RECENT_KEY) ?? '[]') as unknown;
    return Array.isArray(v) ? v.filter((x): x is string => typeof x === 'string').slice(0, 5) : [];
  } catch {
    return [];
  }
}

export function saveRecent(q: string): void {
  try {
    localStorage.setItem(RECENT_KEY, JSON.stringify([q, ...loadRecent().filter((x) => x !== q)].slice(0, 5)));
  } catch {
    /* storage unavailable */
  }
}
