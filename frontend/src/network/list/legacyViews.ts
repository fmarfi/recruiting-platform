// One-off migration of the pre-grid saved views (network.savedViews.v1) into the grid view store. The old key is removed
// afterwards: a saved search text can hold a person's name (KVKK).
import { DEFAULT_FILTERS } from './networkFilters';

const OLD_KEY = 'network.savedViews.v1';
export const NEW_KEY = 'grid.network.views';

export function migrateLegacyViews(storage: Pick<Storage, 'getItem' | 'setItem' | 'removeItem'> = localStorage): void {
  try {
    const raw = storage.getItem(OLD_KEY);
    if (raw == null) return;
    const old = JSON.parse(raw) as unknown;
    const current = JSON.parse(storage.getItem(NEW_KEY) ?? '[]') as { id: string }[];
    const known = new Set(Array.isArray(current) ? current.map((v) => v.id) : []);
    const moved = (Array.isArray(old) ? old : [])
      .filter((v) => v && typeof v.id === 'string' && typeof v.name === 'string' && v.filters && !known.has(v.id))
      .map((v) => ({ id: v.id as string, name: v.name as string, filters: { ...DEFAULT_FILTERS, ...v.filters } }));
    storage.setItem(NEW_KEY, JSON.stringify([...(Array.isArray(current) ? current : []), ...moved]));
    storage.removeItem(OLD_KEY);
  } catch {
    // blocked storage or corrupt data: leave things as they are
  }
}