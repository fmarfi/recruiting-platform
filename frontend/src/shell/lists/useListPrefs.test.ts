// Saved views and recents live in the browser only; check they persist and replace by name.
import { act, renderHook } from '@testing-library/react';
import { readRecents, rememberRecent } from '../utility/recents';
import { useSavedViews } from './useListPrefs';

describe('browser-local list preferences', () => {
  beforeEach(() => window.localStorage.clear());

  it('saves, replaces and removes a view', () => {
    const { result } = renderHook(() => useSavedViews<{ q: string }>('t'));
    act(() => result.current.save('A', { q: '1' }));
    act(() => result.current.save('A', { q: '2' }));
    expect(result.current.views).toEqual([{ name: 'A', filters: { q: '2' } }]);
    act(() => result.current.remove('A'));
    expect(result.current.views).toEqual([]);
  });

  it('keeps recents newest first without duplicates', () => {
    rememberRecent({ path: '/a', label: 'A' });
    rememberRecent({ path: '/b', label: 'B' });
    rememberRecent({ path: '/a', label: 'A' });
    expect(readRecents().map((r) => r.path)).toEqual(['/a', '/b']);
  });
});
