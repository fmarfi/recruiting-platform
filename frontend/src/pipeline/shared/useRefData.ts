// Cached /reference-data: labels, tones and legal moves for every lifecycle.
import { useEffect, useState } from 'react';
import { api } from '../../api';
import type { RefData } from './types';

let refPromise: Promise<RefData | undefined> | undefined;
/** Test hook: forget the cached /reference-data. */
export function resetRefData() {
  refPromise = undefined;
}
export function loadRefData() {
  refPromise ??= api
    .GET('/reference-data')
    .then((r) => r.data)
    .catch(() => {
      refPromise = undefined;
      return undefined;
    });
  return refPromise;
}
export function useRefData(): RefData | undefined {
  const [ref, setRef] = useState<RefData>();
  useEffect(() => {
    let live = true;
    void loadRefData().then((r) => live && setRef(r));
    return () => {
      live = false;
    };
  }, []);
  return ref;
}

