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
  await settleLayout();
  const app = document.querySelector('.app') as HTMLElement;
  await page.viewport(1280, Math.ceil(app.scrollHeight) + 40);
  await settleBeforeCapture();
  await expect(page.elementLocator(app)).toMatchScreenshot(name);
}

/** The parked pointer's resting place. Created once per file and kept in the DOM (see `parkPointer`). */
let parkingSpot: HTMLElement | undefined;

/**
 * Moves the pointer onto a spot of its own, and leaves it there.
 *
 * The mouse position survives a test, and a browser-mode file, since all of them run in one page - so
 * whatever the pointer last touched stays hovered while the next file takes its screenshot, and the
 * controls of these pages do react.
 *
 * The spot is NOT removed afterwards. Blink re-runs the hover hit test at the last known pointer
 * position whenever the hovered node leaves the DOM, so removing it would hand the hover straight to
 * whatever sits in that corner - and the settle that follows would then be long enough to fade that
 * element's shadow IN rather than out. It paints nothing (no background, no border), so it cannot show
 * up in a capture; it is `position: fixed` in the corner, above everything, and transparent to the
 * pointer's own hit testing only after the hover has landed on it.
 */
export async function parkPointer(): Promise<void> {
  if (!parkingSpot) {
    parkingSpot = document.createElement('div');
    parkingSpot.dataset.visualParkingSpot = '';
    parkingSpot.style.cssText = 'position:fixed;right:0;bottom:0;width:4px;height:4px;z-index:2147483647;';
    document.body.appendChild(parkingSpot);
  }
  // `force` skips the actionability check, which a modal <dialog> would otherwise fail: its ::backdrop
  // sits in the top layer, above every z-index. The pointer still moves, which is all this needs.
  await userEvent.hover(parkingSpot, { force: true });
}

/**
 * Waits for what changes layout, BEFORE a caller measures the element it is about to capture.
 *
 * Call it before reading `scrollHeight` to size the viewport: a height measured while a font is still
 * loading sizes the whole capture from a layout that has not settled, and `settleBeforeCapture` cannot
 * repair that afterwards - by then the viewport is already wrong.
 */
export async function settleLayout(): Promise<void> {
  await document.fonts.ready;
  await frame();
}

/**
 * Everything a page has to have finished before it is worth photographing: the fonts settled, the
 * pointer parked off any control, and a frame painted. Call it as the LAST thing before the capture,
 * once the viewport is final; call `settleLayout` before the measurement that sizes the viewport.
 *
 * Transitions and animations are off for the whole file (see test/setup.ts), so there is nothing left
 * to outrun with a sleep here.
 *
 * @param park whether to move the pointer away, for the captures that aim it somewhere themselves.
 */
export async function settleBeforeCapture(park = true): Promise<void> {
  await document.fonts.ready;
  if (park) {
    await parkPointer();
  }
  await frame();
  assertNotResampled();
}

/** Two frames: the first lets the style changes be laid out, the second lets them be painted. */
const frame = (): Promise<void> =>
  new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(() => resolve())));

/**
 * Fails the capture if Vitest had to scale the test iframe to fit the browser window.
 *
 * The window is sized in vitest.config.ts to be larger than every viewport the suites ask for, but the
 * viewports are computed from page content, so a UI change can outgrow it without touching that file.
 * The reference would then be silently resampled, which looks exactly like a legitimate one. This turns
 * that into a failure that names the fix.
 */
function assertNotResampled(): void {
  const frameElement = window.frameElement as HTMLElement | null;
  if (!frameElement) {
    return;
  }
  const rendered = frameElement.getBoundingClientRect().width;
  const requested = window.innerWidth;
  if (rendered > 0 && Math.abs(rendered - requested) > 1) {
    throw new Error(
      `The capture would be resampled: the test viewport is ${requested}px wide but is rendered at ` +
        `${Math.round(rendered)}px. Raise contextOptions.viewport in vitest.config.ts above every ` +
        `viewport this suite asks for.`,
    );
  }
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
