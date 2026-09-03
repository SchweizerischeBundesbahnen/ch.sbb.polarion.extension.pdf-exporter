import type { SendRequest } from '@sbb-polarion/react-sbb-polarion';
import { toast } from 'sonner';
import { errorMessageOf } from '../services/conversion';

/**
 * What an export surface tells the user, and how it reads that out of a failure.
 *
 * The "Export to PDF" dialog and the Document Properties side panel report the same outcomes - an export
 * that failed, an export that came back with a warning, one that succeeded, a validation that could not be
 * run - and used to word and place them differently: the dialog showed alert boxes inside its form, the
 * panel plain red and orange text under its button, and the panel's validation errors ended up in a third
 * place again. They report through these functions now, which is `sonner`'s `toast` - the same toasts every
 * administration page of this extension has always used, and the same host (RSP's `Toaster`, see
 * ExportToaster).
 *
 * Toasts rather than a block in the form because these are events, not state: the message no longer takes
 * a place in a layout it has to be given room in, cannot be scrolled away from the button that produced it,
 * and is the same message in the same corner whichever surface raised it. What stays in the form is what
 * describes a *state*: the form that could not be loaded, and the result of a validation that ran.
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

/**
 * How long a message of each kind stays, and how it can be sent away.
 *
 * A failure waits to be dismissed, which is what the alert box it replaced did: it names something the user
 * has to read, often a server message, and an export dialog is open in front of them while they do. A
 * warning is a conversion that produced a file with something to know about it, so it outstays the 5s an
 * administration page's "Data successfully saved." gets, but it does go. A success needs no more than that
 * 5s default.
 *
 * All three carry a close button. A failure has to have one - nothing else would take it off the screen -
 * and the other two are given one so that a reader who is done with a message never has to wait for it,
 * whichever kind it is.
 */
const FAILURE = { duration: Infinity, closeButton: true } as const;
const WARNING = { duration: 20_000, closeButton: true } as const;
const SUCCESS = { closeButton: true } as const;

/** Reports a failed operation, with whatever the server said about it. */
export const reportFailure = (prefix: string, failure: unknown): void => {
  toast.error(withDetail(prefix, messageOf(failure)), FAILURE);
};

/** Reports an operation refused before it started, whose message names the field that is wrong. */
export const reportRefusal = (message: string): void => {
  toast.error(message, FAILURE);
};

/** Reports something to know about a file that was produced all the same. */
export const reportWarning = (message: string): void => {
  toast.warning(message, WARNING);
};

export const reportSuccess = (message: string): void => {
  toast.success(message, SUCCESS);
};

/**
 * The id the sticky notes warning is raised under.
 *
 * It is the one message that is not an event: it belongs to a checkbox, so it is taken back when that
 * checkbox goes off again, and a fixed id is what makes ticking it twice raise one toast rather than two.
 */
export const STICKY_NOTES_TOAST = 'pdf-exporter-sticky-notes';

/** Raises or takes back the sticky notes warning, as its checkbox is switched. */
export const reportStickyNotes = (renderAsStickyNotes: boolean): void => {
  if (renderAsStickyNotes) {
    toast.warning(STICKY_NOTES_WARNING, { ...WARNING, id: STICKY_NOTES_TOAST });
  } else {
    toast.dismiss(STICKY_NOTES_TOAST);
  }
};

/** Takes back whatever was last reported, which is what an operation does before it starts another. */
export const clearReports = (): void => {
  toast.dismiss();
};

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
