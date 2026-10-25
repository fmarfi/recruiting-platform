import { describe, expect, it } from 'vitest';
import { DEFAULT_JOB_SORT, jobParamsFromState, jobStateFromParams } from './jobFilters';

const parse = (q: string) => jobStateFromParams(new URLSearchParams(q));

describe('job orders URL params', () => {
  it('defaults to my jobs, no status, default sort', () => {
    expect(parse('')).toEqual({ q: '', status: [], mine: true, sort: DEFAULT_JOB_SORT, page: 0 });
  });
  it('reads owner, status list, q, sort and page', () => {
    expect(parse('owner=all&status=open,OFFER&status=BOGUS&q=cfo&sort=daysOpen,desc&page=2')).toEqual({ q: 'cfo', status: ['OPEN', 'OFFER'], mine: false, sort: 'daysOpen,desc', page: 1 });
    expect(parse('owner=me').mine).toBe(true);
    expect(parse('sort=salary,asc').sort).toBe(DEFAULT_JOB_SORT);
  });
  it('leaves defaults out when writing', () => {
    expect(jobParamsFromState(parse('')).toString()).toBe('');
    expect(jobParamsFromState(parse('owner=all&status=OPEN&page=3')).toString()).toBe('owner=all&status=OPEN&page=3');
  });
});
