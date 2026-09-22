import type { Root } from 'react-dom/client';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { DocumentType } from '../src/export/documentType';
import { openExportPopup } from '../src/popup/mount';
import type { BulkExportTarget } from '../src/widget/exportTargets';
import { registerBulkExportTarget, selectedBulkExportTargets } from '../src/widget/exportTargets';
import { popupRoutes } from './exportPopupSamples';
import { installFetchMock } from './mockFetch';
import { clearToasts } from './toasts';

// A report's own "Export to PDF" button - the toolbar one and the report widget one both call
// openExportPopup({documentType: 'LIVE_REPORT'}) - asks first whether to export the report or the rows
// selected in a Bulk PDF Export widget on it. The widgets are stand-ins here: what a real one registers is
// covered by BulkExportWidget.test.tsx.

const roots: Root[] = [];
const unregister: (() => void)[] = [];

const shadow = () => (document.body.lastElementChild as HTMLElement | null)?.shadowRoot ?? null;
const chooser = () => shadow()?.querySelector('.export-target-chooser') ?? null;
const form = () => shadow()?.querySelector('#popup-style-package-select') ?? null;
const options = () => [...(shadow()?.querySelectorAll<HTMLLabelElement>('.export-target-options label') ?? [])];
const radio = (label: HTMLLabelElement) => label.querySelector<HTMLInputElement>('input[type="radio"]')!;
const continueButton = () => shadow()!.querySelector<HTMLButtonElement>('.rsp-modal-footer .sbb-btn--primary')!;
const closeButton = () => shadow()!.querySelector<HTMLButtonElement>('.rsp-modal-footer .sbb-btn--secondary')!;

/** A widget with `count` rows selected, registered for the test. */
function widget(title: string, count: number): BulkExportTarget & { startExport: ReturnType<typeof vi.fn> } {
  const target = { id: `${title}-${count}`, title, selectedCount: () => count, startExport: vi.fn() };
  unregister.push(registerBulkExportTarget(target));
  return target;
}

function open(documentType: DocumentType = 'LIVE_REPORT', exportType?: 'BULK') {
  installFetchMock(popupRoutes());
  roots.push(
    openExportPopup({
      location: {
        documentType,
        scope: 'project/elibrary/',
        projectId: 'elibrary',
        locationPath: '_default/Dashboard',
        spaceId: '_default',
        documentName: 'Dashboard',
        urlQueryParameters: {},
      },
      exportType,
      identifiers: exportType === 'BULK' ? [{ projectId: 'elibrary', documentName: 'One' }] : undefined,
    }),
  );
}

afterEach(() => {
  clearToasts();
  unregister.splice(0).forEach((remove) => remove());
  roots.splice(0).forEach((root) => root.unmount());
  document.querySelectorAll('body > div').forEach((element) => {
    if (element.shadowRoot) element.remove();
  });
  vi.unstubAllGlobals();
  document.cookie = 'selected-style-package=; path=/; max-age=0';
});

describe('the widgets a report button can offer', () => {
  it('lists the widgets with a selection, in the order they registered', () => {
    widget('Documents', 2);
    widget('Empty', 0);
    widget('Test Runs', 1);

    expect(selectedBulkExportTargets().map((target) => target.title)).toEqual(['Documents', 'Test Runs']);
  });

  it('forgets a widget once it is removed', () => {
    widget('Documents', 2);
    unregister.splice(0).forEach((remove) => remove());

    expect(selectedBulkExportTargets()).toEqual([]);
  });
});

describe('choosing what a report button exports', () => {
  it('opens the export dialog straight away where no widget has a selection', async () => {
    widget('Documents', 0);
    open();

    await vi.waitFor(() => expect(form()).not.toBeNull());
    expect(chooser()).toBeNull();
  });

  it('asks first where a widget has a selection, with the selection preselected', async () => {
    widget('Documents', 3);
    open();

    await vi.waitFor(() => expect(chooser()).not.toBeNull());
    expect(form()).toBeNull();
    expect(options().map((label) => label.textContent?.trim())).toEqual([
      'This report',
      '3 selected items from Documents',
    ]);
    expect(radio(options()[1]).checked).toBe(true);
  });

  it('offers every widget with a selection, and names one without a title', async () => {
    widget('Documents', 1);
    widget('', 2);
    open();

    await vi.waitFor(() => expect(chooser()).not.toBeNull());
    expect(options().map((label) => label.textContent?.trim())).toEqual([
      'This report',
      '1 selected item from Documents',
      '2 selected items from the Bulk PDF Export widget',
    ]);
  });

  it('opens the export dialog for the report when the report is picked', async () => {
    const documents = widget('Documents', 3);
    open();
    await vi.waitFor(() => expect(chooser()).not.toBeNull());

    radio(options()[0]).click();
    continueButton().click();

    await vi.waitFor(() => expect(form()).not.toBeNull());
    expect(chooser()).toBeNull();
    expect(documents.startExport).not.toHaveBeenCalled();
  });

  it('hands over to the picked widget and goes away', async () => {
    widget('Documents', 3);
    const testRuns = widget('Test Runs', 1);
    open();
    await vi.waitFor(() => expect(chooser()).not.toBeNull());
    const host = document.body.lastElementChild!;

    radio(options()[2]).click();
    continueButton().click();

    expect(testRuns.startExport).toHaveBeenCalledTimes(1);
    await vi.waitFor(() => expect(host.isConnected).toBe(false));
    roots.length = 0; // unmounted with its host
  });

  it('does nothing but close when closed', async () => {
    const documents = widget('Documents', 3);
    open();
    await vi.waitFor(() => expect(chooser()).not.toBeNull());
    const host = document.body.lastElementChild!;

    closeButton().click();

    await vi.waitFor(() => expect(host.isConnected).toBe(false));
    expect(documents.startExport).not.toHaveBeenCalled();
    roots.length = 0;
  });

  it('does not ask for anything but a report', async () => {
    widget('Documents', 3);
    open('LIVE_DOC');

    await vi.waitFor(() => expect(form()).not.toBeNull());
    expect(chooser()).toBeNull();
  });

  it('does not ask when the widget itself opens the dialog for its selection', async () => {
    widget('Documents', 3);
    open('LIVE_REPORT', 'BULK');

    await vi.waitFor(() => expect(form()).not.toBeNull());
    expect(chooser()).toBeNull();
  });
});
