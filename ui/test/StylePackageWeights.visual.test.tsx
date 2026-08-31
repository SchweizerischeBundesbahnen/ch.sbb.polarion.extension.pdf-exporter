import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';
import { settleBeforeCapture, settleLayout } from './visualHelpers';

// Docker-only snapshot of the Style Package Weights page: the ordered list with its drag handles and
// caret buttons, a read-only entry inherited from the global scope, and the Save / Cancel toolbar.
// The list is a shared react-sbb-polarion component, so this reference is what would catch a change
// in the library moving this page's look.

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

describe.skipIf(!__PIXEL_REFERENCES__)('Style Package Weights page visual', () => {
  it('own packages ordered, one inherited from the global scope', async () => {
    installFetchMock([
      {
        method: 'GET',
        match: /\/settings\/style-package\/weights\?/,
        json: [
          { name: 'Wide layout', scope: 'project/elibrary/', weight: 30 },
          { name: 'Compact layout', scope: 'project/elibrary/', weight: 20 },
          { name: 'Corporate default', scope: '', weight: 10 },
        ],
      },
    ]);
    window.history.replaceState({}, '', '?feature=style-package-weights&embedded=true&scope=project/elibrary/');
    render(<App />);

    await vi.waitFor(() => expect(document.querySelectorAll('.weights-list li').length).toBe(3));
    const app = document.querySelector('.app') as HTMLElement;
    await settleLayout();
    await page.viewport(1280, Math.ceil(app.scrollHeight) + 40);
    await settleBeforeCapture();
    await expect(page.elementLocator(app)).toMatchScreenshot('weights-loaded');
  });
});
