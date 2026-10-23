// Smart lists for the network: built-in presets plus views each consultant saves in this browser.
import type { Schemas } from '../../pipeline/shared';

export type ViewFilters = {
  q: string;
  types: Schemas['PersonType'][];
  mine: boolean;
  consent?: Schemas['ConsentStatus'];
  staleDays: number;
  sort: string;
};

export type SavedView = { id: string; name: string; filters: ViewFilters };

export const DEFAULT_FILTERS: ViewFilters = { q: '', types: [], mine: false, consent: undefined, staleDays: 0, sort: '' };

/** Preset ids double as i18n keys: network.presets.<id>. */
export const PRESETS: { id: string; filters: ViewFilters }[] = [
  { id: 'needsCall', filters: { ...DEFAULT_FILTERS, mine: true, staleDays: 90 } },
  { id: 'noConsent', filters: { ...DEFAULT_FILTERS, consent: 'NOT_SENT' } },
  { id: 'executives', filters: { ...DEFAULT_FILTERS, types: ['EXECUTIVE'] } },
  { id: 'referrers', filters: { ...DEFAULT_FILTERS, types: ['REFERRER'] } },
];

const KEY = 'network.savedViews.v1';

export function loadViews(): SavedView[] {
  try {
    const raw = JSON.parse(localStorage.getItem(KEY) ?? '[]') as SavedView[];
    return Array.isArray(raw) ? raw.filter((v) => v && typeof v.id === 'string' && typeof v.name === 'string' && v.filters) : [];
  } catch {
    return [];
  }
}

export function storeViews(views: SavedView[]) {
  try {
    localStorage.setItem(KEY, JSON.stringify(views));
  } catch {
    // private mode or blocked storage: views simply do not persist
  }
}

export const sameFilters = (a: ViewFilters, b: ViewFilters) =>
  a.q === b.q && a.mine === b.mine && a.consent === b.consent && a.staleDays === b.staleDays && a.sort === b.sort && [...a.types].sort().join() === [...b.types].sort().join();
