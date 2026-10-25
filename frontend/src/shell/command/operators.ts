// Pure parser for the command-bar operator syntax: owner:me stale:90 city:izmir type:exec consent:given company:"Y Holding" status:active
// Unknown operators, empty or invalid values stay in the free text, so typing never loses input.

export type OperatorKey = 'owner' | 'stale' | 'city' | 'type' | 'consent' | 'company' | 'status';

export interface ParsedOperator {
  key: OperatorKey;
  /** Normalized value: enum constant, "me", digits, or the typed text (city, company). */
  value: string;
  /** Position of the token in the input, so a chip can remove itself. */
  start: number;
  end: number;
}

export interface ParsedQuery {
  operators: ParsedOperator[];
  /** Everything that is not a recognised operator, quotes removed from quoted phrases. */
  text: string;
}

const fold = (s: string) => s.replace(/[\u0131\u0130]/g, 'i').toLowerCase().trim();
const constant = (s: string) => fold(s).replace(/[\s-]+/g, '_').toUpperCase();

const TYPE_ALIASES: Record<string, string> = {
  exec: 'EXECUTIVE', executive: 'EXECUTIVE',
  client: 'CLIENT_CONTACT', contact: 'CLIENT_CONTACT', client_contact: 'CLIENT_CONTACT',
  referrer: 'REFERRER', ref: 'REFERRER',
  candidate: 'CANDIDATE', cand: 'CANDIDATE',
};
const CONSENT_ALIASES: Record<string, string> = {
  given: 'GIVEN', yes: 'GIVEN', sent: 'SENT', refused: 'REFUSED', withdrawn: 'WITHDRAWN',
  not_sent: 'NOT_SENT', notsent: 'NOT_SENT', none: 'NOT_SENT',
};
export const ACCOUNT_STATUSES = ['LEAD', 'PROPOSAL_SENT', 'ACTIVE', 'ON_HOLD', 'CLOSED'];
export const JOB_ORDER_STATUSES = ['OPEN', 'SOURCING', 'SHORTLIST_SENT', 'INTERVIEWING', 'OFFER', 'FILLED', 'CANCELLED'];

const NORMALIZERS: Record<OperatorKey, (v: string) => string | null> = {
  owner: (v) => (['me', 'ben'].includes(fold(v)) ? 'me' : null),
  stale: (v) => (/^\d{1,4}$/.test(v.trim()) && Number(v) > 0 ? String(Number(v)) : null),
  city: (v) => v.trim() || null,
  company: (v) => v.trim() || null,
  type: (v) => TYPE_ALIASES[constant(v).toLowerCase()] ?? null,
  consent: (v) => CONSENT_ALIASES[constant(v).toLowerCase()] ?? null,
  status: (v) => {
    const c = constant(v);
    return ACCOUNT_STATUSES.includes(c) || JOB_ORDER_STATUSES.includes(c) ? c : null;
  },
};

interface Token {
  text: string; // raw, with quotes
  start: number;
  end: number;
}

/** Splits on whitespace outside double quotes. A stray quote without a partner does not swallow the rest. */
function tokenize(input: string): Token[] {
  const out: Token[] = [];
  let i = 0;
  while (i < input.length) {
    if (/\s/.test(input[i]!)) {
      i++;
      continue;
    }
    const start = i;
    let quoted = false;
    while (i < input.length && (quoted || !/\s/.test(input[i]!))) {
      // a quote only opens a phrase when a closing quote follows; a stray one is plain text
      if (input[i] === '"' && (quoted || input.indexOf('"', i + 1) !== -1)) quoted = !quoted;
      i++;
    }
    out.push({ text: input.slice(start, i), start, end: i });
  }
  return out;
}

