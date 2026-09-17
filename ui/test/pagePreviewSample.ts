/**
 * A preview the size and shape of a rendered page, drawn rather than pasted in as a base64 blob.
 *
 * The endpoint answers with a screenshot of a page that came out too wide, so a 1x1 pixel - which is what
 * the behavior suites use - would say nothing about a gallery or a dialog whose whole job is to show one at
 * a readable size. Canvas rectangles rasterize exactly, so the same bytes come out of every run.
 *
 * Shared by the two visual suites that render the validation result: the panel at its 360px pane width and
 * the dialog at its own, which lay the same gallery out differently (its width comes from a container
 * query). One fixture keeps the two references comparable.
 */
export function pagePreview(): string {
  const canvas = document.createElement('canvas');
  canvas.width = 620;
  canvas.height = 877; // A4 at 75dpi, which is the shape of the real thing
  const page = canvas.getContext('2d')!;
  page.fillStyle = '#ffffff';
  page.fillRect(0, 0, canvas.width, canvas.height);
  page.fillStyle = '#c9c9c9';
  page.fillRect(60, 48, 320, 20); // a heading
  for (let y = 96; y < 800; y += 26) {
    page.fillRect(60, y, y % 78 === 0 ? 380 : 500, 10);
  }
  // Not SBB red, whose hex a pre-commit hook reads as an internal identifier
  page.fillStyle = '#cc0000';
  page.fillRect(560, 96, 60, 340); // the part that overflows the page, which is why it is being previewed
  return canvas.toDataURL('image/png').split(',')[1];
}
