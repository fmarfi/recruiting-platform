// Edit drawer for a person: every writable field of PersonUpdateRequest, tags (via bulk ADD_TAG / REMOVE_TAG) and compensation behind the role guard.
import { Group, NativeSelect, NumberInput, Text, Textarea, TextInput } from '@mantine/core';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { orNull, OwnerSelect, useDraft, useRecordSave } from '../../accounts/detail/editSupport';
import { type Schemas } from '../../pipeline/shared';
import { EditDrawer, TagInput } from '../../shell/edit';
import { cleanRows, RowsEditor, type Column } from './RowsEditor';

type Person = Schemas['Person'];

export interface PersonForm extends Record<string, string> {
  name: string; type: string; title: string; company: string; email: string; phone: string; city: string;
  linkedinUrl: string; howWeKnow: string; notes: string; ownerId: string;
  sourceChannel: string; noticePeriodDays: string; mobility: string;
  employments: string; languages: string; educations: string;
  compBase: string; compVariable: string; compExpected: string; compCurrency: string; compPeriod: string; compBasis: string; compBenefits: string;
}

const str = (v: unknown) => (v === null || v === undefined ? '' : String(v));
const rowsJson = (rows: unknown[] | undefined) => JSON.stringify(rows ?? []);
const COMP = ['compBase', 'compVariable', 'compExpected', 'compCurrency', 'compPeriod', 'compBasis', 'compBenefits'];

export const personForm = (p: Person): PersonForm => ({
  name: p.name, type: p.type, title: str(p.title), company: str(p.company), email: str(p.email), phone: str(p.phone), city: str(p.city),
  linkedinUrl: str(p.linkedinUrl), howWeKnow: str(p.howWeKnow), notes: str(p.notes), ownerId: p.owner.id,
  sourceChannel: str(p.sourceChannel), noticePeriodDays: str(p.noticePeriodDays), mobility: str(p.mobility),
  employments: rowsJson(p.employments), languages: rowsJson(p.languages), educations: rowsJson(p.educations),
  compBase: str(p.compensation?.currentBase), compVariable: str(p.compensation?.currentVariable), compExpected: str(p.compensation?.expected),
  compCurrency: p.compensation?.currency ?? 'TRY', compPeriod: p.compensation?.period ?? 'ANNUAL', compBasis: p.compensation?.basis ?? 'GROSS', compBenefits: str(p.compensation?.benefits),
});

const NULLABLE = new Set(['title', 'company', 'email', 'phone', 'city', 'linkedinUrl', 'howWeKnow', 'notes', 'sourceChannel', 'mobility']);
const num = (v: string) => (v.trim() === '' ? null : Number(v));

export function personBody(v: PersonForm, keys: string[], cols: { emp: Column[]; lang: Column[]; edu: Column[] }) {
  const body: Record<string, unknown> = {};
  for (const k of keys) {
    if (COMP.includes(k)) {
      const empty = !v.compBase.trim() && !v.compVariable.trim() && !v.compExpected.trim() && !v.compBenefits.trim();
      body.compensation = empty ? null : { currentBase: num(v.compBase), currentVariable: num(v.compVariable), expected: num(v.compExpected), currency: v.compCurrency.trim().toUpperCase(), period: v.compPeriod, basis: v.compBasis, benefits: orNull(v.compBenefits) };
    } else if (k === 'noticePeriodDays') body[k] = num(v[k]);
    else if (k === 'employments') body[k] = cleanRows(v[k], cols.emp);
    else if (k === 'languages') body[k] = cleanRows(v[k], cols.lang).map((r) => ({ ...r, language: String(r.language ?? '').toLowerCase() }));
    else if (k === 'educations') body[k] = cleanRows(v[k], cols.edu);
    else if (NULLABLE.has(k)) body[k] = orNull(v[k]!);
    else body[k] = v[k]!.trim();
  }
  return body;
}

interface Props {
  person: Person;
  opened: boolean;
  onClose: () => void;
  onSaved: () => void;
}

