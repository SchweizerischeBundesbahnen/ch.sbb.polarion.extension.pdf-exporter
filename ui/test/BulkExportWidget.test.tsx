import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import BulkExportWidget from '../src/widget/BulkExportWidget';
import type { WidgetDependencies } from '../src/widget/BulkExportWidget';
import { SAMPLE_ITEMS, SAMPLE_ITEMS_EMPTY, SAMPLE_ITEMS_WITH_UNREADABLE, SAMPLE_SHIM } from '../src/widget/sampleData';
import type { BulkExportItems } from '../src/widget/types';
import { popupDependencies } from './exportPopupSamples';
import { installFetchMock } from './mockFetch';

// The widget as a report page shows it. It is rendered without its shadow root here: what this suite
// is about is the behavior - what reaches the endpoint, what the table does with the answer, and what
// the export button hands to the export dialog. The shadow root and its styles are covered by
// widgetMount.test.tsx and the visual suite.

const ITEMS_ROUTE = /\/widgets\/bulk-export\/items$/;

const rows = () => Array.from(document.querySelectorAll('.polarion-rpw-table-content-row'));
const checkboxes = () => Array.from(document.querySelectorAll<HTMLInputElement>('input.export-item'));
const selectAll = () => document.querySelector<HTMLInputElement>('#export-all')!;
const exportButton = () => document.querySelector<HTMLElement>('#bulk-export-pdf')!;
const isDisabled = () => exportButton().classList.contains('polarion-TestsExecutionButton-buttons-defaultCursor');

const open = (items = SAMPLE_ITEMS, deps: WidgetDependencies = {}) => {
  installFetchMock([{ method: 'POST', match: ITEMS_ROUTE, json: items }]);
  return render(<BulkExportWidget shim={SAMPLE_SHIM} deps={deps} />);
};

