import { describe, expect, it } from 'vitest';
import { defaultSchedule, feeFields, hiddenKeys, scheduleBody, validateAgreement, type FormValues } from './feeModel';
import { checkSignedFile, MAX_SIGNED_BYTES } from './fileCheck';

const base: FormValues = {
  feeModel: 'PERCENT_FIRST_YEAR_CASH', status: 'DRAFT', signedOn: '', validFrom: '', validUntil: '', feePercent: '25', feeBasis: 'BASE',
  flatFeeAmount: '', estimatedFee: '', feeMinimum: '', feeCap: '', currency: 'TRY', paymentTermsDays: '30', guaranteeMonths: '6', scheduleItems: '[]',
};
const row = { label: 'A', trigger: 'SIGNING', offsetDays: '', percent: '', amount: '', dueDays: '30' };

describe('fee model fields', () => {
  it('shows only what the model needs', () => {
    expect(feeFields('FLAT_FEE')).toMatchObject({ flat: true, percent: false, basis: false, schedule: true });
    expect(feeFields('PERCENT_FIRST_YEAR_CASH')).toMatchObject({ percent: true, basis: true, schedule: false, flat: false });
    expect(feeFields('HYBRID')).toMatchObject({ percent: true, estimated: true, schedule: true });
  });
  it('lists the keys to clear when switching model', () => {
    expect(hiddenKeys('FLAT_FEE')).toEqual(expect.arrayContaining(['feePercent', 'feeBasis', 'estimatedFee', 'feeMinimum', 'feeCap']));
    expect(hiddenKeys('HYBRID')).toEqual(['flatFeeAmount']);
  });
  it('builds thirds that sum to 100', () => {
    const rows = defaultSchedule('RETAINED_THIRDS');
    expect(rows).toHaveLength(3);
    expect(validateAgreement({ ...base, feeModel: 'RETAINED_THIRDS', scheduleItems: JSON.stringify(rows) })).toEqual({});
  });
});

describe('agreement validation', () => {
  it('accepts a valid percent fee', () => expect(validateAgreement(base)).toEqual({}));
  it('needs a flat amount for FLAT_FEE', () => expect(validateAgreement({ ...base, feeModel: 'FLAT_FEE' }).flatFeeAmount).toBe('required'));
  it('needs percent and a basis for percent models', () => {
    const e = validateAgreement({ ...base, feePercent: '', feeBasis: '' });
    expect(e.feePercent).toBe('required');
    expect(e.feeBasis).toBe('basis');
  });
  it('rejects percent over 100 and minimum above cap', () => {
    expect(validateAgreement({ ...base, feePercent: '120' }).feePercent).toBe('percentRange');
    expect(validateAgreement({ ...base, feeMinimum: '500', feeCap: '100' }).feeMinimum).toBe('minCap');
  });
  it('needs signedOn when SIGNED', () => expect(validateAgreement({ ...base, status: 'SIGNED' }).signedOn).toBe('signedOn'));
  it('needs a position for an engagement letter', () => expect(validateAgreement(base, true, false).jobOrderId).toBe('job'));
  it('checks instalments: one of percent and amount, offset days, thirds sum', () => {
    const m = { ...base, feeModel: 'RETAINED_CUSTOM' };
    expect(validateAgreement({ ...m, scheduleItems: JSON.stringify([row]) }).scheduleItems).toBe('oneOfPercentAmount');
    expect(validateAgreement({ ...m, scheduleItems: JSON.stringify([{ ...row, percent: '50', trigger: 'DATE_OFFSET' }]) }).scheduleItems).toBe('offsetDays');
    const t = { ...base, feeModel: 'RETAINED_THIRDS', scheduleItems: JSON.stringify([{ ...row, percent: '50' }, { ...row, percent: '40' }]) };
    expect(validateAgreement(t).scheduleItems).toBe('sum100');
  });
  it('rejects thirds without instalments', () => {
    expect(validateAgreement({ ...base, feeModel: 'RETAINED_THIRDS', scheduleItems: '[]' }).scheduleItems).toBe('sum100');
  });
  it('keeps vatApplies per instalment and omits it when unknown', () => {
    const body = scheduleBody(JSON.stringify([{ ...row, percent: '50', vatApplies: false, id: 'i1' }, { ...row, percent: '50' }]));
    expect(body[0]).toMatchObject({ id: 'i1', vatApplies: false });
    expect(body[1]).not.toHaveProperty('vatApplies');
  });
  it('maps instalments to the request shape', () => {
    const body = scheduleBody(JSON.stringify([{ ...row, label: ' A ', offsetDays: '5', percent: '50' }]));
    expect(body).toEqual([{ label: 'A', trigger: 'SIGNING', offsetDays: null, percent: '50', amount: null, dueDays: 30 }]);
  });
});

describe('signed copy check', () => {
  it('accepts a PDF within 50 MB', () => expect(checkSignedFile({ name: 'a.pdf', type: 'application/pdf', size: 1000 })).toBe('ok'));
  it('accepts a PDF whose type the browser left empty', () => expect(checkSignedFile({ name: 'A.PDF', type: '', size: 10 })).toBe('ok'));
  it('rejects other types', () => expect(checkSignedFile({ name: 'a.docx', type: 'application/msword', size: 10 })).toBe('notPdf'));
  it('rejects files over 50 MB and empty files', () => {
    expect(checkSignedFile({ name: 'a.pdf', type: 'application/pdf', size: MAX_SIGNED_BYTES + 1 })).toBe('tooLarge');
    expect(checkSignedFile({ name: 'a.pdf', type: 'application/pdf', size: 0 })).toBe('empty');
    expect(checkSignedFile({ name: 'a.pdf', type: 'application/pdf', size: MAX_SIGNED_BYTES })).toBe('ok');
  });
});
