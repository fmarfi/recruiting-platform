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
