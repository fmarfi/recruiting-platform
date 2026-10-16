import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { App } from './App';
import { tokenCss, toneOf } from './theme/tokens';

describe('app shell', () => {
  it('renders the default TR placeholder route', () => {
    render(
      <MemoryRouter initialEntries={['/accounts']}>
        <App />
      </MemoryRouter>,
    );
    expect(screen.getByTestId('screen-title')).toHaveTextContent('Müşteriler');
  });

  it('maps design tokens to CSS variables and falls back to NEUTRAL', () => {
    expect(tokenCss()).toContain('--tone-good-bg:#dff0e6');
    expect(toneOf('nope')).toBe('NEUTRAL');
  });
});
