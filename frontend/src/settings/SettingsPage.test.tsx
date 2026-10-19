import { MantineProvider } from '@mantine/core';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import '../i18n';
import { AuthContext, type AuthCtx } from '../shell/auth/useAuth';
import { SettingsPage } from './SettingsPage';

function renderAs(role: 'CONSULTANT' | 'MANAGER' | 'ADMIN', path = '/settings/roles') {
  const ctx = { user: { id: 'u1', role }, status: 'authed', isManager: role !== 'CONSULTANT', isAdmin: role === 'ADMIN' } as unknown as AuthCtx;
  return render(
    <MantineProvider>
      <AuthContext.Provider value={ctx}>
        <MemoryRouter initialEntries={[path]}>
          <Routes>
            <Route path="/settings/:section" element={<SettingsPage />} />
          </Routes>
        </MemoryRouter>
      </AuthContext.Provider>
    </MantineProvider>,
  );
}

describe('SettingsPage', () => {
  it('hides settings from consultants', () => {
    renderAs('CONSULTANT');
    expect(screen.getByRole('alert')).toHaveTextContent('erişiminiz yok');
  });

  it('shows the fixed role matrix to managers', () => {
    renderAs('MANAGER');
    expect(screen.getByText('Sistem yöneticisi')).toBeInTheDocument();
  });
});
