import { act, fireEvent, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderShell } from '../../shell/edit/testUtils';

const POST = vi.fn();
vi.mock('../../api', () => ({ api: { POST: (...a: unknown[]) => POST(...a), GET: vi.fn() } }));
vi.mock('../../shell/refdata/useReference', () => ({ useReference: () => ({ data: { sectors: ['FMCG'] } }) }));

import { NewAccountDialog } from './NewAccountDialog';

describe('NewAccountDialog', () => {
  beforeEach(() => {
    // jsdom has no document.fonts, which Mantine's autosize textarea listens to.
    Object.defineProperty(document, 'fonts', { value: { addEventListener() {}, removeEventListener() {} }, configurable: true });
    POST.mockReset();
  });

  it('shows name, sector and city first and keeps the extra details behind a closed disclosure', () => {
    renderShell(<NewAccountDialog opened onClose={vi.fn()} onCreated={vi.fn()} />);
    expect(screen.getByLabelText(/^Name/)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Sector/)).toBeInTheDocument();
    expect(screen.getByLabelText(/^City/)).toBeInTheDocument();
    const toggle = screen.getByRole('button', { name: 'More details' });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    fireEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByLabelText(/^Website/)).toBeInTheDocument();
  });

  it('blocks submit with field errors when name or sector is empty', async () => {
    renderShell(<NewAccountDialog opened onClose={vi.fn()} onCreated={vi.fn()} />);
    fireEvent.keyDown(screen.getByLabelText(/^Sector/), { key: 'Enter', ctrlKey: true });
    await act(async () => {});
    expect(POST).not.toHaveBeenCalled();
    expect(screen.getByText('Enter the client name.')).toBeInTheDocument();
    expect(screen.getByText('Pick or type a sector.')).toBeInTheDocument();
  });

  it('creates the client on Ctrl+Enter with the trimmed values and empty optional fields as null', async () => {
    POST.mockResolvedValueOnce({ data: { id: 'a9' } });
    const onCreated = vi.fn();
    const onClose = vi.fn();
    renderShell(<NewAccountDialog opened onClose={onClose} onCreated={onCreated} />);
    fireEvent.change(screen.getByLabelText(/^Name/), { target: { value: '  Acme Foods  ' } });
    fireEvent.change(screen.getByLabelText(/^Sector/), { target: { value: 'FMCG' } });
    fireEvent.keyDown(screen.getByLabelText(/^Sector/), { key: 'Enter', ctrlKey: true });
    await act(async () => {});
    expect(POST).toHaveBeenCalledWith('/accounts', { body: { name: 'Acme Foods', sector: 'FMCG', city: null, website: null, notes: null } });
    expect(onClose).toHaveBeenCalled();
    expect(onCreated).toHaveBeenCalledWith('a9');
  });
});
