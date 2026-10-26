// Pure rules for the candidate profile block.
import type { Schemas } from '../../pipeline/shared';

type P = Pick<Schemas['Person'], 'compensation' | 'compensationRestricted'>;

/** Compensation is shown only when the API sent it and says it is not restricted for this user. Restricted wins even if a value slipped through. */
export const compensationVisible = (p: P): boolean => !p.compensationRestricted && !!p.compensation;

/** The row exists (as "restricted") only when the server withheld it; no row at all when there is simply no value. */
export const compensationRestrictedNote = (p: P): boolean => !!p.compensationRestricted;

/** Only http(s) URLs become links; anything else is shown as plain text. */
export const safeLinkedIn = (url?: string | null): string | undefined => {
  if (!url) return undefined;
  try {
    const u = new URL(url);
    return u.protocol === 'https:' || u.protocol === 'http:' ? u.toString() : undefined;
  } catch {
    return undefined;
  }
};

/** Consent states from which a one-click (re)send makes sense: not given yet, pending, or withdrawn/refused (manual only). */
export const canResendConsent = (status: Schemas['ConsentStatus'], hasEmail: boolean, anonymised: boolean): boolean =>
  hasEmail && !anonymised && status !== 'GIVEN';

export const hasProfile = (p: Schemas['Person']): boolean =>
  !!(p.sourceChannel || p.noticePeriodDays != null || p.mobility || p.languages?.length || p.educations?.length || p.linkedinUrl || p.compensationRestricted || p.compensation);
