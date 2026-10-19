import { useEffect, useState } from 'react';
import { api, type components } from '../../api';

export type SearchResults = components['schemas']['SearchResults'];

/** Debounced (200 ms) search of 3 hits per group once 2+ characters are typed. */
export function useTypeahead(q: string): SearchResults | null {
  const [res, setRes] = useState<SearchResults | null>(null);
  const term = q.trim();
  useEffect(() => {
    if (term.length < 2) {
      setRes(null);
      return;
    }
    let alive = true;
    const h = setTimeout(() => {
      void api.GET('/search', { params: { query: { q: term, limit: 3 } } }).then(({ data }) => alive && data && setRes(data));
    }, 200);
    return () => {
      alive = false;
      clearTimeout(h);
    };
  }, [term]);
  return res;
}
