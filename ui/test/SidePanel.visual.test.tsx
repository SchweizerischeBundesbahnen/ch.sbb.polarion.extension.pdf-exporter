import { afterEach, describe, expect, it, vi } from 'vitest';
import { page, userEvent } from 'vitest/browser';
import { mountSidePanel } from '../src/sidepanel/mount';
import {
  SAMPLE_STYLE_PACKAGE,
  SAMPLE_STYLE_PACKAGE_FULL,
  SAMPLE_STYLE_PACKAGE_HIDDEN,
  sampleDependencies,
} from './sidePanelSamples';
import type { SampleOptions } from './sidePanelSamples';

// Docker-only snapshots of the export panel as the document editor shows it, mounted the way the
// form-extension fragment mounts it: its own shadow root, carrying react-sbb-polarion's stylesheet, the
// base font rule and the panel's own CSS. Polarion's page CSS is not part of this app and is not loaded
// here, so these references show the panel's own styling - which is exactly what a change in this repo can
// move.

/** The width of Polarion's Document Properties pane, so the rows wrap where they really wrap. */
const PANE_WIDTH = 360;

const hosts: HTMLElement[] = [];

function mounted(options: SampleOptions = {}): HTMLElement {
  const host = document.createElement('div');
  host.id = `side-panel-visual-${hosts.length}`;
  host.className = 'pdf-exporter form-wrapper';
  host.style.width = `${PANE_WIDTH}px`;
  document.body.appendChild(host);
  hosts.push(host);
  mountSidePanel(`#${host.id}`, sampleDependencies(options));
  return host;
}

const settled = (host: HTMLElement, selector = '#filename') =>
  vi.waitFor(() => expect(host.shadowRoot!.querySelector(selector)).not.toBeNull());

/** Every dropdown painted and showing its selection, so a snapshot cannot catch a blank trigger. */
const dropdownsUpgraded = (host: HTMLElement) =>
  vi.waitFor(() => {
    const root = host.shadowRoot!;
    expect(root.querySelectorAll('.searchable-dropdown').length).toBe(root.querySelectorAll('select').length);
    const triggers = Array.from(root.querySelectorAll<HTMLInputElement>('input.sd-trigger'));
    expect(triggers.every((trigger) => trigger.value !== '')).toBe(true);
    const multi = Array.from(root.querySelectorAll('.sd-trigger-multi'));
    expect(multi.every((trigger) => trigger.querySelector('.sd-chip, .sd-placeholder') !== null)).toBe(true);
  });

async function snapshot(host: HTMLElement, name: string): Promise<void> {
  await dropdownsUpgraded(host);
  // Park the pointer somewhere without hover styling. Wherever it happened to rest after the previous test
  // might have some, which is enough to make a reference disagree with itself from one run to the next.
  await userEvent.hover(host.shadowRoot!.querySelector('p')!);
  await page.viewport(640, Math.ceil(host.scrollHeight) + 40);
  await expect(page.elementLocator(host)).toMatchScreenshot(name);
}

afterEach(() => {
  hosts.splice(0).forEach((host) => host.remove());
});

describe.skipIf(!__PIXEL_REFERENCES__)('side panel visual', () => {
  it('a style package that exposes its settings', async () => {
    const host = mounted({ stylePackage: SAMPLE_STYLE_PACKAGE });
    await settled(host);

    await snapshot(host, 'panel-settings-exposed');
  });

  it('every optional setting switched on, which is the panel at its tallest', async () => {
    const host = mounted({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL, data: { webhooksEnabled: true } });
    await settled(host);

    await snapshot(host, 'panel-everything-on');
  });

  it('a style package that keeps its settings to itself', async () => {
    const host = mounted({ stylePackage: SAMPLE_STYLE_PACKAGE_HIDDEN });
    await settled(host);

    await snapshot(host, 'panel-settings-hidden');
  });

  it('a user who may not export', async () => {
    const host = mounted({ data: { exportPermission: 'denied' } });
    await settled(host);

    await snapshot(host, 'panel-export-not-allowed');
  });

  it('an export in progress, with the panel out of reach', async () => {
    // The sample conversion never completes, which is the in-progress state
    const host = mounted();
    await settled(host);
    await dropdownsUpgraded(host);

    host.shadowRoot!.querySelector<HTMLButtonElement>('#export-pdf')!.click();
    await vi.waitFor(() => expect(host.shadowRoot!.querySelector('#filename')!.matches(':disabled')).toBe(true));

    await snapshot(host, 'panel-exporting');
  });

  it('a field the export was refused on', async () => {
    const host = mounted({ stylePackage: SAMPLE_STYLE_PACKAGE_FULL });
    await settled(host);
    await dropdownsUpgraded(host);

    const chapters = host.shadowRoot!.querySelector<HTMLInputElement>('#chapters')!;
    await userEvent.fill(chapters, 'one, two');
    host.shadowRoot!.querySelector<HTMLButtonElement>('#export-pdf')!.click();
    await vi.waitFor(() => expect(host.shadowRoot!.querySelector('#export-error')!.textContent).not.toBe(''));

    await snapshot(host, 'panel-invalid-field');
  });

  // An open dropdown is deliberately not snapshotted here: its popup is a portal appended to the shadow
  // root and positioned outside the host's box, which an element screenshot clips. react-sbb-polarion has
  // its own visual references for the control.
});
