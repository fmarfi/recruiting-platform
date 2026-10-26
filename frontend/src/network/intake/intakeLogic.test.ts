import { describe, expect, it } from 'vitest';
import { attachExisting, expiryInfo, needsPolling, prefillFromDetected } from './intakeLogic';

describe('intake logic', () => {
  it('shows the 14-day expiry as days left and flags the last 3 days', () => {
    const uploaded = Date.parse('2026-10-01T00:00:00Z');
    const expiresAt = new Date(uploaded + 14 * 86_400_000).toISOString();
    expect(expiryInfo({ expiresAt }, uploaded)).toEqual({ days: 14, urgent: false });
    expect(expiryInfo({ expiresAt }, uploaded + 12 * 86_400_000)).toEqual({ days: 2, urgent: true });
    expect(expiryInfo({ expiresAt }, uploaded + 20 * 86_400_000).days).toBe(0);
  });
  it('prefills from the first detected values only', () => {
    expect(prefillFromDetected({ emails: ['a@b.co', 'c@d.co'], phones: [], linkedinUrls: ['https://l/in/x'] })).toEqual({ email: 'a@b.co', phone: '', linkedinUrl: 'https://l/in/x' });
  });
  it('polls only while a pending item is still extracting', () => {
    expect(needsPolling([{ status: 'PENDING', textStatus: 'PENDING' }])).toBe(true);
    expect(needsPolling([{ status: 'PENDING', textStatus: 'DONE' }, { status: 'ATTACHED', textStatus: 'PENDING' }])).toBe(false);
    expect(needsPolling(undefined)).toBe(false);
  });
  it('builds the attach body', () => {
    expect(attachExisting('p1')).toEqual({ personId: 'p1' });
  });
});
