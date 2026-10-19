import { ActionIcon, AppShell, Group, Text } from '@mantine/core';
import { useMediaQuery } from '@mantine/hooks';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Outlet } from 'react-router-dom';
import { tokens } from '../../theme/tokens';
import { QuickAdd } from '../../pipeline/quickadd/QuickAdd';
import { GlobalSearch } from '../search/GlobalSearch';
import { UtilityBar } from '../utility/UtilityBar';
import { LanguageSwitch } from './LanguageSwitch';
import { NavItems } from './NavItems';
import { NewMenu } from './NewMenu';
import { TabBar } from './TabBar';
import { UserMenu } from './UserMenu';

const HEADER_HEIGHT = 48;
const TAB_BAR_HEIGHT = 40;
const UTILITY_BAR_HEIGHT = 36;

/**
 * App frame (UX-09): 48 px global header (name, search, + New, TR/EN, user menu) with a 40 px object tab bar
 * below it on laptop; bottom tab bar plus + button on phone; utility bar footer on laptop.
 */
export function Shell() {
  const { t } = useTranslation();
  const phone = useMediaQuery(`(max-width: ${tokens.breakpoints.phone}px)`);
  const [fab, setFab] = useState(false);
  const [version, setVersion] = useState(0); // bump to re-mount the screen after a quick add

  return (
    <AppShell
      header={{ height: phone ? HEADER_HEIGHT : HEADER_HEIGHT + TAB_BAR_HEIGHT }}
      footer={{ height: phone ? tokens.layout.bottomTabBarHeight : UTILITY_BAR_HEIGHT }}
      padding="md"
      styles={{ main: { background: 'var(--color-surface2)' } }}
    >
      <AppShell.Header style={{ background: 'var(--color-surface)', borderColor: 'var(--color-border)' }}>
        <Group h={HEADER_HEIGHT} px="md" gap="sm" wrap="nowrap">
          <Text fw={700} c="brand" style={{ whiteSpace: 'nowrap' }}>
            {t('app.title')}
          </Text>
          <GlobalSearch />
          <NewMenu onChanged={() => setVersion((v) => v + 1)} />
          <LanguageSwitch />
          <UserMenu />
        </Group>
        {!phone && <TabBar />}
      </AppShell.Header>
      <AppShell.Main>
        <Outlet key={version} />
      </AppShell.Main>
      {phone ? (
        <AppShell.Footer p={4}>
          <NavItems vertical={false} />
        </AppShell.Footer>
      ) : (
        <UtilityBar />
      )}
      {phone && (
        <>
          <ActionIcon
            size={56}
            radius="xl"
            variant="filled"
            aria-label={t('quickAdd.new')}
            onClick={() => setFab(true)}
            style={{ position: 'fixed', right: 16, bottom: tokens.layout.bottomTabBarHeight + 16, zIndex: 150 }}
          >
            <Text fz={28} lh={1}>+</Text>
          </ActionIcon>
          <QuickAdd opened={fab} onClose={() => setFab(false)} onSaved={() => setVersion((v) => v + 1)} />
        </>
      )}
    </AppShell>
  );
}
