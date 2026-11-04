import { ActionIcon, AppShell, Burger, Group, Text } from '@mantine/core';
import { useMediaQuery } from '@mantine/hooks';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { UndoToastProvider } from '../edit';
import { tokens } from '../../theme/tokens';
import { QuickAdd } from '../../pipeline/quickadd/QuickAdd';
import { CommandButton, useCommandHotkey } from '../command/CommandButton';
import { CommandPalette } from '../command/CommandPalette';
import { GlobalSearch } from '../search/GlobalSearch';
import { QuickLogHost } from '../quicklog';
import { LanguageSwitch } from './LanguageSwitch';
import { Logo } from './Logo';
import { NavItems } from './NavItems';
import { NewMenu } from './NewMenu';
import { UserMenu } from './UserMenu';

const RAIL_KEY = 'mihenk.rail.collapsed';
function readCollapsed() {
  try {
    return localStorage.getItem(RAIL_KEY) === '1';
  } catch {
    return false;
  }
}

/** App frame (UX-09, brand chrome): 232 px midnight rail (collapsible to 64 px) on laptop, bottom bar on phone, 56 px header with search, + New, TR/EN and user menu. */
export function Shell() {
  const { pathname } = useLocation();
  const { t } = useTranslation();
  const phone = useMediaQuery(`(max-width: ${tokens.breakpoints.phone}px)`);
  const [drawer, setDrawer] = useState(false);
  const [fab, setFab] = useState(false);
  const [version, setVersion] = useState(0); // bump to re-mount the screen after a quick add
  const [command, setCommand] = useState(false);
  const [collapsed, setCollapsed] = useState(readCollapsed);
  useEffect(() => {
    try {
      localStorage.setItem(RAIL_KEY, collapsed ? '1' : '0');
    } catch {
      /* storage unavailable: the rail simply does not remember */
    }
  }, [collapsed]);
  const railCollapsed = collapsed && !phone;
  const toggleCommand = useCallback(() => setCommand((o) => !o), []);
  useCommandHotkey(toggleCommand);

  return (
    <AppShell
      header={{ height: tokens.layout.headerHeight }}
      navbar={{ width: railCollapsed ? tokens.spacing.sidebarCollapsedWidth : tokens.spacing.sidebarWidth, breakpoint: 'sm', collapsed: { mobile: !drawer || !phone, desktop: !!phone } }}
      footer={{ height: phone ? tokens.layout.bottomTabBarHeight : 0 }}
      padding="md"
    >
      <a className="skip-link" href="#main-content">
        {t('shell.skipToContent')}
      </a>
      <AppShell.Header className="chrome-header">
        <Group h="100%" px="md" gap="sm" wrap="nowrap">
          {phone && <Burger opened={drawer} onClick={() => setDrawer((o) => !o)} size="sm" color="white" aria-label={t('shell.menu')} />}
          <Link to="/" className="brand-link" style={{ marginInlineEnd: 8 }}>
            <Logo onDark compact={phone} size={32} />
          </Link>
          <GlobalSearch />
          <CommandButton onClick={() => setCommand(true)} />
          <NewMenu onChanged={() => setVersion((v) => v + 1)} />
          <LanguageSwitch />
          <UserMenu />
        </Group>
      </AppShell.Header>
      <AppShell.Navbar p={railCollapsed ? 'xs' : 'md'} className="chrome-rail">
        <NavItems vertical collapsed={railCollapsed} onNavigate={() => setDrawer(false)} />
        {!phone && (
          <button
            type="button"
            className="rail-toggle"
            onClick={() => setCollapsed((c) => !c)}
            aria-label={collapsed ? t('shell.expandNav') : t('shell.collapseNav')}
            aria-expanded={!collapsed}
            title={collapsed ? t('shell.expandNav') : t('shell.collapseNav')}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden focusable="false">
              {collapsed ? <path d="M9 6l6 6-6 6" /> : <path d="M15 6l-6 6 6 6" />}
            </svg>
          </button>
        )}
      </AppShell.Navbar>
      <AppShell.Main id="main-content" tabIndex={-1} style={{ outline: 'none' }}>
        {/* One undo toast host; keyed by path so a pending undo is dropped when another record opens. */}
        <UndoToastProvider key={pathname}>
          <Outlet key={version} />
          <QuickLogHost onDone={() => setVersion((v) => v + 1)} />
        </UndoToastProvider>
      </AppShell.Main>
      <CommandPalette opened={command} onClose={() => setCommand(false)} onChanged={() => setVersion((v) => v + 1)} />
      {phone && (
        <AppShell.Footer p={4} className="chrome-rail">
          <NavItems vertical={false} />
        </AppShell.Footer>
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
