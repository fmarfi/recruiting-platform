import { MantineProvider } from '@mantine/core';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import '../../../i18n';

// Node's Request rejects relative URLs, so the test client gets an absolute base.
vi.mock('../../../api', async () => {
  const actual = await vi.importActual<typeof import('../../../api')>('../../../api');
  return { ...actual, api: actual.createApiClient('http://localhost/api/v1') };
});

const json = (body: unknown) => new Response(JSON.stringify(body), { status: 200, headers: { 'content-type': 'application/json' } });

describe('JobOrdersList', () => {
  afterEach(() => vi.unstubAllGlobals());

  it('lists job orders from the API', async () => {
    // openapi-fetch captures global fetch when the client is created, so stub before importing the screen.
    vi.resetModules();
    vi.stubGlobal('fetch', vi.fn((input: Request | string) => {
      const url = typeof input === 'string' ? input : input.url;
      if (url.includes('/auth/me')) return Promise.resolve(json({ id: 'u1', name: 'Demo', role: 'CONSULTANT' }));
      if (url.includes('/reference-data')) return Promise.resolve(json({ lifecycles: { jobOrder: { statuses: [] }, pipelineStage: { statuses: [] } } }));
      return Promise.resolve(
        json({ content: [{ id: 'j1', title: 'CFO Search', status: 'OPEN', daysOpen: 4, account: { id: 'a1', name: 'Acme Holding' }, owner: { id: 'u1', name: 'Demo' } }], page: 0, size: 20, totalElements: 1, totalPages: 1 }),
      );
    }));
    const { JobOrdersList } = await import('./JobOrdersList');
    const { AuthContext } = await import('../../../shell/auth/useAuth');
    const { RefContext } = await import('../../../shell/refdata/useReference');
    const auth = { user: { id: 'u1', name: 'Demo', role: 'CONSULTANT' }, status: 'authed', isManager: false, isAdmin: false };
    const ref = { data: { lifecycles: { jobOrder: { statuses: [] }, pipelineStage: { statuses: [] } } }, label: (_k: string, v: string) => v, tone: () => 'gray', def: () => undefined };
    render(
      <MantineProvider>
        <AuthContext.Provider value={auth as never}>
          <RefContext.Provider value={ref as never}>
            <MemoryRouter>
              <JobOrdersList />
            </MemoryRouter>
          </RefContext.Provider>
        </AuthContext.Provider>
      </MantineProvider>,
    );
    expect(await screen.findByText('CFO Search')).toBeInTheDocument();
    expect(screen.getByTestId('screen-title')).toHaveTextContent('Pozisyonlar');
  });
});
