import { ActionIcon, AppShell, Box, Burger, Group } from '@mantine/core';
import { IconChevronLeft, IconChevronRight, IconPlus } from '@tabler/icons-react';
import { useMediaQuery } from '@mantine/hooks';
import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { UndoToastProvider } from '../edit';
import { tokens } from '../../theme/tokens';
import { QuickAdd } from '../../pipeline/quickadd/QuickAdd';
import { ICON_SIZE_NAV, ICON_STROKE } from '../components/objectIcons';
import { CommandButton, useCommandHotkey } from '../command/CommandButton';
import { CommandPalette } from '../command/CommandPalette';
import { GlobalSearch } from '../search/GlobalSearch';
import { QuickLogHost } from '../quicklog';
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

/**
 * App frame (UX-09, brand chrome): midnight rail (collapsible) on laptop; on phone the same nav lives in a burger drawer,
 * and the bottom-right + button is the only add entry. 56 px header with search, command, + New, TR/EN and user menu.
 */
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
      padding="md"
    >
      <a className="skip-link" href="#main-content">
        {t('shell.skipToContent')}
      </a>
      <AppShell.Header className="chrome-header">
        <Group h="100%" px="md" gap="sm" wrap="nowrap">
          {phone && <Burger opened={drawer} onClick={() => setDrawer((o) => !o)} size="sm" color="var(--color-chromeInk)" aria-label={t('shell.menu')} />}
          <Link to="/" className="brand-link">
            <Logo onDark compact={phone} size={32} />
          </Link>
          <GlobalSearch />
          <CommandButton onClick={() => setCommand(true)} />
          {/* On phone the bottom-right + button opens the same quick add, so the header keeps only search and language. */}
          <Box visibleFrom="sm">
            <NewMenu onChanged={() => setVersion((v) => v + 1)} />
          </Box>
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
            {collapsed ? <IconChevronRight size={ICON_SIZE_NAV} stroke={ICON_STROKE} aria-hidden /> : <IconChevronLeft size={ICON_SIZE_NAV} stroke={ICON_STROKE} aria-hidden />}
          </button>
        )}
      </AppShell.Navbar>
      {/* Phone: leave room under the content for the floating + button (56 px button, 16 px inset). */}
      <AppShell.Main id="main-content" tabIndex={-1} style={{ outline: 'none', paddingBottom: phone ? 'calc(var(--space-8) * 2)' : undefined }}>
        {/* One undo toast host; keyed by path so a pending undo is dropped when another record opens. */}
        <UndoToastProvider key={pathname}>
          <Outlet key={version} />
          <QuickLogHost onDone={() => setVersion((v) => v + 1)} />
        </UndoToastProvider>
      </AppShell.Main>
      <CommandPalette opened={command} onClose={() => setCommand(false)} onChanged={() => setVersion((v) => v + 1)} />
      {phone && (
        <>
          <ActionIcon
            size="xl"
            radius="xl"
            variant="filled"
            aria-label={t('quickAdd.new')}
            onClick={() => setFab(true)}
            style={{ position: 'fixed', right: 'var(--space-4)', bottom: 'var(--space-4)', zIndex: 150, boxShadow: 'var(--shadow-pop)' }}
          >
            <IconPlus size={22} stroke={ICON_STROKE} aria-hidden />
          </ActionIcon>
          <QuickAdd opened={fab} onClose={() => setFab(false)} onSaved={() => setVersion((v) => v + 1)} />
        </>
      )}
    </AppShell>
  );
}
