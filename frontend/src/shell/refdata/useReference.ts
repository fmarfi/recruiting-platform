import { createContext, useContext } from 'react';
import type { components } from '../../api';

export type ReferenceData = components['schemas']['ReferenceData'];
export type LifecycleKind = keyof ReferenceData['lifecycles'];
export type StatusDefinition = components['schemas']['StatusDefinition'];

export interface Ref {
  data: ReferenceData | null;
  label(kind: LifecycleKind, value: string): string;
  tone(kind: LifecycleKind, value: string): string;
  def(kind: LifecycleKind, value: string): StatusDefinition | undefined;
}

export const RefContext = createContext<Ref | null>(null);

/** Lifecycle labels (TR/EN) and tones from GET /reference-data. */
export function useReference(): Ref {
  const c = useContext(RefContext);
  if (!c) throw new Error('useReference outside ReferenceDataProvider');
  return c;
}
