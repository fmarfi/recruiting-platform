// Pure helpers of the candidate table: stage filter, counts and sort (default order: stage, then rank).
import type { SortState } from '../../shell/grid/types';
import type { App } from '../longlist/useLonglist';

export const STAGE_ORDER = ['SOURCED', 'SCREENED', 'PRESENTED', 'CLIENT_INTERVIEW', 'OFFER', 'PLACED', 'REJECTED'];
const stageIx = (a: App) => STAGE_ORDER.indexOf(a.stage);
const rankOf = (a: App) => a.rank ?? Number.MAX_SAFE_INTEGER;

export function filterByStage(apps: App[], stages: string[]): App[] {
  return stages.length === 0 ? apps : apps.filter((a) => stages.includes(a.stage));
}

export function countByStage(apps: App[]): Record<string, number> {
  return apps.reduce<Record<string, number>>((m, a) => ({ ...m, [a.stage]: (m[a.stage] ?? 0) + 1 }), {});
}

const KEYS: Record<string, (a: App) => string | number> = {
  name: (a) => a.person.name.toLocaleLowerCase(),
  role: (a) => [a.person.title, a.person.company].filter(Boolean).join(', ').toLocaleLowerCase(),
  stage: stageIx,
  list: (a) => a.list ?? 'ZZ',
  rating: (a) => a.rating ?? 0,
  lastActivity: (a) => a.person.lastContactAt ?? '',
};

export function sortApps(apps: App[], sort: SortState | null): App[] {
  const base = apps.slice().sort((a, b) => stageIx(a) - stageIx(b) || rankOf(a) - rankOf(b));
  const key = sort ? KEYS[sort.field] : undefined;
  if (!sort || !key) return base;
  const dir = sort.dir === 'asc' ? 1 : -1;
  return base.sort((a, b) => {
    const x = key(a);
    const y = key(b);
    return (x < y ? -1 : x > y ? 1 : 0) * dir;
  });
}
