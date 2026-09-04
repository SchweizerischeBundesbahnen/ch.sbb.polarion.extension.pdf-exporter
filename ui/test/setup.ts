// Runs before every test file (see vitest.config.ts setupFiles).
//
// Loads the same stylesheets the app renders with, so the browser paints components realistically:
//   1. react-sbb-polarion's bundled control CSS (tokens + buttons/inputs/checkboxes/searchable-dropdown/
//      alerts + the shared component styles) - the same import main.tsx uses. It also owns the page
//      shell on `.app` (padding, font, size, color), so no base-font rule is needed here: every page of
//      this app renders under `.app`, and there are no shadow-mounted form-extension panels.
//   2. this app's own App.css (the dev Landing list and the alert styling).
// The Polarion-served stylesheet linked in index.html (presentation.css) is not bundled and is not loaded
// here; it is baseline chrome. Also registers the jest-dom matchers.
import '@sbb-polarion/react-sbb-polarion/style.css';
import '@testing-library/jest-dom/vitest';
import '../src/App.css';

// Transitions and animations are off for every capture. A screenshot taken mid-fade is a reference that
// only sometimes reproduces, and the durations are react-sbb-polarion's, which can change them without
// this repository noticing. Killing them removes the race instead of outrunning it with a sleep.
//
// Grayscale antialiasing is NOT pinned here: `-webkit-font-smoothing` is implemented only on macOS in
// Blink, so on the Linux container the rule parses and is ignored - a reference captured with it is
// byte-identical to one captured without. `--disable-lcd-text` in vitest.config.ts is the platform
// independent way to ask for the same thing.
const STILLNESS = '*, *::before, *::after { transition: none !important; animation: none !important; }';

const stillness = document.createElement('style');
stillness.textContent = STILLNESS;
document.head.appendChild(stillness);

// A shadow root sees none of the document's rules, and three of this app's surfaces are mounted in one -
// the side panel, the export dialog and the bulk export widget. Their controls are react-sbb-polarion's,
// which transitions `border-color` over 150ms, so a value read or captured right after a class changed is
// the value it is fading FROM: an input marked `.error` reads as the grey it just left, whatever the
// stylesheet says. That is what this plants in every shadow root as it is created.
//
// An adopted stylesheet rather than a `<style>` child, because `mountInShadow` calls `replaceChildren()`
// on the root it is given - which would sweep a child straight out again - and because adopted sheets are
// applied after the root's own, so nothing in the root can outrank this.
const stillnessSheet = new CSSStyleSheet();
stillnessSheet.replaceSync(STILLNESS);
const attachShadow = Element.prototype.attachShadow;
Element.prototype.attachShadow = function attachStillShadow(init: ShadowRootInit): ShadowRoot {
  const root = attachShadow.call(this, init);
  root.adoptedStyleSheets = [...root.adoptedStyleSheets, stillnessSheet];
  return root;
};
