// Pure rules for the duplicate prompt, the queue and the merge request.
import type { Schemas } from '../../pipeline/shared';

export type Match = Schemas['DuplicateMatch'];
export type Reason = Schemas['DuplicateMatchReason'];
export type Take = NonNullable<Schemas['PersonMergeRequest']['takeFromMerged']>;

/** Strongest first, as the API orders them. */
export const REASON_ORDER: Reason[] = ['EMAIL', 'LINKEDIN_URL', 'PHONE', 'NAME_COMPANY'];

/** Is there enough typed to ask the server? Name alone is too weak (the match is name + company). */
export function checkRequest(f: { name: string; company: string; email: string; phone: string; linkedinUrl: string }): Schemas['DuplicateCheckRequest'] | undefined {
  const v = (s: string) => (s.trim() ? s.trim() : null);
  const req = { name: v(f.name), company: v(f.company), email: v(f.email), phone: v(f.phone), linkedinUrl: v(f.linkedinUrl) };
  const usable = req.email || req.phone || req.linkedinUrl || (req.name && req.company);
  return usable ? req : undefined;
}

/** `prompt` when the check found anyone; a failed or empty check never blocks creating (the server still enforces unique e-mail with 409 DUPLICATE). */
export function duplicateDecision(matches: Match[] | undefined): 'create' | 'prompt' {
  return matches && matches.length > 0 ? 'prompt' : 'create';
}

export const strongestReason = (m: Match): Reason | undefined => REASON_ORDER.find((r) => m.reasons.includes(r));

/** Merge and the queue are for MANAGER and ADMIN only. */
export const canMerge = (role?: string | null) => role === 'MANAGER' || role === 'ADMIN';

export const MERGE_FIELDS: Take = ['name', 'type', 'title', 'company', 'email', 'phone', 'city', 'linkedinUrl', 'howWeKnow', 'notes', 'ownerId', 'sourceChannel', 'noticePeriodDays', 'mobility', 'compensation'];

/** The survivor's `version` is mandatory: a merge cannot be undone. Undefined means the survivor was not loaded, so no request can be built. */
export function mergeRequest(mergedPersonId: string, survivorVersion: number | undefined, take: Take): Schemas['PersonMergeRequest'] | undefined {
  if (survivorVersion == null) return undefined;
  return { mergedPersonId, version: survivorVersion, ...(take.length ? { takeFromMerged: MERGE_FIELDS.filter((f) => take.includes(f)) } : {}) };
}
