// Object tab bar (40 px) under the global header on laptop: My day, Accounts, Job orders, Network, Settings.
import { Box } from '@mantine/core';
import { NavLink } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useNavItems } from './NavItems';

/** Horizontal tabs with an accent underline on the active one. */
export function TabBar() {
  const { t } = useTranslation();
  const items = useNavItems();
  return (
    <Box
      component="nav"
      aria-label={t('shell.mainNav')}
      h={40}
      px="md"
      style={{ display: 'flex', gap: 'var(--space-1)', background: 'var(--color-surface)', borderBottom: '1px solid var(--color-border)', overflowX: 'auto' }}
    >
      {items.map(([to, key]) => (
        <NavLink
          key={to}
          to={to}
          end={to === '/'}
          style={({ isActive }) => ({
            display: 'flex', alignItems: 'center', padding: '0 var(--space-3)', textDecoration: 'none', whiteSpace: 'nowrap',
            fontWeight: isActive ? 600 : 500,
            color: isActive ? 'var(--color-accent)' : 'var(--color-ink2)',
            borderBottom: `3px solid ${isActive ? 'var(--color-accent)' : 'transparent'}`,
          })}
        >
          {t(key)}
        </NavLink>
      ))}
    </Box>
  );
}
