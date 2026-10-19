// Tiny wrapper over localStorage for per-user UI preferences (densities, saved views, recents).
// Storage can be blocked or full, so every access is guarded and falls back to the default.

/** Reads a JSON value stored under `key`, or `fallback` when missing or unreadable. */
export function readStore<T>(key: string, fallback: T): T {
  try {
    const raw = window.localStorage.getItem(key);
    return raw === null ? fallback : (JSON.parse(raw) as T);
  } catch {
    return fallback;
  }
}

/** Stores a JSON value under `key`; silently ignores a blocked or full storage. */
export function writeStore<T>(key: string, value: T): void {
  try {
    window.localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // Preference is lost for this session only; nothing else depends on it.
  }
}