export function PersonEditDrawer({ person: p, opened, onClose, onSaved }: Props) {
  const { t } = useTranslation();
  const restricted = !!p.compensationRestricted;
  const initial = personForm(p);
  const { values, set, errors, setErrors, dirty } = useDraft(initial, opened);
  const [tags, setTags] = useState<string[]>(p.tags ?? []);
  useEffect(() => {
    if (opened) setTags(p.tags ?? []);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [opened]);

  const before = (p.tags ?? []).map((x) => x.toLowerCase());
  const after = tags.map((x) => x.toLowerCase());
  const added = after.filter((x) => !before.includes(x));
  const removed = before.filter((x) => !after.includes(x));
  const tagsChanged = added.length + removed.length > 0;

  const lvl = (['BASIC', 'INTERMEDIATE', 'FLUENT', 'NATIVE'] as const).map((v) => ({ value: v, label: t(`network.profile.levels.${v}`) }));
  const cols = {
    emp: [
      { key: 'company', label: t('network.fields.company'), maxLength: 200, required: true },
      { key: 'title', label: t('network.fields.title'), maxLength: 200 },
      { key: 'startDate', label: t('network.edit.startDate'), kind: 'date' },
      { key: 'endDate', label: t('network.edit.endDate'), kind: 'date' },
    ] as Column[],
    lang: [
      { key: 'language', label: t('network.edit.langCode'), maxLength: 2, required: true },
      { key: 'level', label: t('network.edit.langLevel'), kind: 'select', options: lvl },
    ] as Column[],
    edu: [
      { key: 'school', label: t('network.edit.school'), maxLength: 200, required: true },
      { key: 'degree', label: t('network.edit.degree'), maxLength: 100 },
      { key: 'field', label: t('network.edit.studyField'), maxLength: 200 },
      { key: 'startYear', label: t('network.edit.startYear'), kind: 'number' },
      { key: 'endYear', label: t('network.edit.endYear'), kind: 'number' },
    ] as Column[],
  };

  const bulk = async (action: 'ADD_TAG' | 'REMOVE_TAG', tag: string) => {
    const { data, error } = await api.POST('/bulk-actions', { body: { entity: 'PEOPLE', ids: [p.id], action, params: { tag } } });
    if (error || !data) throw error ?? new Error('bulk');
    const res = data.results[0];
    if (res && res.outcome === 'FAILED') throw { detail: res.code === 'TOO_MANY_TAGS' ? t('network.edit.tooManyTags') : res.message };
  };

  const path = { params: { path: { id: p.id } } };
  const save = useRecordSave<Person, PersonForm>({
    record: p,
    initial,
    values,
    setErrors,
    toBody: (v, keys) => personBody(v, keys, cols),
    patch: (body, version) => api.PATCH('/people/{id}', { ...path, body: { ...body, version } }),
    fetchLatest: () => api.GET('/people/{id}', path),
    toForm: personForm,
    labels: {
      name: t('network.fields.name'), type: t('network.fields.type'), title: t('network.fields.title'), company: t('network.fields.company'), email: t('network.fields.email'),
      phone: t('network.edit.phone'), city: t('network.fields.city'), linkedinUrl: 'LinkedIn', howWeKnow: t('network.fields.howWeKnow'), notes: t('network.edit.notes'), ownerId: t('network.fields.owner'),
      sourceChannel: t('network.profile.source'), noticePeriodDays: t('network.profile.notice'), mobility: t('network.profile.mobility'),
      employments: t('network.career'), languages: t('network.profile.languages'), educations: t('network.profile.education'),
      compBase: t('network.profile.base'), compVariable: t('network.profile.variable'), compExpected: t('network.profile.expected'), compCurrency: t('network.edit.currency'),
      compPeriod: t('network.edit.period'), compBasis: t('network.edit.basis'), compBenefits: t('network.edit.benefits'),
    },
    message: t('network.edit.saved', { name: p.name }),
    onSaved,
    extra: {
      changed: tagsChanged,
      apply: async () => {
        for (const tag of added) await bulk('ADD_TAG', tag);
        for (const tag of removed) await bulk('REMOVE_TAG', tag);
      },
      revert: async () => {
        for (const tag of added) await bulk('REMOVE_TAG', tag);
        for (const tag of removed) await bulk('ADD_TAG', tag);
      },
    },
  });

  const text = (key: keyof PersonForm & string, label: string, extra: object = {}) => (
    <TextInput label={label} value={values[key]} onChange={(e) => set(key, e.currentTarget.value)} error={errors[key] || undefined} {...extra} />
  );
  const rowError = (prefix: string) => Object.entries(errors).find(([k, m]) => k.startsWith(prefix) && m)?.[1];
  const channels = ['LINKEDIN_AD', 'LINKEDIN_DIRECT', 'WEBSITE', 'REFERRAL', 'EXISTING_POOL', 'EMAIL', 'CV_UPLOAD'];

  return (
    <EditDrawer
      opened={opened}
      onClose={onClose}
      title={t('network.edit.title', { name: p.name })}
      size={640}
      dirty={dirty || tagsChanged}
      onSave={save.onSave}
      onResolveConflict={save.onResolveConflict}
      sections={[
        {
          id: 'main',
          title: t('network.edit.sectionMain'),
          content: (
            <>
              {text('name', t('network.fields.name'), { required: true, maxLength: 200, 'data-autofocus': true })}
              <NativeSelect label={t('network.fields.type')} value={values.type} data={(['EXECUTIVE', 'CLIENT_CONTACT', 'REFERRER', 'CANDIDATE'] as const).map((v) => ({ value: v, label: t(`network.types.${v}`) }))} onChange={(e) => set('type', e.currentTarget.value)} error={errors.type || undefined} />
              {text('title', t('network.fields.title'), { maxLength: 200 })}
              {text('company', t('network.fields.company'), { maxLength: 200 })}
              {text('city', t('network.fields.city'), { maxLength: 100 })}
              <OwnerSelect label={t('network.fields.owner')} value={values.ownerId} current={p.owner} onChange={(v) => set('ownerId', v)} error={errors.ownerId} />
              <TagInput label={t('network.pool.tags')} value={tags} onChange={setTags} maxTags={20} />
            </>
          ),
        },
        {
          id: 'contact',
          title: t('network.edit.sectionContact'),
          content: (
            <>
              {text('email', t('network.fields.email'), { type: 'email', maxLength: 254 })}
              {text('phone', t('network.edit.phone'), { type: 'tel', maxLength: 50 })}
              {text('linkedinUrl', 'LinkedIn', { type: 'url', maxLength: 300, placeholder: 'https://' })}
              {text('howWeKnow', t('network.fields.howWeKnow'), { maxLength: 1000 })}
              <Textarea label={t('network.edit.notes')} value={values.notes} onChange={(e) => set('notes', e.currentTarget.value)} error={errors.notes || undefined} autosize minRows={3} maxLength={4000} />
            </>
          ),
        },
        {
          id: 'profile',
          title: t('network.profile.title'),
          content: (
            <>
              <NativeSelect label={t('network.profile.source')} value={values.sourceChannel} data={[{ value: '', label: '-' }, ...channels.map((v) => ({ value: v, label: t(`network.pool.channel.${v}`) }))]} onChange={(e) => set('sourceChannel', e.currentTarget.value)} error={errors.sourceChannel || undefined} />
              <NumberInput label={t('network.profile.notice')} description={t('network.edit.noticeHint')} min={0} max={365} allowDecimal={false} value={values.noticePeriodDays === '' ? '' : Number(values.noticePeriodDays)} onChange={(v) => set('noticePeriodDays', v === '' ? '' : String(v))} error={errors.noticePeriodDays || undefined} />
              <NativeSelect label={t('network.profile.mobility')} value={values.mobility} data={[{ value: '', label: '-' }, ...(['NONE', 'WITHIN_COUNTRY', 'INTERNATIONAL'] as const).map((v) => ({ value: v, label: t(`network.profile.mobilities.${v}`) }))]} onChange={(e) => set('mobility', e.currentTarget.value)} error={errors.mobility || undefined} />
              <RowsEditor legend={t('network.career')} columns={cols.emp} value={values.employments} onChange={(j) => set('employments', j)} addLabel={t('network.edit.addEmployment')} removeLabel={(n) => t('network.edit.removeRow', { n })} error={rowError('employments')} />
              <RowsEditor legend={t('network.profile.languages')} columns={cols.lang} value={values.languages} onChange={(j) => set('languages', j)} addLabel={t('network.edit.addLanguage')} removeLabel={(n) => t('network.edit.removeRow', { n })} error={rowError('languages')} />
              <RowsEditor legend={t('network.profile.education')} columns={cols.edu} value={values.educations} onChange={(j) => set('educations', j)} addLabel={t('network.edit.addEducation')} removeLabel={(n) => t('network.edit.removeRow', { n })} error={rowError('educations')} />
            </>
          ),
        },
        ...(restricted
          ? []
          : [
              {
                id: 'comp',
                title: t('network.profile.compensation'),
                content: (
                  <>
                    <Group grow align="flex-start">
                      {text('compBase', t('network.profile.base'), { type: 'number', min: 0 })}
                      {text('compVariable', t('network.profile.variable'), { type: 'number', min: 0 })}
                      {text('compExpected', t('network.profile.expected'), { type: 'number', min: 0 })}
                    </Group>
                    <Group grow align="flex-start">
                      {text('compCurrency', t('network.edit.currency'), { maxLength: 3 })}
                      <NativeSelect label={t('network.edit.period')} value={values.compPeriod} data={(['MONTHLY', 'ANNUAL'] as const).map((v) => ({ value: v, label: t(`network.profile.periods.${v}`) }))} onChange={(e) => set('compPeriod', e.currentTarget.value)} />
                      <NativeSelect label={t('network.edit.basis')} value={values.compBasis} data={(['GROSS', 'NET'] as const).map((v) => ({ value: v, label: t(`network.profile.bases.${v}`) }))} onChange={(e) => set('compBasis', e.currentTarget.value)} />
                    </Group>
                    {text('compBenefits', t('network.edit.benefits'), { maxLength: 500 })}
                    {rowError('compensation') && <Text size="sm" c="red.8" role="alert">{rowError('compensation')}</Text>}
                  </>
                ),
              },
            ]),
      ]}
    />
  );
}
