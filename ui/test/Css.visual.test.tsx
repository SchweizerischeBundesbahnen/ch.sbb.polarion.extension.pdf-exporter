import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup } from 'vitest-browser-react';
import { filled, snapshotFeature } from './visualHelpers';

// Docker-only snapshot of the CSS settings page: the configuration selector, the opt-out checkbox,
// the tab bar and the editor with its syntax highlighting, and the Save / Cancel / Revisions toolbar.

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'selected-configuration-css=; path=/; max-age=0';
});

// The editor is 60vh tall, and snapshotFeature grows the viewport to the page height. The editor then
// grows with it and overflows the new viewport, so a scrollbar narrows the capture. Pin its height first.
async function pinEditorHeight(): Promise<void> {
  const editor = document.querySelector('.css-editor') as HTMLElement;
  editor.style.height = `${editor.getBoundingClientRect().height}px`;
}

describe.skipIf(!__PIXEL_REFERENCES__)('CSS page visual', () => {
  it('a configuration loaded, custom stylesheet on screen', async () => {
    await snapshotFeature(
      'css',
      [
        { method: 'GET', match: /\/settings\/css\/names\?/, json: [{ name: 'Default', scope: 'project/elibrary/' }] },
        {
          method: 'GET',
          match: /\/settings\/css\/names\/[^/]+\/content/,
          json: {
            css: '/* narrower tables in the exported PDF */\ntable.polarion-rte-table {\n  width: 100%;\n  font-size: 9pt;\n}',
            disableDefaultCss: false,
          },
        },
        { method: 'GET', match: /\/settings\/css\/default-content/, json: { css: 'body { margin: 0; }' } },
        { method: 'GET', match: /\/settings\/css\/names\/[^/]+\/revisions/, json: [] },
      ],
      filled('#custom-css-input'),
      'css-loaded',
      undefined,
      pinEditorHeight,
    );
    expect(true).toBe(true);
  });
});
