import { act, fireEvent, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderShell } from '../../shell/edit/testUtils';

const POST = vi.fn();
const PATCH = vi.fn();
const GET = vi.fn();
vi.mock('../../api', () => ({ api: { POST: (...a: unknown[]) => POST(...a), PATCH: (...a: unknown[]) => PATCH(...a), GET: (...a: unknown[]) => GET(...a) }, API_BASE: '/api/v1' }));

import { AgreementDrawer } from './AgreementDrawer';

const preset = { account: { id: 'a1', name: 'Acme' }, fixedJob: { id: 'j1', title: 'CFO' } };
const agreement = {
  id: 'g1', account: { id: 'a1', name: 'Acme' }, jobOrder: { id: 'j1', title: 'CFO' }, kind: 'ENGAGEMENT_LETTER', status: 'DRAFT', title: 'Letter', feeModel: 'PERCENT_FIRST_YEAR_CASH',
  feePercent: '25', feeBasis: ['BASE'], currency: 'TRY', paymentTermsDays: 30, guaranteeMonths: 6, scheduleItems: [], version: 4, createdAt: '', updatedAt: '', billingAccount: { id: 'a1', name: 'Acme' },
} as never;

describe('AgreementDrawer', () => {
  beforeEach(() => {
    Object.defineProperty(document, 'fonts', { value: { addEventListener() {}, removeEventListener() {} }, configurable: true });
    for (const m of [POST, PATCH, GET]) m.mockReset();
    GET.mockResolvedValue({ data: [] });
  });

  it('shows only the fields of the chosen fee model', async () => {
    renderShell(<AgreementDrawer opened onClose={vi.fn()} onSaved={vi.fn()} preset={preset} />);
    expect(screen.getByLabelText(/Fee percent/)).toBeInTheDocument();
    await act(async () => {
      fireEvent.change(screen.getByLabelText('Fee model'), { target: { value: 'FLAT_FEE' } });
    });
    expect(screen.queryByLabelText(/Fee percent/)).toBeNull();
    expect(screen.getByLabelText(/Flat fee/)).toBeInTheDocument();
  });

  it('creates an agreement preset to the position and client', async () => {
    POST.mockResolvedValue({ data: { id: 'g9' } });
    const onSaved = vi.fn();
    renderShell(<AgreementDrawer opened onClose={vi.fn()} onSaved={onSaved} preset={preset} />);
    fireEvent.change(screen.getByLabelText(/Fee percent/), { target: { value: '25' } });
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Create agreement' }));
    });
    await waitFor(() => expect(POST).toHaveBeenCalled());
    const body = POST.mock.calls[0]![1].body;
    expect(body).toMatchObject({ accountId: 'a1', jobOrderId: 'j1', kind: 'ENGAGEMENT_LETTER', feeModel: 'PERCENT_FIRST_YEAR_CASH', feePercent: '25', feeBasis: ['BASE'] });
    expect(body.flatFeeAmount).toBeUndefined();
    expect(onSaved).toHaveBeenCalled();
  });

  it('sends version on update and offers compare on 409', async () => {
    PATCH.mockResolvedValue({ error: { code: 'VERSION_CONFLICT' }, response: { status: 409 } });
    GET.mockImplementation((url: string) => Promise.resolve(url === '/agreements/{id}' ? { data: { ...(agreement as object), title: 'Theirs', version: 5 } } : { data: [] }));
    renderShell(<AgreementDrawer opened onClose={vi.fn()} onSaved={vi.fn()} agreement={agreement} preset={preset} />);
    fireEvent.change(screen.getByLabelText('Title'), { target: { value: 'Mine' } });
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /Save/ }));
    });
    await waitFor(() => expect(PATCH).toHaveBeenCalled());
    expect(PATCH.mock.calls[0]![1].body).toEqual({ title: 'Mine', version: 4 });
    expect(await screen.findByText('Theirs')).toBeInTheDocument();
  });
});
