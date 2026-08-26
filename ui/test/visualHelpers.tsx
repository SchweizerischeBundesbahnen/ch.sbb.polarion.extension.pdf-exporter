import { expect, vi } from 'vitest';
import { render } from 'vitest-browser-react';
import { page, userEvent } from 'vitest/browser';
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
 * The pointer is taken off the page on the same terms - see `parkPointer`.
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
  await parkPointer();
  await expect(page.elementLocator(app)).toMatchScreenshot(name);
}

const PARKING_ID = 'visual-pointer-parking';
const NO_TRANSITIONS_ID = 'visual-no-transitions';

/**
 * Takes the pointer off the page and stops the transitions it drives, so that a reference shows the page
 * as it loads and not as it reacts to the mouse.
 *
 * The browser keeps one pointer position for the whole run, and a test file inherits it from whichever
 * file ran before. So a page can open with an element already hovered - and RSP answers `:hover` on its
 * controls with a box shadow, faded in over 150ms. The cover page reference was recorded that way, with
 * the shadow of the HTML editor baked into it: it held only as long as the pointer kept landing on that
 * editor, and disagreed by 4578 pixels as soon as it did not, which is what the three page suites do
 * when they run without the rest. The transitions are stopped rather than waited out, because moving the
 * pointer away starts the very same fade in reverse, which a shot can catch just as well.
 *
 * Call this **after** the viewport is final: a resize moves the page under a pointer that stays where it
 * is, which hovers whatever ends up beneath it.
 *
 * @param target where to leave the pointer. A dialog names one of its own hover-free elements, its title:
 *   a `<dialog>` is in the top layer, which paints over any box parked behind it, and its shadow root is
 *   out of reach of a stylesheet in the document. A page names nothing and gets a box of this helper's
 *   own - held in a corner of the viewport, painting nothing, outside the `.app` element the page
 *   references capture, and left in the document, because removing it hands the hover straight back to
 *   whatever is underneath.
 */
export async function parkPointer(target?: Element): Promise<void> {
  stopTransitions(document);
  const root = target?.getRootNode();
  if (root instanceof ShadowRoot) {
    stopTransitions(root);
  }
  await userEvent.hover(target ?? parkingBox());
}

/** The box the pointer is parked on - see {@link parkPointer}. */
function parkingBox(): HTMLElement {
  const existing = document.getElementById(PARKING_ID);
  if (existing) {
    return existing;
  }
  const parking = document.createElement('div');
  parking.id = PARKING_ID;
  parking.style.cssText =
    'position: fixed; top: 0; right: 0; width: 8px; height: 8px; z-index: 2147483647; pointer-events: auto';
  document.body.appendChild(parking);
  return parking;
}

/** Switches the transitions and animations of one document or shadow root off, once. */
function stopTransitions(root: Document | ShadowRoot): void {
  if (root.getElementById(NO_TRANSITIONS_ID)) {
    return;
  }
  const style = document.createElement('style');
  style.id = NO_TRANSITIONS_ID;
  style.textContent = '*, *::before, *::after { transition: none !important; animation: none !important }';
  (root instanceof Document ? root.head : root).appendChild(style);
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
