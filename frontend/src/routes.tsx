import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Navigate, Route, Routes, useLocation, useParams } from 'react-router-dom';
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
import { CvIntakePage } from './network/intake/CvIntakePage';
import { DuplicatesPage } from './network/duplicates/DuplicatesPage';
import { NetworkList } from './network/list/NetworkList';
import { PersonPage } from './network/person/PersonPage';

/** Old path to new path, keeping the id and the query string (filters). */
export function RedirectTo({ base }: { base: string }) {
  const { id } = useParams();
  const { search } = useLocation();
  return <Navigate to={`${base}${id ? `/${id}` : ''}${search}`} replace />;
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
            <Route path="/clients" element={<AccountsList />} />
            <Route path="/clients/:id" element={<AccountPage />} />
            <Route path="/searches" element={<JobOrdersList />} />
            <Route path="/searches/:id" element={<JobOrderPage />} />
            <Route path="/candidates" element={<NetworkList />} />
            <Route path="/candidates/cv-intake" element={<CvIntakePage />} />
            <Route path="/candidates/duplicates" element={<DuplicatesPage />} />
            <Route path="/candidates/:id" element={<PersonPage />} />
            <Route path="/accounts" element={<RedirectTo base="/clients" />} />
            <Route path="/accounts/:id" element={<RedirectTo base="/clients" />} />
            <Route path="/job-orders" element={<RedirectTo base="/searches" />} />
            <Route path="/job-orders/:id" element={<RedirectTo base="/searches" />} />
            <Route path="/network" element={<RedirectTo base="/candidates" />} />
            <Route path="/network/cv-intake" element={<RedirectTo base="/candidates/cv-intake" />} />
            <Route path="/network/duplicates" element={<RedirectTo base="/candidates/duplicates" />} />
            <Route path="/people/:id" element={<RedirectTo base="/candidates" />} />
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
