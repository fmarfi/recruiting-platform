import { AppShell, Burger, Group, Text } from '@mantine/core';
import { useMediaQuery } from '@mantine/hooks';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Outlet } from 'react-router-dom';
import { tokens } from '../../theme/tokens';
import { GlobalSearch } from '../search/GlobalSearch';
import { LanguageSwitch } from './LanguageSwitch';
import { NavItems } from './NavItems';
import { NewMenu } from './NewMenu';
import { UserMenu } from './UserMenu';

/** App frame (UX-09): left nav on laptop, bottom tab bar on phone, top bar with search, + New, TR/EN and user menu. */
export function Shell() {
  const { t } = useTranslation();
  const phone = useMediaQuery(`(max-width: ${tokens.breakpoints.phone}px)`);
  const [drawer, setDrawer] = useState(false);
  const [version, setVersion] = useState(0); // bump to re-mount the screen after a quick add

  return (
    <AppShell
      header={{ height: 56 }}
      navbar={{ width: tokens.spacing.sidebarWidth, breakpoint: 'sm', collapsed: { mobile: !drawer || !phone, desktop: !!phone } }}
      footer={{ height: phone ? tokens.layout.bottomTabBarHeight : 0 }}
      padding="md"
    >
      <AppShell.Header>
        <Group h="100%" px="md" gap="sm" wrap="nowrap">
          {phone && <Burger opened={drawer} onClick={() => setDrawer((o) => !o)} size="sm" aria-label={t('shell.menu')} />}
          <Text fw={700} c="brand" visibleFrom="sm" style={{ whiteSpace: 'nowrap' }}>
            {t('app.title')}
          </Text>
          <GlobalSearch />
          <NewMenu onChanged={() => setVersion((v) => v + 1)} />
          <LanguageSwitch />
          <UserMenu />
        </Group>
      </AppShell.Header>
      <AppShell.Navbar p="md">
        <NavItems vertical onNavigate={() => setDrawer(false)} />
      </AppShell.Navbar>
      <AppShell.Main>
        <Outlet key={version} />
      </AppShell.Main>
      {phone && (
        <AppShell.Footer p={4}>
          <NavItems vertical={false} />
        </AppShell.Footer>
      )}
    </AppShell>
  );
}
