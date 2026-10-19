import type { Schemas } from '../../pipeline/shared';

export type Activity = Schemas['Activity'];
export type NextAction = Schemas['NextAction'];
export type TypeFilter = 'ALL' | 'CALL' | 'MEETING' | 'NOTE' | 'EMAIL';

/** Filter chips: All plus the activity types people look for most. */
export const FILTERS: TypeFilter[] = ['ALL', 'CALL', 'MEETING', 'NOTE', 'EMAIL'];

export type MonthGroup = { key: string; date: Date; items: Activity[] };

/** Applies the type chip; VISIT counts with meetings. */
export function applyFilter(list: Activity[], f: TypeFilter): Activity[] {
  if (f === 'ALL') return list;
  return list.filter((a) => a.type === f || (f === 'MEETING' && a.type === 'VISIT'));
}

/** Groups newest-first activities by calendar month, keeping the order. */
export function groupByMonth(list: Activity[]): MonthGroup[] {
  const groups: MonthGroup[] = [];
  for (const a of list) {
    const d = new Date(a.occurredAt);
    const key = `${d.getFullYear()}-${d.getMonth()}`;
    const last = groups[groups.length - 1];
    if (last && last.key === key) last.items.push(a);
    else groups.push({ key, date: d, items: [a] });
  }
  return groups;
}
