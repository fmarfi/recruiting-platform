import { Avatar, Group, Menu, Text, UnstyledButton } from '@mantine/core';
import { IconCheck } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { api } from '../../api';
import { useAuth } from '../auth/useAuth';
import { ICON_SIZE, ICON_STROKE } from '../components/objectIcons';

export function UserMenu() {
  const { t, i18n } = useTranslation();
  const { user, logout, isManager } = useAuth();
  const nav = useNavigate();
  return (
    <Menu position="bottom-end" withinPortal>
      <Menu.Target>
        <UnstyledButton aria-label={t('shell.userMenu')} style={{ minHeight: 'var(--touch-target-min)', whiteSpace: 'nowrap' }}>
          <Group gap="xs" wrap="nowrap">
            <Avatar size={32} radius="xl" color="brand" aria-hidden>
              {(user?.name ?? '?')
                .split(/\s+/)
                .map((w) => w[0])
                .slice(0, 2)
                .join('')
                .toUpperCase()}
            </Avatar>
            <Text size="sm" fw={600} visibleFrom="md">
              {user?.name ?? ''}
            </Text>
          </Group>
        </UnstyledButton>
      </Menu.Target>
      <Menu.Dropdown>
        <Menu.Label>{user?.email}</Menu.Label>
        <Menu.Divider />
        <Menu.Label>{t('shell.language')}</Menu.Label>
        {(['tr', 'en'] as const).map((l) => (
          <Menu.Item
            key={l}
            closeMenuOnClick={false}
            rightSection={i18n.language.startsWith(l) ? <IconCheck size={ICON_SIZE} stroke={ICON_STROKE} aria-hidden /> : null}
            onClick={() => {
              void i18n.changeLanguage(l);
              if (user) void api.PATCH('/auth/me', { body: { language: l.toUpperCase() as 'TR' | 'EN' } });
            }}
          >
            {l.toUpperCase()}
          </Menu.Item>
        ))}
        <Menu.Divider />
        {isManager && <Menu.Item onClick={() => nav('/settings/users')}>{t('nav.settings')}</Menu.Item>}
        <Menu.Item onClick={() => void logout().then(() => nav('/login'))}>{t('shell.logout')}</Menu.Item>
      </Menu.Dropdown>
    </Menu>
  );
}
