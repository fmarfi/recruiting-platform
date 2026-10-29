// Edit drawer for a search (job order): every writable field of JobOrderUpdateRequest, including description and fee terms. Status has its own menu.
import { Group, NumberInput, Textarea, TextInput } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { api } from '../../../api';
import { orNull, OwnerSelect, useDraft, useRecordSave } from '../../../accounts/detail/editSupport';
import { EditDrawer } from '../../../shell/edit';
import { type Schemas } from '../../shared';

type Job = Schemas['JobOrder'];

export interface JobForm extends Record<string, string> {
  title: string;
  location: string;
  seniority: string;
  description: string;
  feeTerms: string;
  openings: string;
  openedOn: string;
  ownerId: string;
}

export const jobForm = (j: Job): JobForm => ({
  title: j.title,
  location: j.location ?? '',
  seniority: j.seniority ?? '',
  description: j.description ?? '',
  feeTerms: j.feeTerms ?? '',
  openings: String(j.openings ?? 1),
  openedOn: j.openedOn,
  ownerId: j.owner.id,
});

const NULLABLE = new Set(['location', 'seniority', 'description', 'feeTerms']);
export const jobBody = (v: JobForm, keys: string[]) =>
  Object.fromEntries(keys.map((k) => [k, NULLABLE.has(k) ? orNull(v[k]!) : k === 'openings' ? Number(v[k]) || 1 : v[k]!.trim()]));

interface Props {
  job: Job;
  opened: boolean;
  onClose: () => void;
  onSaved: () => void;
}

export function JobOrderEditDrawer({ job: j, opened, onClose, onSaved }: Props) {
  const { t } = useTranslation();
  const initial = jobForm(j);
  const { values, set, errors, setErrors, dirty } = useDraft(initial, opened);
  const path = { params: { path: { id: j.id } } };
  const save = useRecordSave<Job, JobForm>({
    record: j,
    initial,
    values,
    setErrors,
    toBody: jobBody,
    patch: (body, version) => api.PATCH('/job-orders/{id}', { ...path, body: { ...body, version } }),
    fetchLatest: () => api.GET('/job-orders/{id}', path),
    toForm: jobForm,
    labels: {
      title: t('pipeline.edit.title'), location: t('pipeline.hl.location'), seniority: t('pipeline.hl.seniority'), description: t('pipeline.edit.description'),
      feeTerms: t('pipeline.fee'), openings: t('pipeline.edit.openings'), openedOn: t('pipeline.hl.opened'), ownerId: t('pipeline.hl.owner'),
    },
    message: t('pipeline.edit.saved', { name: j.title }),
    onSaved,
  });
  const text = (key: keyof JobForm & string, label: string, extra: object = {}) => (
    <TextInput label={label} value={values[key]} onChange={(e) => set(key, e.currentTarget.value)} error={errors[key] || undefined} {...extra} />
  );
  return (
    <EditDrawer
      opened={opened}
      onClose={onClose}
      title={t('pipeline.edit.drawerTitle', { name: j.title })}
      size={560}
      dirty={dirty}
      onSave={save.onSave}
      onResolveConflict={save.onResolveConflict}
      sections={[
        {
          id: 'role',
          title: t('pipeline.edit.sectionRole'),
          content: (
            <>
              {text('title', t('pipeline.edit.title'), { required: true, maxLength: 200, 'data-autofocus': true })}
              {text('location', t('pipeline.hl.location'), { maxLength: 200 })}
              {text('seniority', t('pipeline.hl.seniority'), { maxLength: 100 })}
              <Group grow align="flex-start">
                <NumberInput label={t('pipeline.edit.openings')} min={1} allowDecimal={false} value={values.openings === '' ? '' : Number(values.openings)} onChange={(v) => set('openings', v === '' ? '' : String(v))} error={errors.openings || undefined} />
                {text('openedOn', t('pipeline.hl.opened'), { type: 'date' })}
              </Group>
              <OwnerSelect label={t('pipeline.hl.owner')} value={values.ownerId} current={j.owner} onChange={(v) => set('ownerId', v)} error={errors.ownerId} />
            </>
          ),
        },
        {
          id: 'brief',
          title: t('pipeline.edit.sectionBrief'),
          content: (
            <>
              <Textarea label={t('pipeline.edit.description')} value={values.description} onChange={(e) => set('description', e.currentTarget.value)} error={errors.description || undefined} autosize minRows={4} maxRows={12} maxLength={10000} />
              <Textarea label={t('pipeline.fee')} description={t('pipeline.edit.feeHint')} value={values.feeTerms} onChange={(e) => set('feeTerms', e.currentTarget.value)} error={errors.feeTerms || undefined} autosize minRows={2} maxLength={2000} />
            </>
          ),
        },
      ]}
    />
  );
}
