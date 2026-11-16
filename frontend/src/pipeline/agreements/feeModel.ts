// Which fields an agreement needs for its fee model, and the client-side checks that mirror the server rules (POST /agreements).
import type { components } from '../../api';

type Schemas = components['schemas'];
export type FeeModel = Schemas['FeeModel'];
export type FeeTrigger = Schemas['FeeTrigger'];
export type FeeBasis = Schemas['FeeBasisComponent'];

export const FEE_MODELS: FeeModel[] = ['RETAINED_THIRDS', 'RETAINED_CUSTOM', 'PERCENT_FIRST_YEAR_CASH', 'FLAT_FEE', 'HYBRID'];
export const FEE_BASIS: FeeBasis[] = ['BASE', 'TARGET_BONUS', 'PAID_BONUS', 'ALLOWANCES', 'SIGN_ON', 'CAR', 'OTHER_CASH'];
export const FEE_TRIGGERS: FeeTrigger[] = ['SIGNING', 'DATE_OFFSET', 'SHORTLIST_DELIVERED', 'OFFER_ACCEPTED', 'START_DATE', 'EXPENSES', 'ABORT'];

export interface FeeFields {
  percent: boolean;
  basis: boolean;
  flat: boolean;
  estimated: boolean;
  minCap: boolean;
  schedule: boolean;
}

/** Fields shown per model. Success-only fees have no instalments; a flat fee has no percent or basis. */
export function feeFields(model: FeeModel): FeeFields {
  switch (model) {
    case 'FLAT_FEE':
      return { percent: false, basis: false, flat: true, estimated: false, minCap: false, schedule: true };
    case 'PERCENT_FIRST_YEAR_CASH':
      return { percent: true, basis: true, flat: false, estimated: false, minCap: true, schedule: false };
    default: // RETAINED_THIRDS, RETAINED_CUSTOM, HYBRID
      return { percent: true, basis: true, flat: false, estimated: true, minCap: true, schedule: true };
  }
}

/** Form keys that are cleared when switching to `model`, so the PATCH does not keep stale values. */
export function hiddenKeys(model: FeeModel): string[] {
  const f = feeFields(model);
  return [
    ...(f.percent ? [] : ['feePercent']),
    ...(f.basis ? [] : ['feeBasis']),
    ...(f.flat ? [] : ['flatFeeAmount']),
    ...(f.estimated ? [] : ['estimatedFee']),
    ...(f.minCap ? [] : ['feeMinimum', 'feeCap']),
    ...(f.schedule ? [] : ['scheduleItems']),
  ];
}

export interface ScheduleRow {
  id?: string;
  label: string;
  trigger: FeeTrigger;
  offsetDays: string;
  percent: string;
  amount: string;
  dueDays: string;
  /** Kept from the server so a schedule replace does not reset it. */
  vatApplies?: boolean;
}

export const emptyRow = (label = ''): ScheduleRow => ({ label, trigger: 'SIGNING', offsetDays: '', percent: '', amount: '', dueDays: '30' });

/** Default instalments for a model: thirds are 33.34 / 33.33 / 33.33, so they sum to 100. */
export function defaultSchedule(model: FeeModel): ScheduleRow[] {
  if (model !== 'RETAINED_THIRDS') return [];
  const triggers: FeeTrigger[] = ['SIGNING', 'SHORTLIST_DELIVERED', 'OFFER_ACCEPTED'];
  const pct = ['33.34', '33.33', '33.33'];
  return triggers.map((trigger, i) => ({ ...emptyRow(`Retainer ${i + 1}/3`), trigger, percent: pct[i]! }));
}

const MONEY = /^\d+(\.\d{1,2})?$/;
const PCT = /^\d{1,3}(\.\d{1,2})?$/;

export type ErrorKey =
  | 'required' | 'money' | 'percent' | 'percentRange' | 'basis' | 'minCap' | 'signedOn' | 'oneOfPercentAmount' | 'offsetDays' | 'sum100'
  | 'label' | 'range' | 'job' | 'currency' | 'datesOrder';

