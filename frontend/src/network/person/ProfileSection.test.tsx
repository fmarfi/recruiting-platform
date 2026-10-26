import { MantineProvider } from '@mantine/core';
import { render as rtl, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import '../../i18n';
import type { Schemas } from '../../pipeline/shared';
import { ProfileSection } from './ProfileSection';

const render = (ui: React.ReactElement) => rtl(<MantineProvider>{ui}</MantineProvider>);
const base = { sourceChannel: 'REFERRAL', noticePeriodDays: 30, compensation: { currency: 'TRY', period: 'ANNUAL', basis: 'GROSS', expected: 987654, currentBase: 123456 } } as unknown as Schemas['Person'];

describe('ProfileSection compensation', () => {
  it('never renders restricted compensation, even if a value slipped through', () => {
    const { container } = render(<ProfileSection person={{ ...base, compensationRestricted: true }} />);
    expect(container.textContent).not.toMatch(/987|123/);
    expect(screen.queryByTestId('compensation')).toBeNull();
    expect(screen.getByTestId('compensation-restricted')).toBeTruthy();
  });
  it('renders it when not restricted', () => {
    render(<ProfileSection person={{ ...base, compensationRestricted: false }} />);
    expect(screen.getByTestId('compensation').textContent).toMatch(/987/);
  });
});
