import { MantineProvider } from '@mantine/core';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest';
import i18n from '../../i18n';
import { RedirectTo } from '../../routes';
import { AuthContext, type AuthCtx } from '../auth/useAuth';
import { PinnedSearches } from './NavItems';

const get = vi.fn();
vi.mock('../../api', () => ({ api: { GET: (...a: unknown[]) => get(...a) } }));

const auth = { user: { id: 'u1', name: 'U' }, status: 'authed', isManager: false, isAdmin: false } as unknown as AuthCtx;

beforeAll(async () => {
  await i18n.changeLanguage('en');
});
beforeEach(() => get.mockReset());

function renderPinned() {
  render(
    <MantineProvider env="test">
      <AuthContext.Provider value={auth}>
        <MemoryRouter>
          <PinnedSearches collapsed={false} />
        </MemoryRouter>
      </AuthContext.Provider>
    </MantineProvider>,
  );
}

describe('PinnedSearches', () => {
  it('lists my open searches as links and shows loading first', async () => {
    get.mockResolvedValue({ data: { content: [{ id: 7, title: 'CFO Search' }] } });
    renderPinned();
    expect(screen.getByText('Loading...')).toBeInTheDocument();
    const link = await screen.findByRole('link', { name: 'CFO Search' });
    expect(link).toHaveAttribute('href', '/searches/7');
    expect(get.mock.calls[0]![1].params.query).toMatchObject({ open: true, ownerId: 'u1', size: 5 });
  });

  it('shows an empty line', async () => {
    get.mockResolvedValue({ data: { content: [] } });
    renderPinned();
    expect(await screen.findByText('No open searches')).toBeInTheDocument();
  });

  it('shows an error line when the API fails', async () => {
    get.mockResolvedValue({ error: { code: 'X' } });
    renderPinned();
    expect(await screen.findByRole('alert')).toHaveTextContent('Could not load searches.');
  });
});

function Where() {
  const l = useLocation();
  return <div data-testid="where">{l.pathname + l.search}</div>;
}

function at(path: string) {
  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/accounts" element={<RedirectTo base="/clients" />} />
        <Route path="/accounts/:id" element={<RedirectTo base="/clients" />} />
        <Route path="/people/:id" element={<RedirectTo base="/candidates" />} />
        <Route path="/network/cv-intake" element={<RedirectTo base="/candidates/cv-intake" />} />
        <Route path="/network/duplicates" element={<RedirectTo base="/candidates/duplicates" />} />
        <Route path="*" element={<Where />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('old path redirects', () => {
  it('keeps the id', () => {
    at('/people/42');
    expect(screen.getByTestId('where')).toHaveTextContent('/candidates/42');
  });
  it('keeps the query string', () => {
    at('/accounts?status=LEAD&owner=all');
    expect(screen.getByTestId('where')).toHaveTextContent('/clients?status=LEAD&owner=all');
  });
  it('keeps id and query together', () => {
    at('/accounts/a1?tab=timeline');
    expect(screen.getByTestId('where')).toHaveTextContent('/clients/a1?tab=timeline');
  });
  it('still resolves /network/cv-intake and /network/duplicates', () => {
    at('/network/cv-intake');
    expect(screen.getByTestId('where')).toHaveTextContent('/candidates/cv-intake');
  });
  it('resolves duplicates', () => {
    at('/network/duplicates');
    expect(screen.getByTestId('where')).toHaveTextContent('/candidates/duplicates');
  });
});