export interface FormValues {
  feeModel: string;
  status: string;
  signedOn: string;
  validFrom: string;
  validUntil: string;
  feePercent: string;
  /** Comma-separated FeeBasisComponent values. */
  feeBasis: string;
  flatFeeAmount: string;
  estimatedFee: string;
  feeMinimum: string;
  feeCap: string;
  currency: string;
  paymentTermsDays: string;
  guaranteeMonths: string;
  /** JSON of ScheduleRow[]. */
  scheduleItems: string;
}

export function parseRows(json: string): ScheduleRow[] {
  try {
    const v: unknown = JSON.parse(json || '[]');
    return Array.isArray(v) ? (v as ScheduleRow[]) : [];
  } catch {
    return [];
  }
}

const int = (v: string, min: number, max: number) => /^\d+$/.test(v) && Number(v) >= min && Number(v) <= max;

/** Field-keyed error keys (empty object = valid). Schedule problems are reported under `scheduleItems`. */
export function validateAgreement(v: FormValues, needsJob = false, hasJob = true): Record<string, ErrorKey> {
  const e: Record<string, ErrorKey> = {};
  const f = feeFields(v.feeModel as FeeModel);
  if (needsJob && !hasJob) e.jobOrderId = 'job';
  if (!/^[A-Za-z]{3}$/.test(v.currency.trim())) e.currency = 'currency';
  if (v.status === 'SIGNED' && !v.signedOn) e.signedOn = 'signedOn';
  if (v.validFrom && v.validUntil && v.validUntil < v.validFrom) e.validUntil = 'datesOrder';
  if (!int(v.paymentTermsDays, 0, 180)) e.paymentTermsDays = 'range';
  if (!int(v.guaranteeMonths, 0, 36)) e.guaranteeMonths = 'range';
  if (f.flat && !MONEY.test(v.flatFeeAmount)) e.flatFeeAmount = v.flatFeeAmount ? 'money' : 'required';
  if (f.percent) {
    if (!v.feePercent) e.feePercent = 'required';
    else if (!PCT.test(v.feePercent)) e.feePercent = 'percent';
    else if (Number(v.feePercent) > 100) e.feePercent = 'percentRange';
  }
  if (f.basis && !v.feeBasis) e.feeBasis = 'basis';
  if (f.estimated && v.estimatedFee && !MONEY.test(v.estimatedFee)) e.estimatedFee = 'money';
  if (f.minCap) {
    if (v.feeMinimum && !MONEY.test(v.feeMinimum)) e.feeMinimum = 'money';
    if (v.feeCap && !MONEY.test(v.feeCap)) e.feeCap = 'money';
    if (!e.feeMinimum && !e.feeCap && v.feeMinimum && v.feeCap && Number(v.feeMinimum) > Number(v.feeCap)) e.feeMinimum = 'minCap';
  }
  if (f.schedule) {
    const rows = parseRows(v.scheduleItems);
    for (const r of rows) {
      if (!r.label.trim()) return { ...e, scheduleItems: 'label' };
      if ((r.percent !== '') === (r.amount !== '')) return { ...e, scheduleItems: 'oneOfPercentAmount' };
      if (r.percent && (!PCT.test(r.percent) || Number(r.percent) > 100)) return { ...e, scheduleItems: 'percent' };
      if (r.amount && !MONEY.test(r.amount)) return { ...e, scheduleItems: 'money' };
      if (r.trigger === 'DATE_OFFSET' && !int(r.offsetDays, 0, 730)) return { ...e, scheduleItems: 'offsetDays' };
      if (!int(r.dueDays, 0, 180)) return { ...e, scheduleItems: 'range' };
    }
    if (v.feeModel === 'RETAINED_THIRDS') {
      const sum = Math.round(rows.reduce((s, r) => s + Number(r.percent || 0), 0) * 100);
      if (sum !== 10000 || rows.some((r) => !r.percent)) e.scheduleItems = 'sum100';
    }
  }
  return e;
}

/** API shape of the instalments (FeeScheduleItemRequest). */
export function scheduleBody(json: string) {
  return parseRows(json).map((r) => ({
    ...(r.id ? { id: r.id } : {}),
    label: r.label.trim(),
    trigger: r.trigger,
    offsetDays: r.trigger === 'DATE_OFFSET' ? Number(r.offsetDays) : null,
    percent: r.percent || null,
    amount: r.amount || null,
    dueDays: Number(r.dueDays),
    ...(r.vatApplies === undefined ? {} : { vatApplies: r.vatApplies }),
  }));
}
