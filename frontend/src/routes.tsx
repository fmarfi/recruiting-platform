import { Container, Text, Title } from '@mantine/core';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Navigate, Route, Routes, useParams } from 'react-router-dom';
import { AccountPage } from './accounts/detail/AccountPage';
import { AccountsList } from './accounts/list/AccountsList';
import { SettingsPage } from './settings/SettingsPage';
import { AuthProvider } from './shell/auth/AuthProvider';
import { RequireAuth } from './shell/auth/RequireAuth';
import { MyDay } from './shell/home/MyDay';
import { Shell } from './shell/layout/Shell';
import { Login } from './shell/login/Login';
import { ReferenceDataProvider } from './shell/refdata/ReferenceDataProvider';
import { SearchPage } from './shell/search/SearchPage';

// One placeholder per screen in docs/ux/screens/*.md. Feature agents replace the element, not the path.
function Placeholder({ titleKey }: { titleKey: string }) {
  const { t } = useTranslation();
  const params = useParams();
  const id = params.id ?? params.section;
  return (
    <Container py="md" maw="var(--content-max-width)">
      <Title order={1} data-testid="screen-title">
        {t(titleKey)}
        {id ? ` ${id}` : ''}
      </Title>
      <Text c="dimmed">{t('app.placeholder')}</Text>
    </Container>
  );
}

export function AppRoutes() {
  const { i18n } = useTranslation();
  useEffect(() => {
    const setLang = (l: string) => {
      document.documentElement.lang = l;
    };
    setLang(i18n.language);
    i18n.on('languageChanged', setLang);
    return () => i18n.off('languageChanged', setLang);
  }, [i18n]);
  return (
    <AuthProvider>
      <ReferenceDataProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route
            element={
              <RequireAuth>
                <Shell />
              </RequireAuth>
            }
          >
            <Route path="/" element={<MyDay />} />
            <Route path="/accounts" element={<AccountsList />} />
            <Route path="/accounts/:id" element={<AccountPage />} />
            <Route path="/job-orders" element={<Placeholder titleKey="nav.jobOrders" />} />
            <Route path="/job-orders/:id" element={<Placeholder titleKey="screen.jobOrder" />} />
            <Route path="/network" element={<Placeholder titleKey="nav.network" />} />
            <Route path="/people/:id" element={<Placeholder titleKey="screen.person" />} />
            <Route path="/search" element={<SearchPage />} />
            <Route path="/settings" element={<Navigate to="/settings/users" replace />} />
            <Route path="/settings/:section" element={<SettingsPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </ReferenceDataProvider>
    </AuthProvider>
  );
}
