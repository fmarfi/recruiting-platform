const PREFIX = 'command.recent.';
const LEGACY_KEY = 'command.recent';
const MAX = 8;
const MAX_AGE_MS = 30 * 24 * 3600 * 1000;

export type RecentKind = 'account' | 'person' | 'job';

/** Only the kind and id are stored (KVKK: no names, titles or companies in the browser); labels are fetched when the bar opens. */
export interface RecentRef {
  kind: RecentKind;
  id: string;
  at: number;
}

const KINDS: RecentKind[] = ['account', 'person', 'job'];
const keyFor = (userId: string) => `${PREFIX}${userId}`;

export function recentPath(r: Pick<RecentRef, 'kind' | 'id'>): string {
  return r.kind === 'account' ? `/clients/${r.id}` : r.kind === 'person' ? `/candidates/${r.id}` : `/searches/${r.id}`;
}

/** Per user, newest first, entries older than 30 days dropped. Storage may be blocked, so every access is guarded. */
export function loadRecentRefs(userId: string, now = Date.now()): RecentRef[] {
  try {
    localStorage.removeItem(LEGACY_KEY); // older builds kept titles here
    const v = JSON.parse(localStorage.getItem(keyFor(userId)) ?? '[]') as unknown;
    if (!Array.isArray(v)) return [];
    return v
      .filter((x): x is RecentRef => !!x && typeof x === 'object' && KINDS.includes((x as RecentRef).kind) && typeof (x as RecentRef).id === 'string' && typeof (x as RecentRef).at === 'number' && now - (x as RecentRef).at < MAX_AGE_MS)
      .slice(0, MAX);
  } catch {
    return [];
  }
}

export function pushRecentRef(userId: string, ref: Pick<RecentRef, 'kind' | 'id'>, now = Date.now()): void {
  try {
    const next = [{ ...ref, at: now }, ...loadRecentRefs(userId, now).filter((x) => !(x.kind === ref.kind && x.id === ref.id))].slice(0, MAX);
    localStorage.setItem(keyFor(userId), JSON.stringify(next));
  } catch {
    /* storage unavailable */
  }
}

export function clearRecentRefs(userId: string): void {
  try {
    localStorage.removeItem(keyFor(userId));
  } catch {
    /* storage unavailable */
  }
}
