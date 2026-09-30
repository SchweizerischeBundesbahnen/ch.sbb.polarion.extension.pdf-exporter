import type { Root } from 'react-dom/client';
import { a11yViolations } from '@sbb-polarion/react-sbb-polarion/testing';
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
/** The elements the stand-in widgets stand on, removed after each test. */
const anchors: HTMLElement[] = [];

const shadow = () => (document.body.lastElementChild as HTMLElement | null)?.shadowRoot ?? null;
const chooser = () => shadow()?.querySelector('.export-target-chooser') ?? null;
const form = () => shadow()?.querySelector('#popup-style-package-select') ?? null;
const options = () => [...(shadow()?.querySelectorAll<HTMLLabelElement>('.export-target-options label') ?? [])];
const radio = (label: HTMLLabelElement) => label.querySelector<HTMLInputElement>('input[type="radio"]')!;
const continueButton = () => shadow()!.querySelector<HTMLButtonElement>('.rsp-modal-footer .sbb-btn--primary')!;
const closeButton = () => shadow()!.querySelector<HTMLButtonElement>('.rsp-modal-footer .sbb-btn--secondary')!;

type StandIn = BulkExportTarget & { startExport: ReturnType<typeof vi.fn>; element: HTMLElement };

/**
 * A widget with `count` rows selected, registered for the test. Its element is appended to the page, or put
 * in front of `before` where given.
 */
function widget(title: string, count: number, before?: HTMLElement): StandIn {
  const element = document.createElement('div');
  if (before) {
    before.before(element);
  } else {
    document.body.appendChild(element);
  }
  anchors.push(element);
  const target = {
    id: `${title}-${count}-${anchors.length}`,
    title,
    element,
    anchor: () => element,
    selectedCount: () => count,
    startExport: vi.fn(),
  };
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
  anchors.splice(0).forEach((element) => element.remove());
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

  it('lists them in the order the page shows them, whatever order they registered in', () => {
    const later = widget('Documents', 2);
    widget('Test Runs', 1, later.element);

    expect(selectedBulkExportTargets().map((target) => target.title)).toEqual(['Test Runs', 'Documents']);
  });

  it('drops a widget whose element has left the page', () => {
    // What happens in Polarion when the user moves on to another report: the page is not reloaded and the
    // widget's root is never unmounted, but its host goes
    const gone = widget('Documents', 2);
    widget('Test Runs', 1);
    gone.element.remove();

    expect(selectedBulkExportTargets().map((target) => target.title)).toEqual(['Test Runs']);
    // For good, not only for this read: its element coming back does not bring it back
    document.body.appendChild(gone.element);
    expect(selectedBulkExportTargets().map((target) => target.title)).toEqual(['Test Runs']);
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

  it('tells widgets of the same title apart by their order on the page', async () => {
    const second = widget('Documents', 2);
    widget('Documents', 2, second.element);
    widget('Test Runs', 1);
    open();

    await vi.waitFor(() => expect(chooser()).not.toBeNull());
    expect(options().map((label) => label.textContent?.trim())).toEqual([
      'This report',
      '2 selected items from Documents (widget 1 of 2)',
      '2 selected items from Documents (widget 2 of 2)',
      '1 selected item from Test Runs',
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

  it('asks on a test run page too, which a report button opens as a report', async () => {
    // A test run page carries the report toolbar and report widgets, but its location resolves to TEST_RUN
    widget('Documents', 3);
    installFetchMock(popupRoutes());
    roots.push(
      openExportPopup({
        documentType: 'LIVE_REPORT',
        location: {
          documentType: 'TEST_RUN',
          scope: 'project/elibrary/',
          projectId: 'elibrary',
          urlQueryParameters: { id: 'run_1' },
        },
      }),
    );

    await vi.waitFor(() => expect(chooser()).not.toBeNull());
    expect(options().map((label) => label.textContent?.trim())).toEqual([
      'This test run',
      '3 selected items from Documents',
    ]);
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

describe('accessibility', () => {
  // The dialog is mounted in a shadow root of a host appended to the body, so it is scanned through that host.
  const host = () => document.body.lastElementChild as HTMLElement;

  it('has no WCAG A/AA violations while asking what a report button exports', async () => {
    widget('Documents', 3);
    widget('Test Runs', 1);
    open();
    await vi.waitFor(() => expect(chooser()).not.toBeNull());
    expect(await a11yViolations(host())).toEqual([]);
  });
});
