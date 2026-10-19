import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Navigate, Route, Routes } from 'react-router-dom';
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
import { JobOrdersList } from './pipeline/joborders/list/JobOrdersList';
import { JobOrderPage } from './pipeline/joborders/detail/JobOrderPage';
import { NetworkList } from './network/list/NetworkList';
import { PersonPage } from './network/person/PersonPage';

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
            <Route path="/job-orders" element={<JobOrdersList />} />
            <Route path="/job-orders/:id" element={<JobOrderPage />} />
            <Route path="/network" element={<NetworkList />} />
            <Route path="/people/:id" element={<PersonPage />} />
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
