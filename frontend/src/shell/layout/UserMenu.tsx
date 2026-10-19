import { Menu, Text, UnstyledButton } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';

export function UserMenu() {
  const { t } = useTranslation();
  const { user, logout } = useAuth();
  const nav = useNavigate();
  return (
    <Menu position="bottom-end" withinPortal>
      <Menu.Target>
        <UnstyledButton aria-label={t('shell.userMenu')} style={{ minHeight: 'var(--touch-target-min)', whiteSpace: 'nowrap' }}>
          <Text size="sm" fw={600}>
            {user?.name ?? ''} ▾
          </Text>
        </UnstyledButton>
      </Menu.Target>
      <Menu.Dropdown>
        <Menu.Label>{user?.email}</Menu.Label>
        <Menu.Item onClick={() => void logout().then(() => nav('/login'))}>{t('shell.logout')}</Menu.Item>
      </Menu.Dropdown>
    </Menu>
  );
}
