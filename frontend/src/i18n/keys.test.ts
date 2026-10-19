import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join } from 'node:path';
import i18n from './index';

// Every static t('a.b') key used in the source must exist in both languages (dynamic keys are checked by their enum values below).
function files(dir: string): string[] {
  return readdirSync(dir).flatMap((f) => {
    const p = join(dir, f);
    return statSync(p).isDirectory() ? files(p) : /\.tsx?$/.test(f) && !/\.test\./.test(f) ? [p] : [];
  });
}

describe('i18n keys', () => {
  const src = join(__dirname, '..');
  const keys = new Set<string>();
  for (const f of files(src)) for (const m of readFileSync(f, 'utf8').matchAll(/\bt\(\s*'([a-zA-Z0-9_.]+)'/g)) keys.add(m[1]!);

  it.each(['tr', 'en'])('has every used key in %s', (lng) => {
    const missing = [...keys].filter((k) => !i18n.exists(k, { lng }));
    expect(missing).toEqual([]);
  });

  it('has dynamic key families in both languages', () => {
    const fam = [
      ...['CALL', 'VISIT', 'MEETING', 'EMAIL', 'NOTE'].map((v) => `accounts.activity.${v}`),
      ...['NOT_SENT', 'SENT', 'GIVEN', 'REFUSED', 'WITHDRAWN'].map((v) => `accounts.consent.${v}`),
      ...['ACCOUNT_STATUS', 'ACTIVITY', 'PROPOSAL', 'JOB_ORDER', 'CONTACT', 'CANDIDATE'].map((v) => `accounts.kind.${v}`),
      ...['overdue', 'today', 'upcoming'].map((v) => `myday.group.${v}`),
      ...['activeAccounts', 'openJobs', 'inProcess', 'placed'].map((v) => `myday.stat.${v}`),
    ];
    for (const lng of ['tr', 'en']) expect(fam.filter((k) => !i18n.exists(k, { lng }))).toEqual([]);
  });
});
