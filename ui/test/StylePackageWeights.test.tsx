import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';

// The Style Package Weights page as the administration menu opens it. The list itself - reordering,
// the weight arithmetic, the read-only entries inherited from the global scope - belongs to the shared
// react-sbb-polarion component and is covered there; what matters here is the wiring: that this app
// asks *this* extension's endpoint and posts back what the page produced.

const origUrl = window.location.pathname + window.location.search;

const WEIGHTS = [
  { name: 'Wide', scope: 'project/elibrary/', weight: 30 },
  { name: 'Compact', scope: 'project/elibrary/', weight: 20 },
  { name: 'Corporate', scope: '', weight: 10 },
];

const routes = (weights = WEIGHTS) => [
  { method: 'GET', match: /\/settings\/style-package\/weights\?/, json: weights },
  { method: 'POST', match: /\/settings\/style-package\/weights$/, json: {} },
];

const open = (search = '?feature=style-package-weights&embedded=true&scope=project/elibrary/') => {
  window.history.replaceState({}, '', search);
  render(<App />);
};

const rows = () => Array.from(document.querySelectorAll('.weights-list li'));

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

describe('Style Package Weights page', () => {
  it('lists the packages of the scope, heaviest first', async () => {
    const fetchMock = installFetchMock(routes());
    open();

    await vi.waitFor(() => expect(rows().length).toBe(3));
    expect(rows().map((r) => r.textContent)).toEqual([
      expect.stringContaining('Wide'),
      expect.stringContaining('Compact'),
      expect.stringContaining('Corporate'),
    ]);
    expect(document.body.textContent).toContain('PDF Exporter: Style Package Weights');
    // The scope of the administration page reaches the endpoint.
    expect(String(fetchMock.mock.calls[0][0])).toContain('scope=project%2Felibrary%2F');
  });

  it('posts the reordered weights to this extension’s endpoint', async () => {
    const fetchMock = installFetchMock(routes());
    open();
    await vi.waitFor(() => expect(rows().length).toBe(3));

    // Move the second package up, then save.
    const up = rows()[1].querySelector<HTMLElement>('.caret-up, [title*="up" i]')!;
    await userEvent.click(up);
    const save = Array.from(document.querySelectorAll<HTMLElement>('button, .sbb-btn')).find(
      (b) => b.textContent?.trim() === 'Save',
    )!;
    await userEvent.click(save);

    await vi.waitFor(() => {
      const post = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST');
      expect(post).toBeDefined();
      expect(String(post![0])).toContain('/polarion/pdf-exporter/rest/internal/settings/style-package/weights');
      // Only this scope's own rows are stored: Corporate is defined globally, shown here read-only as
      // a fixed point in the ordering, and administered in the global scope.
      const sent = JSON.parse(String(post![1]!.body)) as { name: string; scope: string }[];
      expect(sent.map((w) => w.name)).toEqual(['Compact', 'Wide']);
      expect(sent.every((w) => w.scope === 'project/elibrary/')).toBe(true);
    });
  });

  it('reports a scope whose weights cannot be read', async () => {
    installFetchMock([
      { method: 'GET', match: /\/settings\/style-package\/weights\?/, json: { message: 'nope' }, status: 400 },
    ]);
    open();

    await vi.waitFor(() => expect(document.querySelector('.alert-error')).not.toBeNull());
  });
});
