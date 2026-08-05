import { afterEach, describe, expect, it, vi } from 'vitest';
import { ensureMicroModal } from '../src/services/productModules';

// The product's export dialog needs the `MicroModal` global and does not load it itself: every entry
// point that opens the dialog provides it (live-reports.js and starter.js inject the script, the
// widget's vanilla predecessor imported it on its first line). Missing it, the dialog throws
// "MicroModal is not defined" and nothing opens.
//
// Only the branch that finds it already there can be exercised here - the other one fetches the script
// from the extension's webapp, which exists in Polarion and nowhere else.

declare global {
  interface Window {
    MicroModal?: unknown;
  }
}

afterEach(() => {
  delete window.MicroModal;
  vi.unstubAllGlobals();
});

describe('the product export dialog dependencies', () => {
  it('leaves a micromodal that the page already carries alone', async () => {
    const alreadyThere = { show: vi.fn() };
    window.MicroModal = alreadyThere;

    await expect(ensureMicroModal()).resolves.toBeUndefined();

    // Not reloaded, and above all not replaced: the toolbar on the same page holds on to this instance
    expect(window.MicroModal).toBe(alreadyThere);
  });
});
