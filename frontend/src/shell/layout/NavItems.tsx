import { Group, Stack, Text } from '@mantine/core';
import { useEffect, useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { NavLink } from 'react-router-dom';
import { api } from '../../api';
import { useAuth } from '../auth/useAuth';

const icon = (d: ReactNode) => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" aria-hidden focusable="false" style={{ flex: 'none' }}>
    {d}
  </svg>
);
const ICONS: Record<string, ReactNode> = {
  'nav.myDay': icon(<><path d="M5 12l7-7 7 7" /><path d="M6 10v9h12v-9" /></>),
  'nav.accounts': icon(<><path d="M4 20V6l8-2v16" /><path d="M12 9h8v11" /><path d="M8 9h1M8 13h1M8 17h1M16 13h1M16 17h1" /></>),
  'nav.jobOrders': icon(<><circle cx="12" cy="12" r="8" /><circle cx="12" cy="12" r="3.5" /><path d="M12 12l6-6" /></>),
  'nav.network': icon(<><circle cx="12" cy="8" r="3.5" /><path d="M5 20c0-3.9 3.1-6 7-6s7 2.1 7 6" /></>),
  'nav.settings': icon(<><circle cx="12" cy="12" r="3" /><path d="M12 3v3M12 18v3M3 12h3M18 12h3M5.6 5.6l2.1 2.1M16.3 16.3l2.1 2.1M18.4 5.6l-2.1 2.1M7.7 16.3l-2.1 2.1" /></>),
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
      <Text size="xs" fw={700} tt="uppercase" px="var(--space-3)" style={{ color: 'var(--color-chromeInk2)' }}>
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

/** Main navigation on midnight chrome: a column in the rail on laptop (icons only when collapsed), a row in the bottom bar on phone. */
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
            gap: vertical ? 12 : 2,
            padding: vertical ? (collapsed ? 0 : '0 var(--space-3)') : '4px 2px',
            fontSize: vertical ? '0.9375rem' : 12,
            whiteSpace: 'nowrap',
            minWidth: 0,
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            textAlign: 'center',
            borderRadius: 'var(--radius-md)',
            textDecoration: 'none',
            fontWeight: 600,
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
