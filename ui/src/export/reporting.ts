import type { SendRequest } from '@sbb-polarion/react-sbb-polarion';
import { errorMessageOf } from '../services/conversion';

/**
 * What an export dialog tells the user, and how it reads that out of a failure.
 *
 * Both the "Export to PDF" dialog and the Document Properties side panel report the same four outcomes -
 * an export that failed, an export that came back with a warning, a page width validation and its result -
 * and used to word and place them differently: the dialog showed alert boxes at the top of its form, the
 * panel plain red and orange text under its button, and the panel's validation errors ended up in a third
 * place again. They render the same alerts from the same strings now (see ExportFormView).
 */

/** How many invalid page previews a dialog shows; the endpoint is asked for one more to detect "more". */
export const MAX_PAGE_PREVIEWS = 4;

export const EXPORT_ERROR = 'Error occurred during PDF generation';
export const EXPORT_SUCCESS = 'PDF was successfully generated';
export const VALIDATION_ERROR = 'Error occurred validating pages width';
export const ALL_PAGES_VALID = 'All pages are valid';

export const STICKY_NOTES_WARNING =
  'Be aware that comments rendered in PDF as sticky notes are not compliant with any of PDF/A variants';

/** `<prefix>: <detail>` - the legacy `prefix + ": " + message`, with nothing appended where there is none. */
export const withDetail = (prefix: string, detail: string): string => (detail ? `${prefix}: ${detail}` : prefix);

/** What a rejected read, conversion or validation says, which is the server's message or nothing. */
export const messageOf = (error: unknown): string => (error instanceof Error ? error.message : '');

/** What `/validate` answers: a preview per page that does not fit, and the work items likely behind them. */
export interface WidthValidationResult {
  invalidPages: { content: string }[];
  suspiciousWorkItems: { id: string; link: string }[];
}

/**
 * Runs the page width validation.
 *
 * Rejects with the server's message where it gave one, so the caller reports a refused validation exactly
 * as it reports a refused conversion.
 */
export async function validatePageWidth(sendRequest: SendRequest, body: string): Promise<WidthValidationResult> {
  const response = await sendRequest({
    method: 'POST',
    url: `/validate?max-results=${MAX_PAGE_PREVIEWS + 1}`,
    contentType: 'application/json',
    body,
  });
  if (!response.ok) {
    throw new Error(await errorMessageOf(response));
  }
  return (await response.json()) as WidthValidationResult;
}

/** What the form says about the pages that did not fit, counting only the ones it is about to show. */
export function invalidPagesSummary(invalidPages: number): string {
  if (invalidPages > MAX_PAGE_PREVIEWS) {
    return `Invalid pages found. First ${MAX_PAGE_PREVIEWS} of them:`;
  }
  return `${invalidPages} invalid ${invalidPages === 1 ? 'page' : 'pages'} found:`;
}
