import createClient, { type Middleware } from 'openapi-fetch';
import type { paths } from './schema';

// Session cookie + CSRF (ADR 0007): the server sets XSRF-TOKEN (readable by JS);
// state-changing requests send it back in X-XSRF-TOKEN.
const SAFE = new Set(['GET', 'HEAD', 'OPTIONS']);

export function readCookie(name: string): string | undefined {
  const hit = document.cookie.split('; ').find((c) => c.startsWith(`${name}=`));
  return hit ? decodeURIComponent(hit.slice(name.length + 1)) : undefined;
}

const csrf: Middleware = {
  onRequest({ request }) {
    if (!SAFE.has(request.method.toUpperCase())) {
      const token = readCookie('XSRF-TOKEN');
      if (token) request.headers.set('X-XSRF-TOKEN', token);
    }
    return request;
  },
};

/**
 * Spring splits a single `sort=name,desc` value at the comma into two sort keys and answers 400 ("unsupported sort key: desc").
 * A repeated parameter is not split, so a lone "key,dir" sort is sent twice (same order twice is harmless).
 * Remove when the backend reads the parameter as one value (change request in docs/process/handoffs/frontend-shell.md).
 */
export const sortFix: Middleware = {
  onRequest({ request }) {
    const url = new URL(request.url);
    const sorts = url.searchParams.getAll('sort');
    if (sorts.length === 1 && sorts[0]!.includes(',')) {
      url.searchParams.append('sort', sorts[0]!);
      return new Request(url, request);
    }
    return request;
  },
};

export const API_BASE = '/api/v1';

export function createApiClient(baseUrl = API_BASE) {
  const client = createClient<paths>({ baseUrl, credentials: 'include' });
  client.use(csrf);
  client.use(sortFix);
  return client;
}

export const api = createApiClient();
