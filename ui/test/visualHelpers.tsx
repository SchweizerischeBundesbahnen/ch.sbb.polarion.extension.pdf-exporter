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
  const app = document.querySelector('.app') as HTMLElement;
  await page.viewport(1280, Math.ceil(app.scrollHeight) + 40);
  await settleBeforeCapture();
  await expect(page.elementLocator(app)).toMatchScreenshot(name);
}

/**
 * Everything a page has to have finished before it is worth photographing: its text rendering pinned, its
 * fonts settled, the pointer off any control, the hover styling that pointer leaves behind faded out, and a
 * frame painted. Call it as the LAST thing before the capture, once the viewport is final.
 *
 * Two of these were measured to make the Cover Page reference disagree with itself run after run, and this
 * is what each was worth:
 *
 *  - the pointer, 4578 subpixels: the reference had been captured with the mouse resting on the HTML
 *    editor, so it carried a `:hover` shadow that no other run reproduced (see `parkPointer`);
 *  - the antialiasing, 644 subpixels: the same glyphs in the same place, drawn with a different gamma,
 *    depending on which file had run before (see `pinTextRendering`).
 *
 * The rest are precautions rather than proven culprits, kept because they are cheap and each removes a way
 * for a capture to land on an unfinished page: `document.fonts.ready` (the pages name fonts Polarion serves
 * and nothing serves under test, and `document.fonts.status` was indeed "loading" in a lone run against
 * "loaded" in a run after another file), the wait for the hover transition, and the two frames.
 *
 * @param park whether to move the pointer away, for the captures that do not aim it somewhere themselves.
 */
export async function settleBeforeCapture(park = true): Promise<void> {
  pinTextRendering();
  await document.fonts.ready;
  if (park) {
    await parkPointer();
  }
  // The hover styling of whatever the pointer leaves behind fades over `transition: box-shadow .15s`, and a
  // capture in the middle of that fade is a reference that only sometimes reproduces.
  await new Promise((resolve) => setTimeout(resolve, 200));
  // Two frames: the first lets the style changes above be laid out, the second lets them be painted.
  await new Promise((resolve) => requestAnimationFrame(() => requestAnimationFrame(() => resolve(undefined))));
}

/**
 * Pins text to grayscale antialiasing for the capture.
 *
 * Chromium decides per layer how to rasterize text, and the decision depends on the compositing of the page
 * as a whole - which differs between "this file ran on its own" and "this file ran after that one". The
 * result is the same glyphs at the same coordinates with a different gamma, 644 subpixels apart, and it
 * flipped from run to run on the dropdown triggers of the Cover Page and Style Packages references. Asking
 * for grayscale explicitly takes the decision away from the compositor.
 *
 * Test-only, injected for the capture: nothing in the product asks for this, and the references are
 * regenerated with it so they and the runs agree.
 */
function pinTextRendering(): void {
  if (document.getElementById('visual-text-rendering')) {
    return;
  }
  const style = document.createElement('style');
  style.id = 'visual-text-rendering';
  style.textContent = '*, *::before, *::after { -webkit-font-smoothing: antialiased !important; }';
  document.head.appendChild(style);
}

/**
 * Moves the pointer onto a spot of its own.
 *
 * The mouse position survives a test, and a browser-mode file, since all of them run in one page - so
 * whatever the pointer last touched stays hovered while the next file takes its screenshot. The controls
 * of these pages do react: `.code-editor:hover` and the `sbb-btn` / dropdown-trigger rules paint a shadow.
 * That is what made the Cover Page reference carry a hover shadow under the HTML editor - and then
 * disagree with every run in which the pointer happened to rest elsewhere.
 *
 * The spot is `position: fixed` in the corner and above everything, so it has no styling of its own and
 * nothing can intercept the hover. `force` skips the actionability check for the sake of the pages that
 * capture inside a modal `<dialog>`, whose backdrop sits above every z-index.
 */
export async function parkPointer(): Promise<void> {
  const spot = document.createElement('div');
  spot.style.cssText = 'position:fixed;right:0;bottom:0;width:4px;height:4px;z-index:2147483647;';
  document.body.appendChild(spot);
  try {
    await userEvent.hover(spot, { force: true });
  } finally {
    spot.remove();
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
