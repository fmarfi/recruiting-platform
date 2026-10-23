import type { NewView, SavedView, ViewStore } from './types';

/** localStorage-backed ViewStore, one list per grid id. Storage failures degrade to an in-memory list. */
export function localViewStore<F = unknown>(gridId: string): ViewStore<F> {
  const k = `grid.${gridId}.views`;
  let memory: SavedView<F>[] = [];
  const read = (): SavedView<F>[] => {
    try {
      const v = JSON.parse(localStorage.getItem(k) ?? 'null') as unknown;
      if (Array.isArray(v)) memory = v.filter((x): x is SavedView<F> => !!x && typeof x === 'object' && typeof (x as SavedView).id === 'string' && typeof (x as SavedView).name === 'string');
    } catch {
      /* keep memory */
    }
    return memory;
  };
  const write = (v: SavedView<F>[]) => {
    memory = v;
    try {
      localStorage.setItem(k, JSON.stringify(v));
    } catch {
      /* storage unavailable */
    }
  };
  return {
    async list() {
      return read();
    },
    async save(view: NewView<F>) {
      const all = read();
      const id = view.id ?? `v${Date.now().toString(36)}${Math.random().toString(36).slice(2, 6)}`;
      const saved: SavedView<F> = { ...view, id };
      write(all.some((x) => x.id === id) ? all.map((x) => (x.id === id ? saved : x)) : [...all, saved]);
      return saved;
    },
    async delete(id: string) {
      write(read().filter((x) => x.id !== id));
    },
  };
}
