/**
 * True only inside the pinned Playwright Docker image, where the committed reference screenshots were
 * generated (scripts/docker-test.mjs sets PIXEL_REFERENCES=1; vitest.config.ts turns it into this
 * compile-time constant). Visual suites gate themselves on it with `describe.skipIf(!__PIXEL_REFERENCES__)`
 * so a run on any other host reports the behavior results instead of failing on font metrics.
 */
declare const __PIXEL_REFERENCES__: boolean;
