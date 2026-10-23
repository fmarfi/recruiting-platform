const KEY = 'command.recent';
const MAX = 8;

export interface RecentItem {
  to: string;
  title: string;
  sub?: string;
}

/** Records opened from the command bar, kept in this browser only. Storage may be blocked, so every access is guarded. */
export function loadRecentItems(): RecentItem[] {
  try {
    const v = JSON.parse(localStorage.getItem(KEY) ?? '[]') as unknown;
    return Array.isArray(v)
      ? v.filter((x): x is RecentItem => !!x && typeof x === 'object' && typeof (x as RecentItem).to === 'string' && typeof (x as RecentItem).title === 'string').slice(0, MAX)
      : [];
  } catch {
    return [];
  }
}

export function pushRecentItem(item: RecentItem): void {
  try {
    localStorage.setItem(KEY, JSON.stringify([item, ...loadRecentItems().filter((x) => x.to !== item.to)].slice(0, MAX)));
  } catch {
    /* storage unavailable */
  }
}
