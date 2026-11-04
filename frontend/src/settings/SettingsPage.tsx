import { Anchor, Box, Flex, Paper, NavLink as MNavLink, Stack, Text, Title } from '@mantine/core';
import { IconClockHour4, IconFileImport, IconHandStop, IconHistory, IconListCheck, IconUserShield, IconUsers, type Icon } from '@tabler/icons-react';
import { PageHeader } from '../shell/components/PageHeader';
import { ICON_SIZE_NAV, ICON_STROKE } from '../shell/components/objectIcons';
import { useTranslation } from 'react-i18next';
import { Link, NavLink, Navigate, useParams } from 'react-router-dom';
import { useAuth } from '../shell/auth/useAuth';
import { AuditSection } from './AuditSection';
import { ImportSection } from './ImportSection';
import { OffLimitsSection } from './OffLimitsSection';
import { RetentionSection } from './RetentionSection';
import { RolesSection } from './RolesSection';
import { StatusListsSection } from './StatusListsSection';
import { UsersSection } from './users/UsersSection';

const SECTIONS = ['users', 'roles', 'statuses', 'import', 'retention', 'offlimits', 'audit'] as const;
type SectionKey = (typeof SECTIONS)[number];

const ICONS: Record<SectionKey, Icon> = {
  users: IconUsers,
  roles: IconUserShield,
  statuses: IconListCheck,
  import: IconFileImport,
  retention: IconClockHour4,
  offlimits: IconHandStop,
  audit: IconHistory,
};

/**
 * Settings (UX-07), visual system v3 admin layout: a vertical section nav on the left (above the content on phone),
 * and on the right the selected section as h2, a one-line description, then its content in one white panel.
 * Manager and admin only.
 */
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

  const current = section as SectionKey;

  return (
    <Stack>
      <PageHeader title={t('settings.title')} />
      <Flex gap="xl" align="flex-start" direction={{ base: 'column', sm: 'row' }}>
        <Stack gap={4} component="nav" aria-label={t('settings.title')} w={{ base: '100%', sm: '12.5rem' }} style={{ flexShrink: 0 }}>
          {SECTIONS.map((s) => {
            const SectionIcon = ICONS[s];
            return (
              <MNavLink
                key={s}
                component={NavLink}
                to={`/settings/${s}`}
                label={t(`settings.sec.${s}`)}
                active={s === current}
                leftSection={<SectionIcon size={ICON_SIZE_NAV} stroke={ICON_STROKE} aria-hidden />}
              />
            );
          })}
        </Stack>
        <Box style={{ flex: 1, minWidth: 0 }} w="100%">
          <section aria-labelledby="settings-section-title">
            <Stack gap="xs">
              <Title order={2} id="settings-section-title" fz="1rem" fw={600} c="var(--color-ink)">
                {t(`settings.sec.${current}`)}
              </Title>
              <Text c="dimmed" size="sm">
                {t(`settings.sub.${current}`)}
              </Text>
            </Stack>
            <Paper withBorder p="md" mt="md" style={{ boxShadow: 'none' }}>
              {current === 'users' && <UsersSection />}
              {current === 'roles' && <RolesSection />}
              {current === 'statuses' && <StatusListsSection />}
              {current === 'import' && <ImportSection />}
              {current === 'retention' && <RetentionSection />}
              {current === 'offlimits' && <OffLimitsSection />}
              {current === 'audit' && <AuditSection />}
            </Paper>
          </section>
        </Box>
      </Flex>
    </Stack>
  );
}