const unquote = (s: string) => s.replace(/"/g, '');

export function parseQuery(input: string): ParsedQuery {
  const operators: ParsedOperator[] = [];
  const free: string[] = [];
  for (const tok of tokenize(input)) {
    const m = /^([A-Za-z]+):(.*)$/.exec(tok.text);
    const key = m ? (m[1]!.toLowerCase() as OperatorKey) : null;
    if (m && key && Object.hasOwn(NORMALIZERS, key)) {
      const value = NORMALIZERS[key](unquote(m[2]!));
      if (value) {
        operators.push({ key, value, start: tok.start, end: tok.end });
        continue;
      }
    }
    free.push(m ? tok.text : unquote(tok.text));
  }
  return { operators, text: free.join(' ').trim() };
}

/** Input without one operator token (used by the chip remove button). */
export function removeOperator(input: string, op: Pick<ParsedOperator, 'start' | 'end'>): string {
  return `${input.slice(0, op.start)}${input.slice(op.end)}`.replace(/\s{2,}/g, ' ').trimStart();
}

export interface OperatorRoute {
  path: '/network' | '/accounts' | '/job-orders';
  /** Query parameters, in a stable order. */
  params: [string, string][];
  /** Operators that this list cannot filter by; shown dimmed, not sent. */
  dropped: ParsedOperator[];
  /** Sent in the URL, but the API cannot filter by them yet (city, company); shown dimmed as 
ot applied yet. */
  pending: ParsedOperator[];
  /** path + query string, ready for navigate(). */
  to: string;
}

const ACCEPTED: Record<OperatorRoute['path'], OperatorKey[]> = {
  '/network': ['owner', 'stale', 'type', 'consent', 'city', 'company'],
  '/accounts': ['owner', 'status'],
  '/job-orders': ['owner', 'status'],
};
const ORDER: OperatorKey[] = ['owner', 'stale', 'type', 'consent', 'city', 'company', 'status'];

/**
 * Which list screen the operators belong to, with filters as query params:
 * /network?owner=me&stale=90&type=EXECUTIVE&consent=GIVEN&city=izmir&company=Y+Holding&q=text,
 * /accounts?owner=me&status=ACTIVE&q=text, /job-orders?owner=me&status=OPEN&q=text.
 * Repeated type/city/company operators are joined with commas (type=EXECUTIVE,CANDIDATE); owner, stale and status keep the last one.
 * Returns null when no operator maps to a list.
 */
export function routeFor(p: ParsedQuery): OperatorRoute | null {
  const has = (k: OperatorKey) => p.operators.some((o) => o.key === k);
  let path: OperatorRoute['path'] | null = null;
  if (['stale', 'type', 'consent', 'city', 'company'].some((k) => has(k as OperatorKey))) path = '/network';
  else if (p.operators.some((o) => o.key === 'status' && JOB_ORDER_STATUSES.includes(o.value))) path = '/job-orders';
  else if (has('status') || has('owner')) path = '/accounts';
  if (!path) return null;

  const accepted = ACCEPTED[path];
  const kept = p.operators.filter((o) => accepted.includes(o.key) && (o.key !== 'status' || (path === '/job-orders' ? JOB_ORDER_STATUSES : ACCOUNT_STATUSES).includes(o.value)));
  const dropped = p.operators.filter((o) => !kept.includes(o));
  const params: [string, string][] = [];
  for (const k of ORDER) {
    const vals = [...new Set(kept.filter((o) => o.key === k).map((o) => o.value))];
    if (vals.length) params.push([k, k === 'owner' || k === 'stale' || k === 'status' ? vals[vals.length - 1]! : vals.join(',')]);
  }
  if (p.text) params.push(['q', p.text]);
  const pending = path === '/network' ? kept.filter((o) => o.key === 'city' || o.key === 'company') : [];
  const qs = new URLSearchParams(params).toString();
  return { path, params, dropped, pending, to: qs ? `${path}?${qs}` : path };
}

/** Wrap-around arrow navigation for the listbox: -1 means "nothing active yet". */
export function moveActive(index: number, delta: 1 | -1, count: number): number {
  if (count <= 0) return -1;
  if (index < 0) return delta === 1 ? 0 : count - 1;
  return (index + delta + count) % count;
}

/** Case- and accent-insensitive "contains", for filtering quick actions. */
export function matchesText(label: string, text: string): boolean {
  const f = (s: string) => fold(s).normalize('NFD').replace(/[\u0300-\u036f]/g, '');
  return f(label).includes(f(text));
}

