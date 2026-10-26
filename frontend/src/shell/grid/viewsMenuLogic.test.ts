import { describe, expect, it } from 'vitest';
import type { SavedView } from './types';
import { effectiveShare, findOverwrite, saveVisibility, shouldWarnQ } from './viewsMenuLogic';

const v = (over: Partial<SavedView<unknown>>): SavedView<unknown> => ({ id: 'a', name: 'Mine', filters: {}, ...over });

describe('views menu logic', () => {
  it('finds only own or editable views to overwrite', () => {
    const views = [v({ id: '1', name: 'Team', mine: false, editable: false }), v({ id: '2', name: 'Mine' })];
    expect(findOverwrite(views, ' mine ')?.id).toBe('2');
    expect(findOverwrite(views, 'team')).toBeUndefined();
    expect(findOverwrite(views, '')).toBeUndefined();
  });
  it('keeps an overwritten SHARED view shared unless the user unticks', () => {
    const shared = v({ visibility: 'SHARED' });
    expect(effectiveShare(null, shared)).toBe(true);
    expect(effectiveShare(false, shared)).toBe(false);
    expect(effectiveShare(true, undefined)).toBe(true);
    expect(effectiveShare(null, v({ visibility: 'PRIVATE' }))).toBe(false);
  });
  it('sends PRIVATE to unshare for managers, keeps existing for others', () => {
    expect(saveVisibility(true, false, v({ visibility: 'SHARED' }))).toBe('PRIVATE');
    expect(saveVisibility(true, true, undefined)).toBe('SHARED');
    expect(saveVisibility(false, false, v({ visibility: 'PRIVATE' }))).toBe('PRIVATE');
    expect(saveVisibility(false, false, undefined)).toBeUndefined();
  });
  it('warns about free text when sharing, also for an overwritten shared view', () => {
    expect(shouldWarnQ(effectiveShare(null, v({ visibility: 'SHARED' })), true)).toBe(true);
    expect(shouldWarnQ(false, true)).toBe(false);
    expect(shouldWarnQ(true, false)).toBe(false);
  });
});
