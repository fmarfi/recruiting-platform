import { SegmentedControl } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { api } from '../../api';
import { useAuth } from '../auth/useAuth';

/** TR/EN switch; remembers the choice on the user profile when logged in. */
export function LanguageSwitch() {
  const { i18n } = useTranslation();
  const { user } = useAuth();
  return (
    <SegmentedControl
      size="xs"
      aria-label="Language"
      value={i18n.language.startsWith('tr') ? 'tr' : 'en'}
      data={[
        { value: 'tr', label: 'TR' },
        { value: 'en', label: 'EN' },
      ]}
      onChange={(v) => {
        void i18n.changeLanguage(v);
        if (user) void api.PATCH('/auth/me', { body: { language: v.toUpperCase() as 'TR' | 'EN' } });
      }}
    />
  );
}
