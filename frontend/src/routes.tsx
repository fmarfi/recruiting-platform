import { Container, Text, Title } from '@mantine/core';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { NavLink, Navigate, Route, Routes, useParams } from 'react-router-dom';
import { JobOrdersList } from './pipeline/joborders/list/JobOrdersList';
import { JobOrderPage } from './pipeline/joborders/detail/JobOrderPage';
import { NetworkList } from './network/list/NetworkList';
import { QuickAddLauncher } from './pipeline/quickadd/QuickAddLauncher';
import { PersonPage } from './network/person/PersonPage';

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

const NAV = [
  ['/', 'nav.myDay'],
  ['/accounts', 'nav.accounts'],
  ['/job-orders', 'nav.jobOrders'],
  ['/network', 'nav.network'],
  ['/settings/users', 'nav.settings'],
] as const;

export function AppRoutes() {
  const { t, i18n } = useTranslation();
  useEffect(() => {
    const setLang = (l: string) => {
      document.documentElement.lang = l;
    };
    setLang(i18n.language);
    i18n.on('languageChanged', setLang);
    return () => i18n.off('languageChanged', setLang);
  }, [i18n]);
  return (
    <>
      <nav style={{ display: 'flex', gap: 'var(--space-4)', padding: 'var(--space-3)' }}>
        {NAV.map(([to, key]) => (
          <NavLink key={to} to={to} end={to === '/'}>
            {t(key)}
          </NavLink>
        ))}
        <QuickAddLauncher />
        <button type="button" onClick={() => void i18n.changeLanguage(i18n.language === 'tr' ? 'en' : 'tr')}>
          {i18n.language === 'tr' ? 'EN' : 'TR'}
        </button>
      </nav>
      <Routes>
        <Route path="/" element={<Placeholder titleKey="nav.myDay" />} />
        <Route path="/login" element={<Placeholder titleKey="nav.login" />} />
        <Route path="/accounts" element={<Placeholder titleKey="nav.accounts" />} />
        <Route path="/accounts/:id" element={<Placeholder titleKey="screen.account" />} />
        <Route path="/job-orders" element={<JobOrdersList />} />
        <Route path="/job-orders/:id" element={<JobOrderPage />} />
        <Route path="/network" element={<NetworkList />} />
        <Route path="/people/:id" element={<PersonPage />} />
        <Route path="/search" element={<Placeholder titleKey="nav.search" />} />
        <Route path="/settings" element={<Navigate to="/settings/users" replace />} />
        <Route path="/settings/:section" element={<Placeholder titleKey="screen.settingsSection" />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </>
  );
}
