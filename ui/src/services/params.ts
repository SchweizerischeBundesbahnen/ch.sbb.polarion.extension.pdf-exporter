/**
 * Whether the app is running "embedded" (inside Polarion's admin iframe), where the user must not be
 * able to navigate away from the page Polarion opened.
 *
 * Not embedded by default: only an explicit `?embedded=true` turns it on. hivemodule.xml opens pages
 * with `embedded=true`; dev navigation (opening a page directly or from the Landing stub) omits the
 * param and keeps the "Overview" back link.
 */
export function isEmbedded(): boolean {
  return new URLSearchParams(window.location.search).get('embedded') === 'true';
}
