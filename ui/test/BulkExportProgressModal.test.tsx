import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import BulkExportProgressModal from '../src/widget/BulkExportProgressModal';
import { SAMPLE_ITEMS } from '../src/widget/sampleData';
import type { BulkExportState, ExportRowState } from '../src/widget/useBulkExport';

// The progress dialog of a running bulk export. It renders into the page body, not into the widget's
// shadow root, because it wears the same micromodal styling as the export parameters dialog that opens
// it - and that stylesheet is put on the page by the widget renderer. This suite is behavior only for
// the same reason: the stylesheet is not part of this app, so a screenshot here would show the markup
// unstyled and prove nothing.

const rowsWith = (...states: ExportRowState[]) =>
  states.map((state, index) => ({ item: SAMPLE_ITEMS.items[index], state }));

const state = (over: Partial<BulkExportState> = {}): BulkExportState => ({
  status: 'in-progress',
  rows: rowsWith('finished', 'in-progress', 'paused'),
  processed: 1,
  errors: false,
  ...over,
});

const modal = () => document.querySelector('#bulk-pdf-export-modal-popup');

/** Renders the dialog and waits for it: a React root paints after the call that asks it to. */
async function open(state: BulkExportState) {
  render(<BulkExportProgressModal state={state} onStop={onStop} onClose={onClose} />);
  await vi.waitFor(() => expect(modal()).not.toBeNull());
}

const onStop = vi.fn();
const onClose = vi.fn();
const items = () => Array.from(document.querySelectorAll('.modal__content .export-item'));
const footerButton = () => document.querySelector<HTMLElement>('.modal__footer button')!;

afterEach(() => {
  cleanup();
  onStop.mockClear();
  onClose.mockClear();
});

describe('Bulk export progress dialog', () => {
  it('stays away until a run starts', async () => {
    render(<BulkExportProgressModal state={state({ status: 'closed', rows: [] })} onStop={onStop} onClose={onClose} />);

    await vi.waitFor(() => expect(document.querySelector('.modal__overlay')).toBeNull());
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

    expect(footerButton().textContent).toBe('Stop');
    await userEvent.click(footerButton());
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

    expect(footerButton().textContent).toBe('Close');
    await userEvent.click(footerButton());
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
