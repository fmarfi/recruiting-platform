// Edit drawer for a client: every writable field of AccountUpdateRequest (status has its own badge menu).
import { SimpleGrid, Textarea, TextInput } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { api, type components } from '../../api';
import { EditDrawer } from '../../shell/edit';
import { OwnerSelect, orNull, useDraft, useRecordSave } from './editSupport';

type Account = components['schemas']['Account'];

export interface AccountForm extends Record<string, string> {
  name: string;
  sector: string;
  city: string;
  website: string;
  notes: string;
  ownerId: string;
}

export const accountForm = (a: Account): AccountForm => ({
  name: a.name,
  sector: a.sector ?? '',
  city: a.city ?? '',
  website: a.website ?? '',
  notes: a.notes ?? '',
  ownerId: a.owner.id,
});

const NULLABLE = new Set(['city', 'website', 'notes']);
export const accountBody = (v: AccountForm, keys: string[]) => Object.fromEntries(keys.map((k) => [k, NULLABLE.has(k) ? orNull(v[k]!) : v[k]!.trim()]));

interface Props {
  account: Account;
  opened: boolean;
  onClose: () => void;
  onSaved: () => void;
}

export function AccountEditDrawer({ account: a, opened, onClose, onSaved }: Props) {
  const { t } = useTranslation();
  const initial = accountForm(a);
  const { values, set, errors, setErrors, dirty } = useDraft(initial, opened);
  const path = { params: { path: { id: a.id } } };
  const save = useRecordSave<Account, AccountForm>({
    record: a,
    initial,
    values,
    setErrors,
    toBody: accountBody,
    patch: (body, version) => api.PATCH('/accounts/{id}', { ...path, body: { ...body, version } }),
    fetchLatest: () => api.GET('/accounts/{id}', path),
    toForm: accountForm,
    labels: { name: t('accounts.edit.name'), sector: t('accounts.edit.sector'), city: t('accounts.edit.city'), website: t('accounts.edit.website'), notes: t('accounts.edit.notes'), ownerId: t('accounts.edit.owner') },
    message: t('accounts.edit.saved', { name: a.name }),
    onSaved,
  });
  const text = (key: keyof AccountForm & string, label: string, extra: object = {}) => (
    <TextInput label={label} value={values[key]} onChange={(e) => set(key, e.currentTarget.value)} error={errors[key] || undefined} {...extra} />
  );
  return (
    <EditDrawer
      opened={opened}
      onClose={onClose}
      title={t('accounts.edit.title', { name: a.name })}
      dirty={dirty}
      onSave={save.onSave}
      onResolveConflict={save.onResolveConflict}
      size={640}
      sections={[
        {
          id: 'main',
          title: t('accounts.edit.sectionMain'),
          content: (
            <>
              {text('name', t('accounts.edit.name'), { required: true, maxLength: 200, 'data-autofocus': true })}
              <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm">
                {text('sector', t('accounts.edit.sector'), { required: true, maxLength: 100 })}
                <OwnerSelect label={t('accounts.edit.owner')} value={values.ownerId} current={a.owner} onChange={(v) => set('ownerId', v)} error={errors.ownerId} />
              </SimpleGrid>
            </>
          ),
        },
        {
          id: 'contact',
          title: t('accounts.edit.sectionContact'),
          content: (
            <>
              <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm">
                {text('city', t('accounts.edit.city'), { maxLength: 100 })}
                {text('website', t('accounts.edit.website'), { type: 'url', maxLength: 300, placeholder: 'https://' })}
              </SimpleGrid>
              <Textarea label={t('accounts.edit.notes')} value={values.notes} onChange={(e) => set('notes', e.currentTarget.value)} error={errors.notes || undefined} autosize minRows={3} maxLength={4000} />
            </>
          ),
        },
      ]}
    />
  );
}
