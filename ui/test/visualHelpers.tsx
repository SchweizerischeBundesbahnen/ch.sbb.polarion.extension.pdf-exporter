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
 *
 * The caller's `settled` says the page's own content has arrived; every dropdown being painted is
 * waited for on top of it, for every page, so a new suite cannot forget it (see `dropdownsUpgraded`).
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

  await vi.waitFor(() => expect(settled() && dropdownsUpgraded()).toBe(true));
  const app = document.querySelector('.app') as HTMLElement;
  await page.viewport(1280, Math.ceil(app.scrollHeight) + 40);
  await expect(page.elementLocator(app)).toMatchScreenshot(name);
}

export const found = (selector: string) => () => document.querySelector(selector) !== null;
export const filled = (selector: string) => () =>
  (document.querySelector<HTMLTextAreaElement>(selector)?.value ?? '') !== '';

/**
 * Whether every `<select>` on the page has become a searchable dropdown that is showing its selection.
 *
 * A `SearchableSelect` renders the `<select>` first and upgrades it in an effect, and the option list of
 * a dropdown fed by REST arrives later still (the vendored control picks it up through a
 * MutationObserver). So the element being in the DOM says nothing about the control being painted: a
 * snapshot taken in between catches blank triggers, and on a page full of dropdowns a different page
 * height as well. That is what made the Style Packages reference flaky, and every page carrying a
 * `ConfigurationsPane` has at least the one dropdown to be caught by it.
 *
 * A page with no dropdowns at all has nothing to wait for, so it passes - which is what lets
 * `snapshotFeature` apply this to every page unconditionally. It is only reached once the caller's own
 * content check has passed, so "no selects yet" cannot be mistaken for "no selects".
 */
export const dropdownsUpgraded = () => {
  const app = document.querySelector('.app');
  if (!app) return false;
  const selects = app.querySelectorAll('select');
  if (app.querySelectorAll('.searchable-dropdown').length !== selects.length) {
    return false;
  }
  // A single-select trigger is an input carrying the selected option's text; a multi-select one is a
  // div holding a chip per selection, or the placeholder when it has none.
  const triggers = Array.from(app.querySelectorAll<HTMLInputElement>('input.sd-trigger'));
  const multi = Array.from(app.querySelectorAll('.sd-trigger-multi'));
  return (
    triggers.every((trigger) => trigger.value !== '') &&
    multi.every((trigger) => trigger.querySelector('.sd-chip, .sd-placeholder') !== null)
  );
};
