import { Group } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';

function useNavItems() {
  const { isManager } = useAuth();
  const items: [string, string][] = [
    ['/', 'nav.myDay'],
    ['/accounts', 'nav.accounts'],
    ['/job-orders', 'nav.jobOrders'],
    ['/network', 'nav.network'],
  ];
  // Settings is hidden from consultants (docs/ux/screens/settings.md).
  if (isManager) items.push(['/settings/users', 'nav.settings']);
  return items;
}

/** Main navigation: a column in the sidebar on laptop, a row in the bottom tab bar on phone. */
export function NavItems({ vertical, onNavigate }: { vertical: boolean; onNavigate?: () => void }) {
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
          style={({ isActive }) => ({
            minHeight: 'var(--touch-target-min)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: vertical ? 'flex-start' : 'center',
            padding: '0 var(--space-3)',
            borderRadius: 'var(--radius-md)',
            textDecoration: 'none',
            fontWeight: 600,
            flex: vertical ? undefined : 1,
            color: isActive ? 'var(--color-accent)' : 'var(--color-ink2)',
            background: isActive ? 'var(--color-accentSoft)' : 'transparent',
          })}
        >
          {t(key)}
        </NavLink>
      ))}
    </Group>
  );
}
