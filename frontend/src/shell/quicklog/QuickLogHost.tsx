import { useEffect, useState } from 'react';
import { matchPath, useLocation } from 'react-router-dom';
import { QuickLog } from './QuickLog';

const OPEN_EVENT = 'mihenk:quicklog';

export interface QuickLogContext {
  personId?: string;
  accountId?: string;
  jobOrderId?: string;
}

/** Opens the global composer from anywhere (command palette, + New menu). Context defaults to the current page. */
export function openQuickLog(ctx?: QuickLogContext) {
  window.dispatchEvent(new CustomEvent(OPEN_EVENT, { detail: ctx }));
}

/** What the current route is about: a person, a client or a search. */
export function contextFromPath(pathname: string): QuickLogContext {
  const person = matchPath('/candidates/:id', pathname)?.params.id;
  if (person) return { personId: person };
  const account = matchPath('/clients/:id', pathname)?.params.id;
  if (account) return { accountId: account };
  const job = matchPath('/searches/:id', pathname)?.params.id;
  if (job) return { jobOrderId: job };
  return {};
}

/** Global shortcut: Alt+L opens the composer, prefilled from the page. Mount inside the undo toast provider. */
export function QuickLogHost({ onDone }: { onDone?: () => void }) {
  const { pathname } = useLocation();
  const [ctx, setCtx] = useState<QuickLogContext | null>(null);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.altKey && !e.ctrlKey && !e.metaKey && !e.shiftKey && e.code === 'KeyL') {
        e.preventDefault();
        setCtx(contextFromPath(pathname));
      }
    };
    const onOpen = (e: Event) => setCtx({ ...contextFromPath(pathname), ...((e as CustomEvent<QuickLogContext | undefined>).detail ?? {}) });
    window.addEventListener('keydown', onKey);
    window.addEventListener(OPEN_EVENT, onOpen);
    return () => {
      window.removeEventListener('keydown', onKey);
      window.removeEventListener(OPEN_EVENT, onOpen);
    };
  }, [pathname]);

  return <QuickLog opened={ctx !== null} onClose={() => setCtx(null)} {...(ctx ?? {})} onDone={onDone} />;
}
