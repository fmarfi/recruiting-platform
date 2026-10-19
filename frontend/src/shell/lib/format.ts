// Dates: EN `16 Oct 2026`, TR `16.10.2026`; relative dates carry the absolute date in a tooltip (docs/ux/README.md).
const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

function parts(iso: string): [number, number, number] {
  const d = iso.length === 10 ? iso.split('-').map(Number) : (() => { const x = new Date(iso); return [x.getFullYear(), x.getMonth() + 1, x.getDate()]; })();
  return [d[0] ?? 0, d[1] ?? 1, d[2] ?? 1];
}

export function fmtDate(iso: string | null | undefined, lang: string): string {
  if (!iso) return '-';
  const [y, m, d] = parts(iso);
  if (lang.startsWith('tr')) return `${String(d).padStart(2, '0')}.${String(m).padStart(2, '0')}.${y}`;
  return `${d} ${MONTHS[m - 1]} ${y}`;
}

/** Whole days from the date part of `iso` to `today` (YYYY-MM-DD); positive = in the past. */
export function daysAgo(iso: string, today: string): number {
  const [y, m, d] = parts(iso);
  const [ty, tm, td] = parts(today);
  return Math.round((Date.UTC(ty, tm - 1, td) - Date.UTC(y, m - 1, d)) / 86_400_000);
}

export function fmtRelative(iso: string | null | undefined, lang: string, today: string = new Date().toISOString().slice(0, 10)): string {
  if (!iso) return '-';
  const n = daysAgo(iso, today);
  return new Intl.RelativeTimeFormat(lang, { numeric: 'auto' }).format(-n, 'day');
}

export function todayIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export function addDays(iso: string, n: number): string {
  const [y, m, d] = parts(iso);
  const x = new Date(Date.UTC(y, m - 1, d + n));
  return x.toISOString().slice(0, 10);
}
