import { api, type components } from '../api';
import { useAsync } from '../shell/hooks/useAsync';

export type OffLimitsRule = components['schemas']['OffLimitsRule'];
export type OffLimitsReason = components['schemas']['OffLimitsReason'];
type CreateBody = components['schemas']['OffLimitsRuleCreateRequest'];

export const OFF_LIMITS_REASONS: OffLimitsReason[] = ['CLIENT_AGREEMENT', 'GUARANTEE_PERIOD', 'CLIENT_REQUEST', 'OTHER'];

export interface OffLimitsForm {
  target: 'account' | 'company';
  accountId: string | null;
  companyName: string;
  reason: OffLimitsReason;
  note: string;
  startsOn: string;
  endsOn: string;
}

export const emptyForm = (): OffLimitsForm => ({ target: 'account', accountId: null, companyName: '', reason: 'CLIENT_AGREEMENT', note: '', startsOn: '', endsOn: '' });

export type FormError = 'target' | 'dates';

/** Local checks before sending: exactly one target, end date not before the start (the server checks again). */
export function validateForm(f: OffLimitsForm, today: string): FormError | null {
  if (f.target === 'account' ? !f.accountId : !f.companyName.trim()) return 'target';
  if (f.endsOn && f.endsOn < (f.startsOn || today)) return 'dates';
  return null;
}

/** Exactly one of accountId and companyName; blank optional fields are left out. */
export function toCreateBody(f: OffLimitsForm): CreateBody {
  return {
    ...(f.target === 'account' ? { accountId: f.accountId! } : { companyName: f.companyName.trim() }),
    reason: f.reason,
    ...(f.note.trim() ? { note: f.note.trim() } : {}),
    ...(f.startsOn ? { startsOn: f.startsOn } : {}),
    ...(f.endsOn ? { endsOn: f.endsOn } : {}),
  };
}

export const todayIso = (now = new Date()) => `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;

/** Maps a problem response to a message key under settings.offLimits.err. */
export function offLimitsErrorKey(status: number | undefined, error: unknown): 'duplicate' | 'forbidden' | 'validation' | 'generic' {
  const code = (error as { code?: string } | undefined)?.code;
  if (status === 409 && code === 'DUPLICATE') return 'duplicate';
  if (status === 403) return 'forbidden';
  if (status === 400) return 'validation';
  return 'generic';
}

/** All rules (server default order). */
export function useOffLimitsRules(active: boolean | undefined) {
  return useAsync(() => api.GET('/off-limits', { params: { query: { active, size: 100 } } }), [active]);
}

/** Rules in force for one account (account page). */
export function useAccountOffLimits(accountId: string) {
  return useAsync(() => api.GET('/off-limits', { params: { query: { accountId, active: true, size: 20 } } }), [accountId]);
}
