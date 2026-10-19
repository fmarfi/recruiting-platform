import { Anchor, Group, NavLink as MNavLink, Stack, Text, Title } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link, NavLink, Navigate, useParams } from 'react-router-dom';
import { useAuth } from '../shell/auth/useAuth';
import { RolesSection } from './RolesSection';
import { StatusListsSection } from './StatusListsSection';
import { UsersSection } from './users/UsersSection';

const SECTIONS = ['users', 'roles', 'statuses', 'import', 'retention', 'audit'] as const;

/** Settings (UX-07): section list on the left (wraps on phone), manager and admin only. */
export function SettingsPage() {
  const { t } = useTranslation();
  const { isManager } = useAuth();
  const { section = 'users' } = useParams();

  if (!isManager)
    return (
      <Stack>
        <Text role="alert">{t('settings.noAccess')}</Text>
        <Anchor component={Link} to="/">{t('settings.home')}</Anchor>
      </Stack>
    );
  if (!(SECTIONS as readonly string[]).includes(section)) return <Navigate to="/settings/users" replace />;

  return (
    <Stack>
      <Title order={1}>{t('settings.title')}</Title>
      <Group align="flex-start" wrap="wrap" gap="lg">
        <Stack gap={0} miw={180} component="nav" aria-label={t('settings.title')}>
          {SECTIONS.map((s) => (
            <MNavLink key={s} component={NavLink} to={`/settings/${s}`} label={t(`settings.sec.${s}`)} active={s === section} />
          ))}
        </Stack>
        <div style={{ flex: 1, minWidth: 280 }}>
          <Title order={2} size="h3" mb="sm">{t(`settings.sec.${section}`)}</Title>
          {section === 'users' && <UsersSection />}
          {section === 'roles' && <RolesSection />}
          {section === 'statuses' && <StatusListsSection />}
          {!['users', 'roles', 'statuses'].includes(section) && <Text c="dimmed">{t('settings.soon')}</Text>}
        </div>
      </Group>
    </Stack>
  );
}
