import { expect, vi } from 'vitest';
import { render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';
import type { Route } from './mockFetch';

/**
 * Opens one feature of the app with its REST mocked, waits for the page to settle and snapshots the
 * whole app root at its natural height. Shared by the per-page visual suites, which otherwise repeat
 * the same six lines each.
 */
export async function snapshotFeature(
  feature: string,
  routes: Route[],
  settled: () => boolean,
  name: string,
  scope = 'project/elibrary/',
): Promise<void> {
  installFetchMock(routes);
  window.history.replaceState({}, '', `?feature=${feature}&embedded=true&scope=${scope}`);
  render(<App />);

  await vi.waitFor(() => expect(settled()).toBe(true));
  const app = document.querySelector('.app') as HTMLElement;
  await page.viewport(1280, Math.ceil(app.scrollHeight) + 40);
  await expect(page.elementLocator(app)).toMatchScreenshot(name);
}

export const found = (selector: string) => () => document.querySelector(selector) !== null;
export const filled = (selector: string) => () =>
  (document.querySelector<HTMLTextAreaElement>(selector)?.value ?? '') !== '';
