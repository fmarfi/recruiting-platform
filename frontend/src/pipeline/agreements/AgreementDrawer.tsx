// Agreement create and edit drawer. Fields follow the chosen fee model (feeModel.ts). Edit sends `version` and uses the shared 409 compare dialog.
import { Checkbox, NativeSelect, SimpleGrid, Text, Textarea, TextInput } from '@mantine/core';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { api, type components } from '../../api';
import { fieldErrors, orNull, useDraft, useRecordSave } from '../../accounts/detail/editSupport';
import { HistoryLink, ReadOnlyField, useCreateState } from './parts';
import { useAsync } from '../../shell/hooks/useAsync';
import { EditDrawer } from '../../shell/edit';
import { SignedCopy } from './SignedCopy';
import { ScheduleEditor } from './ScheduleEditor';
import { defaultSchedule, FEE_BASIS, FEE_MODELS, feeFields, hiddenKeys, parseRows, scheduleBody, validateAgreement, type FeeModel, type ScheduleRow } from './feeModel';

type Agreement = components['schemas']['Agreement'];
type Kind = components['schemas']['AgreementKind'];
export const STATUSES: components['schemas']['AgreementStatus'][] = ['DRAFT', 'SENT', 'SIGNED', 'EXPIRED', 'TERMINATED'];
const KINDS: Kind[] = ['TERMS_OF_BUSINESS', 'ENGAGEMENT_LETTER', 'AMENDMENT'];

export interface AgForm extends Record<string, string> {
  status: string; title: string; signedOn: string; validFrom: string; validUntil: string; feeModel: string; feePercent: string; feeBasis: string;
  flatFeeAmount: string; estimatedFee: string; feeMinimum: string; feeCap: string; currency: string; paymentTermsDays: string; guaranteeMonths: string;
  signatoryContactId: string; notes: string; scheduleItems: string;
}

const rowsOf = (a: Agreement): ScheduleRow[] =>
  a.scheduleItems.map((i) => ({ id: i.id, label: i.label ?? '', trigger: i.trigger ?? 'SIGNING', offsetDays: i.offsetDays?.toString() ?? '', percent: i.percent ?? '', amount: i.amount ?? '', dueDays: String(i.dueDays), vatApplies: i.vatApplies }));

/** Contact id of the signatory: the field the API returns, else the active contact of the same person. */
export const signatoryId = (a: Agreement | undefined, contacts: { id: string; person: { id: string } }[]): string =>
  a?.signatoryContactId ?? (a?.signatory ? contacts.find((c) => c.person.id === a.signatory!.id)?.id : undefined) ?? '';

export const agForm = (a?: Agreement, contacts: { id: string; person: { id: string } }[] = []): AgForm => ({
  status: a?.status ?? 'DRAFT', title: a?.title ?? '', signedOn: a?.signedOn ?? '', validFrom: a?.validFrom ?? '', validUntil: a?.validUntil ?? '',
  feeModel: a?.feeModel ?? 'PERCENT_FIRST_YEAR_CASH', feePercent: a?.feePercent ?? '', feeBasis: (a?.feeBasis ?? ['BASE']).join(','), flatFeeAmount: a?.flatFeeAmount ?? '',
  estimatedFee: a?.estimatedFee ?? '', feeMinimum: a?.feeMinimum ?? '', feeCap: a?.feeCap ?? '', currency: a?.currency ?? 'TRY',
  paymentTermsDays: String(a?.paymentTermsDays ?? 30), guaranteeMonths: String(a?.guaranteeMonths ?? 6), signatoryContactId: signatoryId(a, contacts), notes: a?.notes ?? '',
  scheduleItems: JSON.stringify(a ? rowsOf(a) : []),
});

