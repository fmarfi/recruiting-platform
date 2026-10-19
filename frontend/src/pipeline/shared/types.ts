import type { components } from '../../api';

export type Schemas = components['schemas'];
export type RefData = Schemas['ReferenceData'];
export type StatusDef = Schemas['StatusDefinition'];
export type LifecycleKey = keyof RefData['lifecycles'];

