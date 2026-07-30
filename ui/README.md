# PDF Exporter UI

A React + Vite single-page app on [react-sbb-polarion](https://github.com/grigoriev/react-sbb-polarion)
(RSP), served from the `pdf-exporter-app` webapp.

It is replacing the JSP administration UI **page by page**, so both live side by side for now: the
administration entries listed below are React, the rest (style packages, the
Velocity/CSS editors, localization, webhooks) still point at `pdf-exporter-admin`. That webapp goes
away once the last of them is converted.

## Feature routing

There is one `index.html` / bundle. The page to render is chosen from the `feature` query parameter:

- `/` (no param) renders a development landing page listing every feature.
- `/?feature=about` - About (RSP's shared `About`).
- `/?feature=disclaimer` - Usage Disclaimer. The only page whose article is **not** a REST call:
  generic serves `/readme` and `/user-guide` but has no disclaimer endpoint, so `disclaimer.html` is
  read as a static file from this app's own webapp, where markdown2html writes it during the build.
- `/?feature=user-guide` - User Guide (RSP's shared `UserGuide`).
- `/?feature=style-package-weights` - Style Package Weights (RSP's shared `StylePackageWeights` over
  this extension's `settings/style-package/weights` endpoint; the list is shared with docx-exporter).
- `/?feature=authorization` - Authorization (RSP's shared `AuthorizationSettings` over the
  `authorization` named setting). It reads generic's `/roles` endpoint, which is opt-in: the two roles
  controllers are registered in `PdfExporterRestApplication` for this page.

Features are declared in [`src/features.tsx`](src/features.tsx). Add a page component under
`src/pages/`, register it there, and it appears on the landing page automatically. The ids must stay
in sync with the `pageUrl`s in `src/main/resources/META-INF/hivemodule.xml` — a mismatch shows up as a
blank page in Polarion and no test catches it.

## Local development

No Polarion restart is needed to develop the UI:

```bash
cd ui
cp .env.local.template .env.local   # optional: VITE_BASE_URL / VITE_BEARER_TOKEN for real REST calls
npm install
npm run dev                          # http://localhost:5173/
```

REST calls are proxied to the Polarion instance in `VITE_BASE_URL`; a personal access token in
`VITE_BEARER_TOKEN` switches `useRemote` from the session `/internal` endpoints to the token `/api`
ones.

## Running the tests

**One command, locally and in CI: `npm run test:coverage:docker`.** It runs the full suite (behavior +
visual regression) plus the 80% istanbul coverage gate inside the pinned Playwright Docker image,
which is what the Maven `test` phase and the pre-commit hook execute. Docker must be running.

```bash
npm run test:coverage:docker   # the canonical run: full suite + coverage gate, in the pinned image
npm run test:coverage          # fast local loop: behavior only + the gate, no Docker, no pixels
npm run test:update:docker     # regenerate the committed reference PNGs after an intentional UI change
```

> Do **not** run `npm run test:coverage:full` directly outside a container. It is the inner command the
> Docker wrapper invokes; the visual suites detect that they are not in the reference environment and
> skip themselves, so a run there proves nothing about the screenshots.

## Formatting & linting

```bash
npm run format          # Prettier: format every file in place
npm run format:check    # Prettier: check only (what pre-commit / CI runs)
npm run lint            # ESLint: report problems
npm run lint:fix        # ESLint: auto-fix what it can
```

The repo's pre-commit hooks run `format:check`, `lint` and the dockerized coverage suite on any change
under `ui/`. They are check-only and never modify your files.

## Production build

`npm run build` emits the bundle to `ui/dist/app` with base path
`/polarion/pdf-exporter-app/ui/app/`. The Maven build (frontend-maven-plugin +
maven-resources-plugin) runs this automatically and copies the bundle into
`src/main/resources/webapp/pdf-exporter-app/app`, where `AADSynchronizerAppServlet` serves it at
`/polarion/pdf-exporter-app/ui/app/index.html`.
