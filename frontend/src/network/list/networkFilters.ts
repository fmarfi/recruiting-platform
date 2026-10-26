// Network list state <-> URL. The command bar writes the same params: owner=me, stale=N, type=A,B, consent=X, city, company, q.
import { formatSort, parseSort } from '../../shell/grid';
import type { Schemas } from '../../pipeline/shared';
import type { SortState } from '../../shell/grid';

export type ViewFilters = {
  q: string;
  types: Schemas['PersonType'][];
  mine: boolean;
  consent?: Schemas['ConsentStatus'];
  staleDays: number;
  sort: string;
  /** Not API filters: applied client-side to the loaded page only. */
  city: string;
  company: string;
  /** Talent pool filters (1.2.0): tag = pool, channel = source channel, cv = full-text over current CVs, shareable = consent GIVEN. */
  tags: string[];
  channels: Schemas["SourceChannel"][];
  cv: string;
  shareable: boolean;
};

export const DEFAULT_FILTERS: ViewFilters = { q: '', types: [], mine: false, consent: undefined, staleDays: 0, sort: '', city: '', company: '', tags: [], channels: [], cv: '', shareable: false };

export const PERSON_TYPES: Schemas['PersonType'][] = ['EXECUTIVE', 'CLIENT_CONTACT', 'REFERRER', 'CANDIDATE'];
export const CONSENT_STATUSES: Schemas['ConsentStatus'][] = ['NOT_SENT', 'SENT', 'GIVEN', 'REFUSED', 'WITHDRAWN'];
export const SOURCE_CHANNELS: Schemas["SourceChannel"][] = ["LINKEDIN_AD", "LINKEDIN_DIRECT", "WEBSITE", "REFERRAL", "EXISTING_POOL", "EMAIL", "CV_UPLOAD"];
export const MAX_TAG_LENGTH = 40;
export const SORT_FIELDS = ['name', 'company', 'lastContactAt', 'createdAt'];

/** Preset ids double as i18n keys: network.presets.<id>. */
export const PRESETS: { id: string; filters: ViewFilters }[] = [
  { id: 'needsCall', filters: { ...DEFAULT_FILTERS, mine: true, staleDays: 90 } },
  { id: 'noConsent', filters: { ...DEFAULT_FILTERS, consent: 'NOT_SENT' } },
  { id: 'executives', filters: { ...DEFAULT_FILTERS, types: ['EXECUTIVE'] } },
  { id: 'referrers', filters: { ...DEFAULT_FILTERS, types: ['REFERRER'] } },
];

export const sameFilters = (a: ViewFilters, b: ViewFilters) =>
  a.q === b.q &&
  a.mine === b.mine &&
  a.consent === b.consent &&
  a.staleDays === b.staleDays &&
  a.sort === b.sort &&
  a.city === b.city &&
  a.company === b.company &&
  a.cv === b.cv &&
  a.shareable === b.shareable &&
  [...a.tags].sort().join() === [...b.tags].sort().join() &&
  [...a.channels].sort().join() === [...b.channels].sort().join() &&
  [...a.types].sort().join() === [...b.types].sort().join();

/** Unknown values are dropped; repeated owner/stale/consent keep the last value (as the command bar does). */
/** Tags are lower-case (the API matches lower-case), unique and short; anything else is dropped. */
export const normTags = (xs: string[]) => [...new Set(xs.map((x) => x.trim().toLowerCase()).filter((x) => x && x.length <= MAX_TAG_LENGTH))];

export function filtersFromParams(params: URLSearchParams): { filters: ViewFilters; page: number } {
  const last = (k: string) => params.getAll(k).join(',').split(',').pop()?.trim() ?? '';
  const types = [...new Set(params.getAll('type').join(',').split(',').map((x) => x.trim().toUpperCase()))].filter((x): x is Schemas['PersonType'] => (PERSON_TYPES as string[]).includes(x));
  const consent = last('consent').toUpperCase();
  const stale = Number(last('stale'));
  const sort = parseSort(params.get('sort'));
  const page = Number(params.get('page'));
  return {
    filters: {
      q: params.get('q') ?? '',
      types,
      mine: last('owner').toLowerCase() === 'me',
      consent: (CONSENT_STATUSES as string[]).includes(consent) ? (consent as Schemas['ConsentStatus']) : undefined,
      staleDays: Number.isInteger(stale) && stale > 0 ? stale : 0,
      sort: sort && SORT_FIELDS.includes(sort.field) ? formatSort(sort) : '',
      city: params.get('city')?.trim() ?? '',
      company: params.get('company')?.trim() ?? '',
      tags: normTags(params.getAll('tag').join(',').split(',')),
      channels: [...new Set([...params.getAll('sourceChannel'), ...params.getAll('channel')].join(',').split(',').map((x) => x.trim().toUpperCase()))].filter((x): x is Schemas['SourceChannel'] => (SOURCE_CHANNELS as string[]).includes(x)),
      cv: params.get('cv')?.trim() ?? '',
      shareable: params.get('shareable') === 'true',
    },
    page: Number.isInteger(page) && page > 1 ? page - 1 : 0,
  };
}

/** Defaults are left out; page is 1-based. */
export function paramsFromFilters(f: ViewFilters, page: number): URLSearchParams {
  const p = new URLSearchParams();
  if (f.mine) p.set('owner', 'me');
  if (f.staleDays) p.set('stale', String(f.staleDays));
  if (f.types.length) p.set('type', f.types.join(','));
  if (f.consent) p.set('consent', f.consent);
  if (f.city) p.set('city', f.city);
  if (f.company) p.set('company', f.company);
  if (f.q) p.set('q', f.q);
  if (f.tags.length) p.set('tag', f.tags.join(','));
  if (f.channels.length) p.set('sourceChannel', f.channels.join(','));
  if (f.cv) p.set('cv', f.cv);
  if (f.shareable) p.set('shareable', 'true');
  if (f.sort) p.set('sort', f.sort);
  if (page > 0) p.set('page', String(page + 1));
  return p;
}

/** What a saved view stores: filters only, never people (KVKK). */
export const viewSort = (f: ViewFilters): SortState | null => parseSort(f.sort);

const norm = (s: string | null | undefined) => (s ?? '').toLocaleLowerCase('tr');
/** city/company are not API filters yet: narrow the loaded page by substring. */
export function clientFilter<T extends { city?: string | null; company?: string | null }>(rows: T[], f: Pick<ViewFilters, 'city' | 'company'>): T[] {
  const c = norm(f.city);
  const co = norm(f.company);
  return rows.filter((r) => (!c || norm(r.city).includes(c)) && (!co || norm(r.company).includes(co)));
}

/** Key for the grid: a new filter or page remounts it, which clears the row selection (bulk actions must never reach rows that are no longer shown). */
export const gridKey = (f: ViewFilters, page: number) => paramsFromFilters(f, page).toString();
