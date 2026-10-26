import type { SavedView, ViewVisibility } from './types';

/** The view a typed name would overwrite: same name (case-insensitive), own or editable. */
export function findOverwrite<F>(views: SavedView<F>[], name: string): SavedView<F> | undefined {
  const n = name.trim().toLowerCase();
  return n ? views.find((v) => v.name.toLowerCase() === n && v.mine !== false && v.editable !== false) : undefined;
}

/**
 * Share state of the save dialog: an explicit tick wins, otherwise an overwritten SHARED view stays shared
 * (so its q loss is announced and the user can untick to make it private).
 */
export function effectiveShare(choice: boolean | null, existing: SavedView<unknown> | undefined): boolean {
  return choice ?? existing?.visibility === 'SHARED';
}

/** Visibility to send: only managers (canShare) choose; others keep what the view has (undefined = server default). */
export function saveVisibility(canShare: boolean, share: boolean, existing: SavedView<unknown> | undefined): ViewVisibility | undefined {
  return canShare ? (share ? 'SHARED' : 'PRIVATE') : existing?.visibility;
}

/** Warn that free text is not stored when the saved view will be shared. */
export function shouldWarnQ(share: boolean, hasFreeText: boolean): boolean {
  return share && hasFreeText;
}
