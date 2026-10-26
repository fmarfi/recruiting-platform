// Pure helpers for the CV intake review list.
import type { Schemas } from '../../pipeline/shared';
import { daysLeft } from '../files/fileRules';

type Item = Schemas['CvIntakeItem'];

/** Pending items are deleted at expiry; warn in the last 3 days. */
export function expiryInfo(item: Pick<Item, 'expiresAt'>, now = Date.now()) {
  const days = daysLeft(item.expiresAt, now);
  return { days, urgent: days <= 3 };
}

/** Suggestions for the "create person" form: the first value the server found for each field (no AI, patterns only). */
export function prefillFromDetected(d: Item['detected']) {
  return { email: d.emails[0] ?? '', phone: d.phones[0] ?? '', linkedinUrl: d.linkedinUrls[0] ?? '' };
}

/** Poll while any item is still being extracted. */
export const needsPolling = (items: Pick<Item, 'textStatus' | 'status'>[] | undefined) => !!items?.some((i) => i.status === 'PENDING' && i.textStatus === 'PENDING');

export const attachExisting = (personId: string): Schemas['CvIntakeAttachRequest'] => ({ personId });