const NULLABLE = new Set(['title', 'signedOn', 'validFrom', 'validUntil', 'feePercent', 'flatFeeAmount', 'estimatedFee', 'feeMinimum', 'feeCap', 'signatoryContactId', 'notes']);
export function agBody(v: AgForm, keys: string[]): Record<string, unknown> {
  return Object.fromEntries(
    keys.map((k) => {
      if (k === 'feeBasis') return [k, v[k]!.split(',').filter(Boolean)];
      if (k === 'scheduleItems') return [k, scheduleBody(v[k]!)];
      if (k === 'paymentTermsDays' || k === 'guaranteeMonths') return [k, Number(v[k])];
      if (k === 'currency') return [k, v[k]!.trim().toUpperCase()];
      return [k, NULLABLE.has(k) ? orNull(v[k]!) : v[k]!];
    }),
  );
}

export interface AgreementPreset {
  account: { id: string; name: string };
  /** Set when the drawer is opened on a search: the position is fixed. */
  fixedJob?: { id: string; title: string };
  /** Positions of the client to pick from (client page). */
  jobs?: { id: string; title: string }[];
}

interface Props {
  opened: boolean;
  onClose: () => void;
  onSaved: () => void;
  /** Edit this agreement; absent = create. */
  agreement?: Agreement;
  preset: AgreementPreset;
  onHistory?: () => void;
}

