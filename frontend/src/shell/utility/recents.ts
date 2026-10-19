// Recently opened records and the quick note, kept in this browser only (no backend, per S5).
import { readStore, writeStore } from '../lib/browserStore';

export interface RecentItem {
  path: string;
  label: string;
}

const RECENT_KEY = 'rp.recentItems';
const NOTE_KEY = 'rp.quickNote';
const MAX_RECENT = 8;

/** Recent records, newest first. */
export function readRecents(): RecentItem[] {
  return readStore<RecentItem[]>(RECENT_KEY, []);
}

/** Moves a record to the top of the recent list and notifies open panels. */
export function rememberRecent(item: RecentItem): void {
  const rest = readRecents().filter((r) => r.path !== item.path);
  writeStore(RECENT_KEY, [item, ...rest].slice(0, MAX_RECENT));
  window.dispatchEvent(new Event('rp-recents'));
}

export const readNote = (): string => readStore<string>(NOTE_KEY, '');
export const writeNote = (text: string): void => writeStore(NOTE_KEY, text);
