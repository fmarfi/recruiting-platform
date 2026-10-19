import { Button, Card, Group, Select, Stack, Table, Text, TextInput } from '@mantine/core';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../shell/auth/useAuth';
import { EmptyState } from '../../shell/components/EmptyState';
import { ErrorBanner } from '../../shell/components/ErrorBanner';
import { SkeletonRows } from '../../shell/components/SkeletonRows';
import { ToneBadge } from '../../shell/components/ToneBadge';
import { problemText } from '../../shell/lib/problem';
import { InviteUserDialog } from './InviteUserDialog';
import { patchUser, useUsers, type User } from './useUsers';

/** Users (admin edits; managers read only). Role change and enable/disable send the row version. */
export function UsersSection() {
  const { t } = useTranslation();
  const { isAdmin, user: me } = useAuth();
  const [q, setQ] = useState('');
  const [invite, setInvite] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const { data, error, loading, reload } = useUsers(q);

  async function change(u: User, body: Parameters<typeof patchUser>[1]) {
    const { error: e } = await patchUser(u, body);
    setErr(e ? problemText(e, t('shell.saveError')) : null);
    reload();
  }

  const users = data?.content ?? [];
  const roleSelect = (u: User) => (
    <Select
      aria-label={t('settings.users.role')}
      w={170}
      allowDeselect={false}
      disabled={!isAdmin || u.id === me?.id}
      data={['CONSULTANT', 'MANAGER', 'ADMIN'].map((r) => ({ value: r, label: t(`settings.role.${r}`) }))}
      value={u.role}
      onChange={(v) => v && v !== u.role && void change(u, { role: v as User['role'] })}
    />
  );
  const stateBadge = (u: User) => <ToneBadge size="sm" tone={u.active ? 'GOOD' : 'NEUTRAL'} label={t(u.active ? 'settings.state.active' : 'settings.state.disabled')} />;
  const toggle = (u: User) =>
    isAdmin && u.id !== me?.id ? (
      <Button size="xs" variant="subtle" onClick={() => void change(u, { active: !u.active })}>
        {t(u.active ? 'settings.state.disabled' : 'settings.state.active')}
      </Button>
    ) : null;

  return (
    <Stack>
      <Group justify="space-between">
        <TextInput aria-label={t('settings.users.search')} placeholder={t('settings.users.search')} value={q} onChange={(e) => setQ(e.currentTarget.value)} />
        {isAdmin && <Button onClick={() => setInvite(true)}>{t('settings.users.invite')}</Button>}
      </Group>
      {!isAdmin && <Text c="dimmed" size="sm">{t('settings.users.adminOnly')}</Text>}
      {err && <Text c="red" role="alert">{err}</Text>}
      {loading && !data && <SkeletonRows n={4} />}
      {error !== undefined && <ErrorBanner onRetry={reload} />}
      {data && users.length === 0 && <EmptyState title={t('settings.users.empty')} />}
      {users.length > 0 && (
        <>
          <Table visibleFrom="sm" withTableBorder>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>{t('settings.users.name')}</Table.Th>
                <Table.Th>{t('settings.users.email')}</Table.Th>
                <Table.Th>{t('settings.users.role')}</Table.Th>
                <Table.Th>{t('settings.users.state')}</Table.Th>
                <Table.Th />
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {users.map((u) => (
                <Table.Tr key={u.id}>
                  <Table.Td>{u.name}</Table.Td>
                  <Table.Td>{u.email}</Table.Td>
                  <Table.Td>{roleSelect(u)}</Table.Td>
                  <Table.Td>{stateBadge(u)}</Table.Td>
                  <Table.Td>{toggle(u)}</Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
          <Stack hiddenFrom="sm">
            {users.map((u) => (
              <Card key={u.id} withBorder>
                <Text fw={600}>{u.name}</Text>
                <Text size="sm" c="dimmed">{u.email}</Text>
                <Group mt="xs" justify="space-between">
                  {roleSelect(u)}
                  {stateBadge(u)}
                  {toggle(u)}
                </Group>
              </Card>
            ))}
          </Stack>
        </>
      )}
      <InviteUserDialog opened={invite} onClose={() => setInvite(false)} onCreated={reload} />
    </Stack>
  );
}
