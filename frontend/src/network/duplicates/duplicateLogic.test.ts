import { describe, expect, it } from 'vitest';
import { canMerge, checkRequest, duplicateDecision, mergeRequest, strongestReason, type Match } from './duplicateLogic';

const person = { id: 'p1', name: 'A' } as Match['person'];

describe('duplicate prompt decision', () => {
  it('prompts when the check finds anyone, creates otherwise or when the check failed', () => {
    expect(duplicateDecision([{ person, reasons: ['EMAIL'] }])).toBe('prompt');
    expect(duplicateDecision([])).toBe('create');
    expect(duplicateDecision(undefined)).toBe('create');
  });
  it('asks the server only with something to match on', () => {
    const base = { name: '', company: '', email: '', phone: '', linkedinUrl: '' };
    expect(checkRequest({ ...base, name: 'Ali Veli' })).toBeUndefined();
    expect(checkRequest({ ...base, name: 'Ali Veli', company: 'ACME' })).toMatchObject({ name: 'Ali Veli', company: 'ACME', email: null });
    expect(checkRequest({ ...base, email: ' a@b.co ' })?.email).toBe('a@b.co');
    expect(checkRequest({ ...base, linkedinUrl: 'https://x/in/y' })).toBeDefined();
  });
  it('reports the strongest reason', () => {
    expect(strongestReason({ person, reasons: ['NAME_COMPANY', 'PHONE'] })).toBe('PHONE');
  });
});

describe('merge', () => {
  it('is for managers and admins only', () => {
    expect(canMerge('MANAGER')).toBe(true);
    expect(canMerge('ADMIN')).toBe(true);
    expect(canMerge('CONSULTANT')).toBe(false);
    expect(canMerge(undefined)).toBe(false);
  });
  it('needs the survivor version and keeps field order stable', () => {
    expect(mergeRequest('m', undefined, [])).toBeUndefined();
    expect(mergeRequest('m', 3, [])).toEqual({ mergedPersonId: 'm', version: 3 });
    expect(mergeRequest('m', 0, ['email', 'name'])).toEqual({ mergedPersonId: 'm', version: 0, takeFromMerged: ['name', 'email'] });
  });
});
