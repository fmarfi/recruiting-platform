import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { App } from './App';
import { tokenCss, toneOf } from './theme/tokens';

describe('app shell', () => {
  it('sends an anonymous visitor to the TR sign-in screen', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response('{}', { status: 401, headers: { 'content-type': 'application/json' } })));
    render(
      <MemoryRouter initialEntries={['/accounts']}>
        <App />
      </MemoryRouter>,
    );
    expect(await screen.findByRole('button', { name: 'Giriş yap' })).toBeInTheDocument();
    vi.unstubAllGlobals();
  });

  it('maps design tokens to CSS variables and falls back to NEUTRAL', () => {
    expect(tokenCss()).toContain('--tone-good-bg:#dff0e6');
    expect(toneOf('nope')).toBe('NEUTRAL');
  });
});
