import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import BulkExportProgressModal from '../src/widget/BulkExportProgressModal';
import { SAMPLE_ITEMS } from '../src/widget/sampleData';
import type { BulkExportState, ExportRowState } from '../src/widget/useBulkExport';

// The progress dialog of a running bulk export: RSP's shared Modal, rendered inside the widget's own shadow
// root in production. It is rendered directly here, so the assertions read the document rather than a shadow
// root; how it looks in that root is covered by BulkExportWidget.visual.test.tsx.
//
// The dialog offers exactly one action at a time, Stop or Close, while the shared Modal always renders both
// of its footer buttons - widget.css hides the one that does not apply. That hiding is CSS the suite does not
// carry, so which button is *offered* is asserted through the `running` / `done` class the CSS keys off.

const rowsWith = (...states: ExportRowState[]) =>
  states.map((state, index) => ({ item: SAMPLE_ITEMS.items[index], state }));

const state = (over: Partial<BulkExportState> = {}): BulkExportState => ({
  status: 'in-progress',
  rows: rowsWith('finished', 'in-progress', 'paused'),
  processed: 1,
  errors: false,
  merge: false,
  ...over,
});

const modal = () => document.querySelector('#bulk-pdf-export-popup');

/** Renders the dialog and waits for it: a React root paints after the call that asks it to. */
async function open(state: BulkExportState) {
  render(<BulkExportProgressModal state={state} onStop={onStop} onClose={onClose} />);
  await vi.waitFor(() => expect(modal()).not.toBeNull());
}

const onStop = vi.fn();
const onClose = vi.fn();
const items = () => Array.from(document.querySelectorAll('.bulk-export-progress .export-item'));
const stopButton = () => document.querySelector<HTMLElement>('.rsp-modal-footer .sbb-btn--primary')!;
const closeButton = () => document.querySelector<HTMLElement>('.rsp-modal-footer .sbb-btn--secondary')!;
/** Which of the two footer buttons the CSS leaves visible. */
const offers = () => (modal()!.classList.contains('running') ? 'Stop' : 'Close');

afterEach(() => {
  cleanup();
  onStop.mockClear();
  onClose.mockClear();
});

describe('Bulk export progress dialog', () => {
  it('stays away until a run starts', async () => {
    render(<BulkExportProgressModal state={state({ status: 'closed', rows: [] })} onStop={onStop} onClose={onClose} />);

    await vi.waitFor(() => expect(document.querySelector('.rsp-modal')).toBeNull());
    expect(modal()).toBeNull();
  });

  it('lists every item of the run with its state', async () => {
    await open(state());

    expect(items().map((item) => item.className)).toEqual([
      'export-item finished',
      'export-item in-progress',
      'export-item paused',
    ]);
    // Type and name, as the vanilla dialog wrote them
    expect(items()[0].querySelector('.type')?.textContent).toBe('Test Run: ');
    expect(items()[0].querySelector('.name')?.textContent).toBe('build_quick-20170211-141155');
  });

  it('shows the progress while the run is going and offers to stop it', async () => {
    await open(state());

    const bar = document.querySelector<HTMLElement>('.progress-bar span')!;
    expect(bar.style.width).toBe('33%');
    expect(bar.textContent).toBe('1 out of 3 finished');
    expect(document.querySelector('.result')).toBeNull();

    expect(offers()).toBe('Stop');
    expect(stopButton().textContent).toBe('Stop');
    await userEvent.click(stopButton());
    expect(onStop).toHaveBeenCalled();
  });

  it('keeps the progress bar away for a single item', async () => {
    await open(state({ rows: rowsWith('in-progress'), processed: 0 }));

    expect(document.querySelector('.progress-bar')).toBeNull();
  });

  it('leaves the count out of a bar too narrow to hold it', async () => {
    // One of four done is 25%, which is where the text stops fitting
    await open(state({ rows: rowsWith('finished', 'in-progress', 'paused', 'paused'), processed: 1 }));

    expect(document.querySelector('.progress-bar span')?.textContent).toBe('');
  });

  it('reports a finished run and offers to close', async () => {
    await open(state({ status: 'finished', rows: rowsWith('finished', 'finished', 'finished'), processed: 3 }));

    expect(document.querySelector('.result')?.textContent).toBe('Export successfully finished');
    expect(document.querySelector('.result')?.className).toBe('result finished');
    expect(document.querySelector('.progress-bar')).toBeNull();

    expect(offers()).toBe('Close');
    expect(closeButton().textContent).toBe('Close');
    await userEvent.click(closeButton());
    expect(onClose).toHaveBeenCalled();
  });

  it('marks a finished run that had failures, and says why each one failed', async () => {
    await open(
      state({
        status: 'finished',
        rows: [
          { item: SAMPLE_ITEMS.items[0], state: 'finished' },
          { item: SAMPLE_ITEMS.items[1], state: 'error', error: 'Document has no content' },
        ],
        processed: 2,
        errors: true,
      }),
    );

    expect(document.querySelector('.result')?.textContent).toBe('Export finished with errors');
    expect(document.querySelector('.result')?.className).toBe('result finished with-errors');
    expect(document.querySelector('.export-item.error .error-message')?.textContent).toBe('Document has no content');
  });

  it('reports a run the user stopped', async () => {
    await open(state({ status: 'interrupted', rows: rowsWith('finished', 'interrupted', 'interrupted') }));

    expect(document.querySelector('.result')?.textContent).toBe('Export interrupted by user');
    expect(document.querySelector('.result')?.className).toBe('result interrupted');
    expect(items().filter((item) => item.classList.contains('interrupted')).length).toBe(2);
  });
});
