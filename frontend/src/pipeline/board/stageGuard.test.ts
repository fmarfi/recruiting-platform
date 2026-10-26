import { describe, expect, it } from 'vitest';
import { canOverrideOffLimits, classifyStageError, consentResendPossible, offLimitsRules, overrideBody, validOverrideReason, warningLines } from './stageGuard';

describe('stage move 409 handling', () => {
  it('classifies CONSENT_REQUIRED and OFF_LIMITS with their blocking items', () => {
    const c = classifyStageError({ code: 'CONSENT_REQUIRED', blockingItems: [{ id: 'p', title: 'Ali', type: 'PERSON', reasonCode: 'CONSENT_PENDING' }] });
    expect(c).toEqual({ kind: 'CONSENT_REQUIRED', items: [{ id: 'p', title: 'Ali', reasonCode: 'CONSENT_PENDING' }] });
    expect(classifyStageError({ code: 'OFF_LIMITS' })).toEqual({ kind: 'OFF_LIMITS', items: [] });
  });
  it('leaves every other failure alone', () => {
    expect(classifyStageError({ code: 'VERSION_CONFLICT' })).toBeUndefined();
    expect(classifyStageError(undefined)).toBeUndefined();
  });
  it('validates the override reason (3 to 500 after trimming) and the role', () => {
    expect(validOverrideReason('  ab ')).toBe(false);
    expect(validOverrideReason(' abc ')).toBe(true);
    expect(validOverrideReason('x'.repeat(500))).toBe(true);
    expect(validOverrideReason('x'.repeat(501))).toBe(false);
    expect(canOverrideOffLimits('MANAGER')).toBe(true);
    expect(canOverrideOffLimits('ADMIN')).toBe(true);
    expect(canOverrideOffLimits('CONSULTANT')).toBe(false);
    expect(overrideBody('  because  ')).toEqual({ offLimitsOverrideReason: 'because' });
  });
  it('cannot resend consent to an anonymised person', () => {
    expect(consentResendPossible([{ id: 'a', title: 'x', reasonCode: 'CONSENT_PENDING' }])).toBe(true);
    expect(consentResendPossible([{ id: 'a', title: 'x', reasonCode: 'ANONYMISED' }])).toBe(false);
  });
  it('turns warnings into codes with counts', () => {
    expect(warningLines([{ code: 'PREVIOUSLY_PRESENTED_TO_ACCOUNT', message: '', exposures: [{} as never, {} as never] }, { code: 'ACTIVE_IN_OTHER_PROCESS', message: '', otherProcesses: [{} as never] }])).toEqual([
      { code: 'PREVIOUSLY_PRESENTED_TO_ACCOUNT', count: 2 },
      { code: 'ACTIVE_IN_OTHER_PROCESS', count: 1 },
    ]);
  });
});

describe('consent guard for do-not-contact', () => {
  it('offers no resend for refused or withdrawn consent', () => {
    expect(consentResendPossible([{ id: 'a', title: 'x', reasonCode: 'CONSENT_REFUSED' }])).toBe(false);
    expect(consentResendPossible([{ id: 'a', title: 'x', reasonCode: 'CONSENT_WITHDRAWN' }])).toBe(false);
    expect(consentResendPossible([{ id: 'a', title: 'x', reasonCode: 'CONSENT_PENDING' }])).toBe(true);
  });
  it('lists off-limits rules for add-to-job errors only', () => {
    expect(offLimitsRules({ code: 'OFF_LIMITS', blockingItems: [{ id: '1', title: 'Acme', type: 'OFF_LIMITS_RULE' }] })).toBe('Acme');
    expect(offLimitsRules({ code: 'DUPLICATE' })).toBeUndefined();
  });
});
