/**
 * The three fields of the export dialogs a user can get wrong, validated exactly as the legacy
 * `ExportPanel.js` / `ExportPopup.js` validated them - same rules, same messages.
 *
 * Shared by the Document Properties side panel and (once it is migrated) the DLE toolbar popup: both
 * offer the same three switches with a value, and both refuse to start an export on a bad one.
 */

export const CHAPTERS_ERROR =
  "Please, provide comma separated list of integer values in 'Specific higher level chapters' field";

export const METADATA_FIELDS_ERROR = "Please, provide comma separated list of values in 'Metadata fields' field";

/**
 * The chapter numbers to export, or `undefined` when the entry is not a comma separated list of positive
 * integers. Spaces are dropped first, so "1, 2" is as good as "1,2"; a leading zero is not ("01" does not
 * round-trip through parseInt, which is what the legacy check tested for).
 */
export function parseChapters(raw: string | null | undefined): string[] | undefined {
  const chapters = (raw?.replace(/ /g, '') || '').split(',');
  for (const chapter of chapters) {
    const parsed = Number.parseInt(chapter);
    if (Number.isNaN(parsed) || parsed < 1 || String(parsed) !== chapter) {
      return undefined;
    }
  }
  return chapters;
}

/** The metadata field names to export: comma separated, trimmed, empty entries dropped. */
export function parseMetadataFields(raw: string | null | undefined): string[] {
  return (raw ?? '')
    .split(',')
    .map((field) => field.trim())
    .filter((field) => field.length > 0);
}

/** The error for a numbered-list style entry, or `undefined` when it is a combination of `1aAiI`. */
export function validateNumberedListStyles(raw: string | null | undefined): string | undefined {
  if (!raw || raw.trim().length === 0) {
    return 'Please, provide some value';
  }
  if (raw.match('[^1aAiI]+')) {
    return "Please, provide any combination of characters '1aAiI'";
  }
  return undefined;
}
