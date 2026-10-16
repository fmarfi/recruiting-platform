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

export function createApiClient(baseUrl = '/api/v1') {
  const client = createClient<paths>({ baseUrl, credentials: 'include' });
  client.use(csrf);
  return client;
}

export const api = createApiClient();
