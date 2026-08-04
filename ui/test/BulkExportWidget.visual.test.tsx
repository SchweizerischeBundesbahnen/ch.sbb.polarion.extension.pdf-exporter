import { afterEach, describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { mountInto, readShim } from '../src/widget/main';
import {
  SAMPLE_ITEMS,
  SAMPLE_ITEMS_EMPTY,
  SAMPLE_ITEMS_TRUNCATED,
  SAMPLE_ITEMS_WITH_UNREADABLE,
  SAMPLE_SHIM,
} from '../src/widget/sampleData';
import type { BulkExportItems } from '../src/widget/types';

// Docker-only snapshots of the widget as a report page shows it, mounted the way the renderer mounts
// it: its own shadow root, carrying the tokens and the widget's stylesheet. Polarion's page CSS is not
// part of this app and is not loaded here, so these references show the widget's own styling - which is
// exactly what a change in this repo can move.

const hosts: HTMLElement[] = [];

function mounted(items: BulkExportItems): HTMLElement {
  const host = document.createElement('div');
  host.id = `widget-visual-${hosts.length}`;
  host.className = 'polarion-PdfExporter-BulkExportWidget sbb-ui';
  host.setAttribute('data-title', 'Test Runs');
  host.setAttribute('data-document-type', 'TEST_RUN');
  host.setAttribute('data-descriptor', SAMPLE_SHIM.descriptor);
  host.setAttribute('data-signature', SAMPLE_SHIM.signature);
  document.body.appendChild(host);
  hosts.push(host);
  mountInto(host, readShim(host), `#${host.id}`, { loadItems: () => Promise.resolve(items) });
  return host;
}

async function snapshot(host: HTMLElement, name: string): Promise<void> {
  // Park the pointer on the heading first. It carries no hover styling, while wherever the pointer
  // happened to rest after the previous test might - a link picking up its underline is enough to make
  // a reference disagree with itself from one run to the next.
  const heading = host.shadowRoot!.querySelector('h3');
  if (heading) {
    await userEvent.hover(heading);
  }
  await page.viewport(1280, Math.ceil(host.scrollHeight) + 40);
  await expect(page.elementLocator(host)).toMatchScreenshot(name);
}

const settled = (host: HTMLElement, selector = '.polarion-rpw-table-counts') =>
  vi.waitFor(() => expect(host.shadowRoot!.querySelector(selector)).not.toBeNull());

afterEach(() => {
  hosts.splice(0).forEach((host) => host.remove());
});

describe.skipIf(!__PIXEL_REFERENCES__)('Bulk PDF Export widget visual', () => {
  it('the table of a loaded widget, nothing selected', async () => {
    const host = mounted(SAMPLE_ITEMS);
    await settled(host);

    await snapshot(host, 'widget-loaded');
  });

  it('a selection, which enables the export button', async () => {
    const host = mounted(SAMPLE_ITEMS);
    await settled(host);

    host.shadowRoot!.querySelectorAll<HTMLInputElement>('input.export-item')[1].click();
    await vi.waitFor(() =>
      expect(host.shadowRoot!.querySelector('#bulk-export-pdf')!.className).not.toContain('defaultCursor'),
    );

    await snapshot(host, 'widget-selection');
  });

  it('a row the user may not read', async () => {
    const host = mounted(SAMPLE_ITEMS_WITH_UNREADABLE);
    await settled(host);

    await snapshot(host, 'widget-unreadable-row');
  });

  it('more items found than the widget shows', async () => {
    const host = mounted(SAMPLE_ITEMS_TRUNCATED);
    await settled(host);

    await snapshot(host, 'widget-truncated');
  });

  it('the query behind the info icon', async () => {
    const host = mounted(SAMPLE_ITEMS);
    await settled(host);

    host.shadowRoot!.querySelector<HTMLElement>('.polarion-rpw-table-show-query img')!.click();
    await vi.waitFor(() => expect(host.shadowRoot!.querySelector('.polarion-rpw-table-query')).not.toBeNull());

    await snapshot(host, 'widget-query-shown');
  });

  it('a data set that found nothing', async () => {
    const host = mounted(SAMPLE_ITEMS_EMPTY);
    await settled(host);

    await snapshot(host, 'widget-empty');
  });

  it('an endpoint that refused the descriptor', async () => {
    const host = document.createElement('div');
    host.id = 'widget-visual-error';
    host.className = 'polarion-PdfExporter-BulkExportWidget sbb-ui';
    host.setAttribute('data-title', 'Test Runs');
    document.body.appendChild(host);
    hosts.push(host);
    mountInto(host, readShim(host), '#widget-visual-error', {
      loadItems: () =>
        Promise.reject(
          new Error('The widget descriptor is missing or was not signed by this server. Reload the page.'),
        ),
    });

    await settled(host, '.widget-error');

    await snapshot(host, 'widget-error');
  });
});
