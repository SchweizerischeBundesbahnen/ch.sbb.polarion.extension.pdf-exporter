// Runs before every test file (see vitest.config.ts setupFiles).
//
// Loads the same stylesheets the app renders with, so the browser paints components realistically:
//   1. react-sbb-polarion's bundled control CSS (tokens + buttons/inputs/checkboxes/searchable-dropdown/
//      alerts + the shared component styles) - the same import main.tsx uses. It also owns the page
//      shell on `.app` (padding, font, size, color), so no base-font rule is needed here: every page of
//      this app renders under `.app`, and there are no shadow-mounted form-extension panels.
//   2. this app's own App.css (the dev Landing list and the alert styling).
// The Polarion-served stylesheets linked in index.html (presentation.css, github-markdown-light.css)
// are not bundled and are not loaded here; they are baseline chrome / help-article styling. Also
// registers the jest-dom matchers.
import '@sbb-polarion/react-sbb-polarion/style.css';
import '@testing-library/jest-dom/vitest';
import '../src/App.css';