/** The export dialog, which the button opens for the selection. Its own behavior: ExportPopup.test.tsx. */
const dialog = () => document.querySelector('.pdf-export-form');

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe('Bulk PDF Export widget', () => {
  it('asks the endpoint for its rows, passing the descriptor of the shim back unchanged', async () => {
    const fetchMock = installFetchMock([{ method: 'POST', match: ITEMS_ROUTE, json: SAMPLE_ITEMS }]);
    render(<BulkExportWidget shim={SAMPLE_SHIM} hostSelector="#host" />);

    await vi.waitFor(() => expect(rows().length).toBe(4));
    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toBe('/polarion/pdf-exporter/rest/internal/widgets/bulk-export/items');
    expect(JSON.parse(String(init?.body))).toEqual({
      descriptor: SAMPLE_SHIM.descriptor,
      signature: SAMPLE_SHIM.signature,
    });
  });

  it('renders the widget frame before the rows arrive', async () => {
    let deliver!: (items: BulkExportItems) => void;
    const pending = new Promise<BulkExportItems>((resolve) => (deliver = resolve));
    render(<BulkExportWidget shim={SAMPLE_SHIM} hostSelector="#host" deps={{ loadItems: () => pending }} />);

    // The title and the button come from the shim, so they are there while the request is in flight
    await vi.waitFor(() => expect(document.querySelector('h3')?.textContent).toBe('Test Runs'));
    expect(exportButton()).not.toBeNull();
    expect(document.querySelector('.widget-loading')).not.toBeNull();

    deliver(SAMPLE_ITEMS);
    await vi.waitFor(() => expect(document.querySelector('.widget-loading')).toBeNull());
    expect(rows().length).toBe(4);
  });

  it('renders a column per configured field and the cells as Polarion rendered them', async () => {
    open();

    await vi.waitFor(() => expect(rows().length).toBe(4));
    expect(
      Array.from(document.querySelectorAll('.polarion-rpw-table-header-row th')).map((th) => th.textContent),
    ).toEqual(['', 'ID', 'Status', 'Template', 'Author', 'Created']);
    // The cell HTML is used as it arrived: the link Polarion rendered is a link, not escaped text
    expect(rows()[0].querySelector('td:nth-child(2) a')?.textContent).toBe('build_quick-20170211-141155');
  });

  it('shows an item the user may not read as a message across the table', async () => {
    open(SAMPLE_ITEMS_WITH_UNREADABLE);

    await vi.waitFor(() => expect(rows().length).toBe(3));
    const cell = rows()[2].querySelector('.polarion-rpw-table-not-readable-cell')!;
    expect(cell.textContent).toBe('You do not have permission to read this item');
    expect(cell.getAttribute('colspan')).toBe('6');
    // ... and it cannot be exported
    expect(checkboxes().length).toBe(2);
  });

  it('keeps the export button disabled until something is selected', async () => {
    open();
    await vi.waitFor(() => expect(rows().length).toBe(4));

    expect(isDisabled()).toBe(true);
    expect(document.querySelector('.polarion-TestsExecutionButton-link')?.getAttribute('title')).toContain(
      'select at least one item',
    );

    await userEvent.click(checkboxes()[0]);
    expect(isDisabled()).toBe(false);

    await userEvent.click(checkboxes()[0]);
    expect(isDisabled()).toBe(true);
  });

  it('selects and clears every row from the header checkbox', async () => {
    open();
    await vi.waitFor(() => expect(rows().length).toBe(4));

    await userEvent.click(selectAll());
    expect(checkboxes().every((checkbox) => checkbox.checked)).toBe(true);

    await userEvent.click(selectAll());
    expect(checkboxes().some((checkbox) => checkbox.checked)).toBe(false);
  });

  it('shows the header checkbox as indeterminate while only some rows are selected', async () => {
    open();
    await vi.waitFor(() => expect(rows().length).toBe(4));

    await userEvent.click(checkboxes()[0]);
    expect(selectAll().indeterminate).toBe(true);
    expect(selectAll().checked).toBe(false);

    await userEvent.click(checkboxes()[1]);
    await userEvent.click(checkboxes()[2]);
    await userEvent.click(checkboxes()[3]);
    expect(selectAll().indeterminate).toBe(false);
    expect(selectAll().checked).toBe(true);
  });

  it('opens the export dialog for the selected items', async () => {
    const identifiers: unknown[] = [];
    open(SAMPLE_ITEMS, {
      popup: {
        ...popupDependencies(),
        loadData: (_send, request) => {
          identifiers.push(request.identifiers);
          expect(request.documentType).toBe('TEST_RUN');
          expect(request.exportType).toBe('BULK');
          return Promise.reject(new Error('enough'));
        },
      },
    });
    await vi.waitFor(() => expect(rows().length).toBe(4));

    await userEvent.click(checkboxes()[1]);
    await userEvent.click(exportButton());

    await vi.waitFor(() => expect(dialog()).not.toBeNull());
    expect(identifiers).toEqual([[{ projectId: 'elibrary', documentName: '0_9b RT' }]]);
  });

  it('does not open the export dialog with nothing selected', async () => {
    open(SAMPLE_ITEMS, { popup: popupDependencies() });
    await vi.waitFor(() => expect(rows().length).toBe(4));

    // Clicked directly: the button reports itself as disabled, which Playwright refuses to click
    exportButton().click();

    expect(dialog()).toBeNull();
  });

  it('reports how many items were found and links to the table view', async () => {
    open();

    await vi.waitFor(() => expect(rows().length).toBe(4));
    expect(document.querySelector('.polarion-rpw-table-counts')?.textContent).toBe('4 items found');
    expect(document.querySelector('.polarion-rpw-table-open-in-table a')?.getAttribute('href')).toBe(
      '/polarion/#/project/elibrary/testruns',
    );
  });

  it('shows the query behind the info icon on demand', async () => {
    open();
    await vi.waitFor(() => expect(rows().length).toBe(4));

    expect(document.querySelector('.polarion-rpw-table-query')).toBeNull();
    await userEvent.click(document.querySelector<HTMLElement>('.polarion-rpw-table-show-query img')!);
    expect(document.querySelector('.polarion-rpw-table-query')?.textContent).toBe(
      'type:testrun AND project.id:elibrary',
    );

    await userEvent.click(document.querySelector<HTMLElement>('.polarion-rpw-table-show-query img')!);
    expect(document.querySelector('.polarion-rpw-table-query')).toBeNull();
  });

  it('renders an empty data set as an empty table, not as an error', async () => {
    open(SAMPLE_ITEMS_EMPTY);

    await vi.waitFor(() =>
      expect(document.querySelector('.polarion-rpw-table-counts')?.textContent).toBe('0 items found'),
    );
    expect(rows().length).toBe(0);
    expect(document.querySelector('.widget-error')).toBeNull();
    expect(isDisabled()).toBe(true);
  });

  it('shows what the endpoint says when it refuses the descriptor', async () => {
    installFetchMock([
      {
        method: 'POST',
        match: ITEMS_ROUTE,
        status: 400,
        json: { message: 'The widget descriptor is missing or was not signed by this server. Reload the page.' },
      },
    ]);
    render(<BulkExportWidget shim={SAMPLE_SHIM} hostSelector="#host" />);

    await vi.waitFor(() => expect(document.querySelector('.widget-error')).not.toBeNull());
    expect(document.querySelector('.widget-error')?.textContent).toContain('Reload the page');
    expect(document.querySelector('.export-items')).toBeNull();
  });
});
