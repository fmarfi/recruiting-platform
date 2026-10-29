import { Group } from '@mantine/core';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { NavLink } from 'react-router-dom';
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
  const { isManager } = useAuth();
  const items: [string, string][] = [
    ['/', 'nav.myDay'],
    ['/accounts', 'nav.accounts'],
    ['/job-orders', 'nav.jobOrders'],
    ['/network', 'nav.network'],
  ];
  // Settings (Admin) is hidden from consultants (docs/product/ux/screens/settings.md).
  if (isManager) items.push(['/settings/users', 'nav.settings']);
  return items;
}

/** Main navigation on midnight chrome: a column in the rail on laptop (icons only when collapsed), a row in the bottom bar on phone. */
export function NavItems({ vertical, collapsed = false, onNavigate }: { vertical: boolean; collapsed?: boolean; onNavigate?: () => void }) {
  const { t } = useTranslation();
  const items = useNavItems();
  return (
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
}