export function AgreementDrawer({ opened, onClose, onSaved, agreement: a, preset, onHistory }: Props) {
  const { t } = useTranslation();
  const contacts = useAsync(() => api.GET('/accounts/{id}/contacts', { params: { path: { id: preset.account.id }, query: { active: true } } }), [preset.account.id]);
  const people = contacts.data ?? [];
  const initial = agForm(a, people);
  const { values, set, errors, setErrors, dirty } = useDraft(initial, opened);
  const [kind, setKind] = useCreateState<Kind>(opened, preset.fixedJob ? 'ENGAGEMENT_LETTER' : 'TERMS_OF_BUSINESS');
  const [jobId, setJobId] = useCreateState<string>(opened, preset.fixedJob?.id ?? '');
  // Contacts may arrive after the draft was seeded: fill the signatory resolved from the person, so the draft is not dirty.
  useEffect(() => {
    if (opened && values.signatoryContactId === '' && initial.signatoryContactId) set('signatoryContactId', initial.signatoryContactId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [opened, initial.signatoryContactId]);
  const model = values.feeModel as FeeModel;
  const f = feeFields(model);
  const locked = !!a && a.scheduleItems.some((i) => i.invoiceState !== 'NOT_DUE');
  const basis = values.feeBasis.split(',').filter(Boolean);
  const path = { params: { path: { id: a?.id ?? '' } } };
  const text = (k: keyof AgForm & string, label: string, extra: object = {}) => <TextInput label={label} value={values[k]} onChange={(e) => set(k, e.currentTarget.value)} error={errors[k] || undefined} {...extra} />;
  const err = (e: Record<string, string>) => Object.fromEntries(Object.entries(e).map(([k, v]) => [k, t(`agr.err.${v}`, { defaultValue: v })]));

  const save = useRecordSave<Agreement, AgForm>({
    record: a ?? ({ version: 0 } as Agreement),
    initial,
    values,
    setErrors,
    toBody: agBody,
    patch: (body, version) => api.PATCH('/agreements/{id}', { ...path, body: { ...body, version } }),
    fetchLatest: () => api.GET('/agreements/{id}', path),
    toForm: (r) => agForm(r, people),
    noUndoKeys: ['scheduleItems'],
    labels: Object.fromEntries(Object.keys(initial).map((k) => [k, t(`agr.f.${k}`)])) as Record<keyof AgForm & string, string>,
    message: t('agr.saved'),
    onSaved,
  });

  const validate = () => {
    const needsJob = !a && kind === 'ENGAGEMENT_LETTER';
    const e = err(validateAgreement(values as never, needsJob, !!jobId));
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const create = async () => {
    const keys = Object.keys(initial).filter((k) => !hiddenKeys(model).includes(k) && (k !== 'scheduleItems' || f.schedule));
    const body = { accountId: preset.account.id, jobOrderId: kind === 'TERMS_OF_BUSINESS' ? null : jobId || null, kind, ...agBody(values, keys) } as components['schemas']['AgreementCreateRequest'];
    const r = await api.POST('/agreements', { body });
    if (r.error) {
      const fe = fieldErrors(r.error);
      if (Object.keys(fe).length) setErrors(fe);
      throw { detail: Object.keys(fe).length ? t('agr.fixFields') : ((r.error as { detail?: string }).detail ?? t('shell.saveError')) };
    }
    onSaved();
  };

  const onSave = async () => {
    if (!validate()) throw { detail: t('agr.fixFields') };
    if (!a) return create();
    return save.onSave();
  };

  const changeModel = (m: FeeModel) => {
    const nf = feeFields(m);
    set('feeModel', m);
    for (const k of hiddenKeys(m)) {
      if (k === 'scheduleItems' && locked) continue;
      set(k as keyof AgForm & string, k === 'scheduleItems' ? '[]' : '');
    }
    if (nf.basis && basis.length === 0) set('feeBasis', 'BASE');
    if (nf.schedule && !locked && parseRows(values.scheduleItems).length === 0) set('scheduleItems', JSON.stringify(defaultSchedule(m)));
  };

  const sigOptions = [
    { value: '', label: '-' },
    ...people.map((c) => ({ value: c.id, label: [c.person.name, c.role].filter(Boolean).join(', ') })),
    ...(a?.signatory && values.signatoryContactId && !people.some((c) => c.id === values.signatoryContactId) ? [{ value: values.signatoryContactId, label: a.signatory.name }] : []),
  ];
  const kindOptions = (preset.fixedJob ? KINDS.filter((k) => k !== 'TERMS_OF_BUSINESS') : KINDS).map((k) => ({ value: k, label: t(`agr.kind.${k}`) }));

  return (
    <EditDrawer
      opened={opened}
      onClose={onClose}
      title={a ? a.title || t(`agr.kind.${a.kind}`) : t('agr.new')}
      saveLabel={a ? undefined : t('agr.create')}
      dirty={dirty || (!a && (kind !== 'TERMS_OF_BUSINESS' || !!jobId))}
      onSave={onSave}
      onResolveConflict={save.onResolveConflict}
      size={720}
      sections={[
        {
          id: 'main',
          title: t('agr.section.main'),
          content: (
            <>
              <ReadOnlyField label={t('agr.client')} value={preset.account.name} />
              <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm">
                {a ? (
                  <ReadOnlyField label={t('agr.f.kind')} value={t(`agr.kind.${a.kind}`)} />
                ) : (
                  <NativeSelect label={t('agr.f.kind')} value={kind} data={kindOptions} onChange={(e) => { setKind(e.currentTarget.value as Kind); if (e.currentTarget.value === 'TERMS_OF_BUSINESS') setJobId(''); }} />
                )}
                {a ? (
                  <ReadOnlyField label={t('agr.position')} value={a.jobOrder?.title ?? t('agr.wholeClient')} />
                ) : (
                  <NativeSelect
                    label={t('agr.position')}
                    value={jobId}
                    disabled={!!preset.fixedJob || kind === 'TERMS_OF_BUSINESS'}
                    error={errors.jobOrderId || undefined}
                    data={[{ value: '', label: t('agr.wholeClient') }, ...(preset.fixedJob ? [{ value: preset.fixedJob.id, label: preset.fixedJob.title }] : (preset.jobs ?? []).map((j) => ({ value: j.id, label: j.title })))]}
                    onChange={(e) => setJobId(e.currentTarget.value)}
                  />
                )}
              </SimpleGrid>
              {text('title', t('agr.f.title'), { maxLength: 200, 'data-autofocus': true })}
              <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm">
                <NativeSelect label={t('agr.f.status')} value={values.status} data={STATUSES.map((s) => ({ value: s, label: t(`agr.status.${s}`) }))} onChange={(e) => set('status', e.currentTarget.value)} />
                {text('signedOn', t('agr.f.signedOn'), { type: 'date', required: values.status === 'SIGNED' })}
                {text('validFrom', t('agr.f.validFrom'), { type: 'date' })}
                {text('validUntil', t('agr.f.validUntil'), { type: 'date' })}
              </SimpleGrid>
            </>
          ),
        },
        {
          id: 'fee',
          title: t('agr.section.fee'),
          content: (
            <>
              <NativeSelect label={t('agr.f.feeModel')} description={t(`agr.modelHelp.${model}`)} value={values.feeModel} data={FEE_MODELS.map((m) => ({ value: m, label: t(`agr.model.${m}`) }))} onChange={(e) => changeModel(e.currentTarget.value as FeeModel)} />
              <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm">
                {text('currency', t('agr.f.currency'), { maxLength: 3, style: { textTransform: 'uppercase' }, required: true })}
                {f.percent && text('feePercent', t('agr.f.feePercent'), { inputMode: 'decimal', required: true, rightSection: <Text size="sm" c="dimmed">%</Text> })}
                {f.flat && text('flatFeeAmount', t('agr.f.flatFeeAmount'), { inputMode: 'decimal', required: true })}
                {f.estimated && text('estimatedFee', t('agr.f.estimatedFee'), { inputMode: 'decimal', description: t('agr.help.estimatedFee') })}
                {f.minCap && text('feeMinimum', t('agr.f.feeMinimum'), { inputMode: 'decimal' })}
                {f.minCap && text('feeCap', t('agr.f.feeCap'), { inputMode: 'decimal' })}
              </SimpleGrid>
              {f.basis && (
                <Checkbox.Group label={t('agr.f.feeBasis')} description={t('agr.help.feeBasis')} value={basis} onChange={(v) => set('feeBasis', v.join(','))} error={errors.feeBasis || undefined}>
                  <SimpleGrid cols={{ base: 2, sm: 4 }} spacing="xs" mt="xs">
                    {FEE_BASIS.map((b) => (
                      <Checkbox key={b} value={b} label={t(`agr.basis.${b}`)} />
                    ))}
                  </SimpleGrid>
                </Checkbox.Group>
              )}
              {f.schedule && (
                <div>
                  <Text size="sm" fw={500} mb="xs">
                    {t('agr.schedule.title')}
                  </Text>
                  {locked ? (
                    <Text size="sm" c="var(--color-ink3)">
                      {t('agr.schedule.locked')}
                    </Text>
                  ) : (
                    <ScheduleEditor value={values.scheduleItems} onChange={(v) => set('scheduleItems', v)} error={errors.scheduleItems} />
                  )}
                </div>
              )}
            </>
          ),
        },
        {
          id: 'terms',
          title: t('agr.section.terms'),
          content: (
            <>
              <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm">
                {text('paymentTermsDays', t('agr.f.paymentTermsDays'), { inputMode: 'numeric' })}
                {text('guaranteeMonths', t('agr.f.guaranteeMonths'), { inputMode: 'numeric' })}
              </SimpleGrid>
              <NativeSelect label={t('agr.f.signatoryContactId')} description={t('agr.help.signatory')} value={values.signatoryContactId} data={sigOptions} onChange={(e) => set('signatoryContactId', e.currentTarget.value)} />
              <Textarea label={t('agr.f.notes')} value={values.notes} onChange={(e) => set('notes', e.currentTarget.value)} autosize minRows={3} maxLength={4000} />
            </>
          ),
        },
        ...(a
          ? [
              { id: 'file', title: t('agr.section.file'), content: <SignedCopy agreement={a} onUploaded={onSaved} /> },
              ...(onHistory ? [{ id: 'history', title: t('agr.history'), content: <HistoryLink onClick={onHistory} /> }] : []),
            ]
          : []),
      ]}
    />
  );
}
