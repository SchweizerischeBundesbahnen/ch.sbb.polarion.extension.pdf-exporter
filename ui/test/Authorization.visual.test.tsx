import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';

// Docker-only snapshot of the Authorization page: the two role groups with the Polarion-styled
// checkboxes, the Save / Cancel / Default / Revisions toolbar and the Quick Help text. This is the
// page a styling change in the shared component would move without any behaviour test noticing.

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

describe.skipIf(!__PIXEL_REFERENCES__)('Authorization page visual', () => {
  it('global and project roles, one of them granted', async () => {
    installFetchMock([
      {
        method: 'GET',
        match: /\/roles\?/,
        json: { globalRoles: ['admin', 'user'], projectRoles: ['project_admin', 'project_user'] },
      },
      {
        method: 'GET',
        match: /\/settings\/authorization\/names\/Default\/content/,
        json: { globalRoles: ['admin'], projectRoles: [] },
      },
    ]);
    window.history.replaceState({}, '', '?feature=authorization&embedded=true&scope=project/elibrary/');
    render(<App />);

    await vi.waitFor(() => expect(document.querySelectorAll('.roles-list input[type="checkbox"]').length).toBe(4));
    const app = document.querySelector('.app') as HTMLElement;
    await page.viewport(1280, Math.ceil(app.scrollHeight) + 40);
    await expect(page.elementLocator(app)).toMatchScreenshot('authorization-loaded');
  });
});
