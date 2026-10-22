import { useState } from 'react';

export type Density = 'comfortable' | 'compact';
const KEY = 'network.density';

/** List row density, remembered in the browser. Storage can be blocked, so every access is guarded. */
export function useDensity(): [Density, (d: Density) => void] {
  const [density, setState] = useState<Density>(() => {
    try {
      return localStorage.getItem(KEY) === 'compact' ? 'compact' : 'comfortable';
    } catch {
      return 'comfortable';
    }
  });
  const setDensity = (d: Density) => {
    setState(d);
    try {
      localStorage.setItem(KEY, d);
    } catch {
      /* ignore */
    }
  };
  return [density, setDensity];
}
