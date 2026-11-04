import { Group, Stack, Text } from '@mantine/core';
import { useEffect, useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { NavLink } from 'react-router-dom';
import { api } from '../../api';
import { useAuth } from '../auth/useAuth';
import { ClientIcon, HomeIcon, ICON_SIZE_NAV, ICON_STROKE, PersonIcon, SearchIcon, SettingsIcon } from '../components/objectIcons';

const ICONS: Record<string, ReactNode> = {
  'nav.myDay': <HomeIcon size={ICON_SIZE_NAV} stroke={ICON_STROKE} aria-hidden style={{ flex: 'none' }} />,
  'nav.accounts': <ClientIcon size={ICON_SIZE_NAV} stroke={ICON_STROKE} aria-hidden style={{ flex: 'none' }} />,
  'nav.jobOrders': <SearchIcon size={ICON_SIZE_NAV} stroke={ICON_STROKE} aria-hidden style={{ flex: 'none' }} />,
  'nav.network': <PersonIcon size={ICON_SIZE_NAV} stroke={ICON_STROKE} aria-hidden style={{ flex: 'none' }} />,
  'nav.settings': <SettingsIcon size={ICON_SIZE_NAV} stroke={ICON_STROKE} aria-hidden style={{ flex: 'none' }} />,
};

function useNavItems() {
  // Four areas only; Settings/Admin live in the user menu.
  const items: [string, string][] = [
    ['/', 'nav.myDay'],
    ['/searches', 'nav.jobOrders'],
    ['/candidates', 'nav.network'],
    ['/clients', 'nav.accounts'],
  ];
  return items;
}

/** Up to 5 open searches owned by the current user, one click each (laptop rail and drawer). */
export function PinnedSearches({ collapsed, onNavigate }: { collapsed: boolean; onNavigate?: () => void }) {
  const { t } = useTranslation();
  const { user } = useAuth();
  const [rows, setRows] = useState<{ id: string; title: string }[] | null>(null);
  const [failed, setFailed] = useState(false);
  const uid = user?.id;
  useEffect(() => {
    if (!uid) return;
    let live = true;
    void api
      .GET('/job-orders', { params: { query: { open: true, ownerId: uid, page: 0, size: 5, sort: ['openedOn,desc'] } } })
      .then((r) => {
        if (!live) return;
        if (r.error || !r.data) {
          setFailed(true);
          setRows([]);
          return;
        }
        setRows((r.data?.content ?? []).slice(0, 5).map((j) => ({ id: String(j.id), title: j.title })));
      })
      .catch(() => {
        if (!live) return;
        setFailed(true);
        setRows([]);
      });
    return () => {
      live = false;
    };
  }, [uid]);
  if (collapsed) return null;
  return (
    <Stack gap={2} component="nav" aria-label={t('nav.pinned')} mt="md">
      <Text size="sm" fw={600} px="var(--space-3)" style={{ color: 'var(--color-chromeInk2)' }}>
        {t('nav.pinned')}
      </Text>
      {rows === null && (
        <Text size="sm" px="var(--space-3)" role="status" style={{ color: 'var(--color-chromeInk2)' }}>
          {t('nav.pinnedLoading')}
        </Text>
      )}
      {rows && rows.length === 0 && failed && (
        <Text size="sm" px="var(--space-3)" role="alert" style={{ color: 'var(--color-chromeInk2)' }}>
          {t('nav.pinnedError')}
        </Text>
      )}
      {rows && rows.length === 0 && !failed && (
        <Text size="sm" px="var(--space-3)" style={{ color: 'var(--color-chromeInk2)' }}>
          {t('nav.noPinned')}
        </Text>
      )}
      {(rows ?? []).map((r) => (
        <NavLink
          key={r.id}
          to={`/searches/${r.id}`}
          onClick={onNavigate}
          className="nav-link"
          title={r.title}
          style={({ isActive }) => ({
            minHeight: 'var(--touch-target-min)',
            display: 'flex',
            alignItems: 'center',
            padding: '0 var(--space-3)',
            borderRadius: 'var(--radius-md)',
            textDecoration: 'none',
            minWidth: 0,
            color: isActive ? 'var(--color-chromeInk)' : 'var(--color-chromeInk2)',
            background: isActive ? 'var(--color-chromeActive)' : 'transparent',
          })}
        >
          <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{r.title}</span>
        </NavLink>
      ))}
    </Stack>
  );
}

/** Main navigation on midnight chrome: a column in the rail or the phone drawer (icons only when the rail is collapsed); the row variant is kept for callers that need a horizontal bar. */
export function NavItems({ vertical, collapsed = false, onNavigate }: { vertical: boolean; collapsed?: boolean; onNavigate?: () => void }) {
  const { t } = useTranslation();
  const items = useNavItems();
  const main = (
    <Group
      component="nav"
      aria-label={t('shell.mainNav')}
      gap="xs"
      wrap="nowrap"
      style={{ flexDirection: vertical ? 'column' : 'row', alignItems: vertical ? 'stretch' : 'center' }}
    >
      {items.map(([to, key]) => (
        <NavLink
          key={to}
          to={to}
          end={to === '/'}
          onClick={onNavigate}
          className="nav-link"
          title={collapsed ? t(key) : undefined}
          aria-label={collapsed ? t(key) : undefined}
          style={({ isActive }) => ({
            minHeight: 'var(--touch-target-min)',
            display: 'flex',
            flexDirection: vertical ? 'row' : 'column',
            alignItems: 'center',
            justifyContent: vertical && !collapsed ? 'flex-start' : 'center',
            gap: vertical ? 'var(--space-3)' : 'var(--space-1)',
            padding: vertical ? (collapsed ? 0 : '0 var(--space-3)') : 'var(--space-1) var(--space-0)',
            fontSize: vertical ? '0.9375rem' : '0.75rem',
            whiteSpace: 'nowrap',
            minWidth: 0,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            textAlign: 'center',
            borderRadius: 'var(--radius-md)',
            textDecoration: 'none',
            fontWeight: 500,
            flex: vertical ? undefined : 1,
            color: isActive ? 'var(--color-chromeInk)' : 'var(--color-chromeInk2)',
            background: isActive ? 'var(--color-chromeActive)' : 'transparent',
          })}
        >
          {ICONS[key]}
          {!collapsed && <span>{t(key)}</span>}
        </NavLink>
      ))}
    </Group>
  );
  if (!vertical) return main;
  return (
    <>
      {main}
      <PinnedSearches collapsed={collapsed} onNavigate={onNavigate} />
    </>
  );
}
