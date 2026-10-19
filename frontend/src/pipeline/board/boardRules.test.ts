import { canMove, legalMoves, withStage, type Application } from './boardRules';
import type { RefData } from '../shared';

const ref = {
  lifecycles: {
    pipelineStage: {
      statuses: [
        { value: 'SOURCED', next: [{ to: 'SCREENED', reasonRequired: false }, { to: 'REJECTED', reasonRequired: true }] },
        { value: 'REJECTED', next: [] },
      ],
    },
  },
} as unknown as RefData;

describe('boardRules', () => {
  it('offers exactly the moves from reference data', () => {
    expect(legalMoves(ref, 'SOURCED').map((m) => m.to)).toEqual(['SCREENED', 'REJECTED']);
    expect(legalMoves(ref, 'REJECTED')).toEqual([]);
    expect(legalMoves(undefined, 'SOURCED')).toEqual([]);
  });

  it('flags moves that need a reason and rejects illegal ones', () => {
    expect(canMove(ref, 'SOURCED', 'REJECTED')?.reasonRequired).toBe(true);
    expect(canMove(ref, 'SOURCED', 'SCREENED')?.reasonRequired).toBe(false);
    expect(canMove(ref, 'SOURCED', 'PLACED')).toBeUndefined();
  });

  it('withStage updates only the moved application and keeps the reason on reject', () => {
    const apps = [{ id: 'a', stage: 'SOURCED' }, { id: 'b', stage: 'SOURCED' }] as unknown as Application[];
    const out = withStage(apps, 'a', 'REJECTED', 'Skills mismatch');
    expect(out[0]!).toMatchObject({ stage: 'REJECTED', rejectionReason: 'Skills mismatch' });
    expect(out[1]!.stage).toBe('SOURCED');
    expect(withStage(out, 'a', 'SOURCED')[0]!.rejectionReason).toBeNull();
  });
});
