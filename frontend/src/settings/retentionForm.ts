import type { components } from '../api';

export type RetentionBasis = components['schemas']['RetentionBasis'];
export const RETENTION_BASES: RetentionBasis[] = ['FROM_LAST_CONSENT', 'FROM_LAST_CONTACT', 'FROM_CREATED'];

/** Reminder days from tag input text: whole numbers 1..365, unique, at most 5, largest first. Returns null when something is invalid. */
export function parseReminderDays(values: string[]): number[] | null {
  const nums = values.map((v) => (/^\d{1,3}$/.test(v.trim()) ? Number(v) : NaN));
  if (nums.some((n) => !Number.isInteger(n) || n < 1 || n > 365)) return null;
  const uniq = [...new Set(nums)];
  if (uniq.length > 5) return null;
  return uniq.sort((a, b) => b - a);
}

/**
 * State of the reminder tag input: the raw tags stay visible (so an invalid one can be seen and removed);
 * `error` is derived from them, never stored, so removing the bad tag clears it and saving is possible again.
 */
export function reminderState(raw: string[] | null, saved: number[]): { tags: string[]; error: boolean; days: number[] } {
  if (raw === null) return { tags: saved.map(String), error: false, days: saved };
  const days = parseReminderDays(raw);
  return { tags: raw, error: days === null, days: days ?? saved };
}
