/**
 * The conversion protocol: how an export dialog turns a set of export parameters into a downloaded file.
 *
 * The TypeScript port of the legacy `ExportContext.js`'s half that talked to the server - submit a
 * conversion job, poll it, download the result, plus the two side errands an export can involve (a test
 * run's attachments, and the documents of a baseline collection). Every export surface of the extension
 * runs through here: the toolbar popup, the document properties side panel and the bulk export widget.
 *
 * The legacy code drove `XMLHttpRequest` through callbacks; this is `fetch` through promises, which is what
 * lets the three surfaces await an export instead of threading success and error callbacks. The requests,
 * the headers read off them and the messages built from those headers are unchanged.
 */
import type { SendRequest } from '@grigoriev/react-sbb-polarion';

/** The two request flavors a conversion needs: the REST base, and a URL the server handed out. */
export interface Remote {
  sendRequest: SendRequest;
  sendAbsoluteRequest: SendRequest;
}

export interface ConversionResult {
  blob: Blob;
  /** The name the server suggests, from the `Export-Filename` header. */
  fileName: string | null;
  /** What the user should know about the result although it was produced - see {@link warningOf}. */
  warning: string | null;
}

/** How long the job is left alone between polls, as `ExportContext.PULL_INTERVAL`. */
export const POLL_INTERVAL = 1000;

const CONVERT_JOBS_URL = '/convert/jobs';

const NOT_VALIDATED_WARNING = "Resulting PDF couldn't be validated if it's compliant with the selected PDF variant.";
const NOT_COMPLIANT_WARNING = "Be aware that resulting PDF isn't compliant with the selected PDF variant.";

const delay = (milliseconds: number): Promise<void> => new Promise((resolve) => setTimeout(resolve, milliseconds));

/** The message an error response carries, whatever shape it came in, or the empty string. */
export async function errorMessageOf(response: Response | Blob): Promise<string> {
  try {
    const text = await response.text();
    const error = text ? (JSON.parse(text) as { message?: string; errorMessage?: string }) : null;
    return error?.message ?? error?.errorMessage ?? '';
  } catch {
    return '';
  }
}

const failed = async (response: Response): Promise<Error> => new Error(await errorMessageOf(response));

/**
 * What a finished conversion warns about, from the headers of its result.
 *
 * Two things can be wrong with a PDF that was still produced: work item images that could not be read (the
 * renderer substitutes a placeholder), and a result that does not comply with the requested PDF/A or PDF/UA
 * variant - or could not be checked for it, which is a separate message because a missing header means the
 * validator did not run at all.
 *
 * The parts are joined with blank lines rather than the legacy `<br><br>`: the side panel already rewrote
 * those to newlines before rendering, and the popup - which set the message as `textContent` to keep an
 * arbitrary server string out of `innerHTML` - showed the literal tags to the user.
 */
export function warningOf(headers: Headers): string | null {
  const warnings: string[] = [];

  const missingAttachments = Number.parseInt(headers.get('Missing-WorkItem-Attachments-Count') ?? '', 10);
  if (missingAttachments > 0) {
    const workItems = headers.get('WorkItem-IDs-With-Missing-Attachment') ?? '';
    warnings.push(
      `${missingAttachments} image(s) in WI(s) ${workItems} were not exported. ` +
        "They were replaced with an image containing 'This image is not accessible'.",
    );
  }

  const compliant = headers.get('PDF-Variant-Compliant');
  if (compliant === null) {
    warnings.push(NOT_VALIDATED_WARNING);
  } else if (compliant !== 'true') {
    warnings.push(NOT_COMPLIANT_WARNING);
  }

  return warnings.length === 0 ? null : warnings.join('\n\n');
}

/**
 * Converts one item to PDF: submits the job and polls it until the file is ready.
 *
 * Rejects with the message the server gave, empty when it gave none - the caller decides what to say around
 * it, as the wording differs per surface.
 */
export async function convertPdf(
  remote: Remote,
  requestBody: string,
  pollInterval: number = POLL_INTERVAL,
): Promise<ConversionResult> {
  const submitted = await remote.sendRequest({
    method: 'POST',
    url: CONVERT_JOBS_URL,
    contentType: 'application/json',
    body: requestBody,
  });
  if (!submitted.ok) {
    throw await failed(submitted);
  }

  const job = submitted.headers.get('Location');
  if (!job) {
    throw new Error('The conversion job was accepted without a location to poll.');
  }

  for (;;) {
    await delay(pollInterval);
    const polled = await remote.sendAbsoluteRequest({ method: 'GET', url: job });
    if (polled.status === 202) {
      continue;
    }
    if (!polled.ok) {
      throw await failed(polled);
    }
    return {
      blob: await polled.blob(),
      fileName: polled.headers.get('Export-Filename'),
      warning: warningOf(polled.headers),
    };
  }
}

/** What a merge (single-file) export needs beyond the request body: interruption polling and the job url. */
export interface MergeOptions {
  /** Polled around every wait so the run stops polling once the user pressed Stop. */
  isInterrupted?: () => boolean;
  /** Handed the job url the moment the job is accepted, so a later Stop can cancel the backend job. */
  onJobStarted?: (jobUrl: string) => void;
  pollInterval?: number;
}

/** Thrown by {@link convertMergePdf} when the run was stopped, so the caller can tell it from a failure. */
export class ConversionInterrupted extends Error {
  constructor() {
    super('Conversion interrupted');
    this.name = 'ConversionInterrupted';
  }
}

/**
 * What a finished merge warns about: the shared {@link warningOf}, plus the documents the merge service
 * could not convert and left out of the combined PDF (its `X-Documents-Failed` header).
 */
function mergeWarningOf(headers: Headers): string | null {
  const base = warningOf(headers);
  const failed = Number.parseInt(headers.get('X-Documents-Failed') ?? '', 10);
  if (!(failed > 0)) {
    return base;
  }
  const message = `${failed} document(s) failed to convert and were excluded from the merged PDF.`;
  return base ? `${base}\n\n${message}` : message;
}

/**
 * Converts several documents into one merged PDF: submits the whole set as a single job and polls it until
 * the combined file is ready, the way {@link convertPdf} polls one document.
 *
 * Unlike the per-item run this is one backend job, so it is cancellable: `onJobStarted` hands back the job
 * url the moment it is known, and `isInterrupted` is checked around every wait so a Stop pressed mid-run
 * stops the polling. A run stopped this way rejects with {@link ConversionInterrupted}, not a failure.
 */
export async function convertMergePdf(
  remote: Remote,
  requestBody: string,
  options: MergeOptions = {},
): Promise<ConversionResult> {
  const isInterrupted = options.isInterrupted ?? (() => false);
  const pollInterval = options.pollInterval ?? POLL_INTERVAL;

  const submitted = await remote.sendRequest({
    method: 'POST',
    url: '/convert/merge/jobs',
    contentType: 'application/json',
    body: requestBody,
  });
  if (!submitted.ok) {
    throw await failed(submitted);
  }

  const job = submitted.headers.get('Location');
  if (!job) {
    throw new Error('The merge conversion job was accepted without a location to poll.');
  }
  options.onJobStarted?.(job);

  for (;;) {
    if (isInterrupted()) {
      throw new ConversionInterrupted();
    }
    await delay(pollInterval);
    if (isInterrupted()) {
      throw new ConversionInterrupted();
    }
    const polled = await remote.sendAbsoluteRequest({ method: 'GET', url: job });
    if (polled.status === 202) {
      continue;
    }
    if (!polled.ok) {
      throw await failed(polled);
    }
    return {
      blob: await polled.blob(),
      fileName: polled.headers.get('Export-Filename'),
      warning: mergeWarningOf(polled.headers),
    };
  }
}

/**
 * Best-effort cancellation of a running conversion job by its job url - the `Location` a submit handed out.
 * Failures are swallowed: the run has already been told to stop, and the job times out on the server anyway.
 */
export async function cancelJob(remote: Remote, jobUrl: string): Promise<void> {
  if (!jobUrl) {
    return;
  }
  try {
    await remote.sendAbsoluteRequest({ method: 'POST', url: `${jobUrl}/cancel` });
  } catch (error) {
    console.warn('Failed to cancel export job:', error);
  }
}

/** Whether the bulk processing (merge) service is configured and reachable, as the merge option needs it. */
export async function isBulkProcessingAvailable(remote: Remote): Promise<boolean> {
  try {
    const response = await remote.sendRequest({ method: 'GET', url: '/bulk-processing/status' });
    if (!response.ok) {
      return false;
    }
    const status = (await response.json()) as { available?: boolean };
    return !!status.available;
  } catch {
    return false;
  }
}

/**
 * Starts a download of the given blob.
 *
 * The link is created in the top window when this runs in an iframe, which is where every one of these
 * surfaces runs: the document editor and the report page are both framed, and a download triggered from
 * inside the frame is what browsers block.
 */
export function downloadBlob(blob: Blob, fileName: string): void {
  const objectUrl = (window.URL ?? window.webkitURL).createObjectURL(blob);
  const targetWindow = window.self !== window.top ? (window.top ?? window) : window;

  const link = targetWindow.document.createElement('a');
  link.href = objectUrl;
  link.download = fileName;
  link.target = '_blank';
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(objectUrl), 100);
}

interface TestRunAttachment {
  id: string;
}

/**
 * Downloads a test run's attachments next to the exported PDF, one file each.
 *
 * Only used where the attachments are not embedded into the PDF itself. Failures are logged and not raised:
 * the export they accompany has its own outcome, and the legacy code did the same.
 */
export async function downloadTestRunAttachments(
  remote: Remote,
  options: {
    projectId: string;
    testRunId: string;
    revision?: string | null;
    filter?: string | null;
    testCaseFieldId?: string | null;
    /** Injectable so a test can watch what would be downloaded instead of downloading it. */
    download?: typeof downloadBlob;
  },
): Promise<void> {
  const download = options.download ?? downloadBlob;
  const parameters = new URLSearchParams();
  if (options.revision) parameters.set('revision', options.revision);
  if (options.filter) parameters.set('filter', options.filter);
  if (options.testCaseFieldId) parameters.set('testCaseFilterFieldId', options.testCaseFieldId);

  const base = `/projects/${encodeURIComponent(options.projectId)}/testruns/${encodeURIComponent(options.testRunId)}/attachments`;
  const listed = await remote.sendRequest({ method: 'GET', url: `${base}?${parameters.toString()}` });
  if (!listed.ok) {
    console.error('Error fetching attachments:', await errorMessageOf(listed));
    return;
  }

  const revision = options.revision ? `?revision=${encodeURIComponent(options.revision)}` : '';
  for (const attachment of (await listed.json()) as TestRunAttachment[]) {
    const content = await remote.sendRequest({
      method: 'GET',
      url: `${base}/${encodeURIComponent(attachment.id)}/content${revision}`,
    });
    if (!content.ok) {
      console.error(`Error downloading attachment ${attachment.id}:`, await errorMessageOf(content));
      continue;
    }
    download(await content.blob(), content.headers.get('Filename') ?? attachment.id);
  }
}

/** One document of a baseline collection, as `/collections/{id}/documents` lists them. */
export interface CollectionDocument {
  projectId: string;
  spaceId: string;
  documentName: string;
  documentType: string;
  revision?: string | null;
  fileName?: string | null;
}

/** Lists the documents of a baseline collection without converting them - what a merge run expands into. */
export async function listCollectionDocuments(
  remote: Remote,
  projectId: string,
  collectionId: string,
): Promise<CollectionDocument[]> {
  const listed = await remote.sendRequest({
    method: 'GET',
    url: `/projects/${encodeURIComponent(projectId)}/collections/${encodeURIComponent(collectionId)}/documents`,
  });
  if (!listed.ok) {
    throw await failed(listed);
  }
  return ((await listed.json()) as CollectionDocument[] | null) ?? [];
}

/**
 * Exports every document of a baseline collection, each downloaded as it is ready.
 *
 * Live Reports are skipped unless the widget was configured to export pages too, which is the
 * `exportPages` parameter of the Bulk PDF Export widget.
 *
 * The conversions run concurrently, as the legacy code ran them, and the failures are collected: the legacy
 * reported one only when it happened to be the last document to finish, so a collection whose first
 * document failed reported nothing at all.
 */
export async function convertCollectionDocuments(
  remote: Remote,
  options: {
    projectId: string;
    collectionId: string;
    exportPages: boolean;
    /** The export request for one document; each document of the collection overrides where it lives. */
    params: object;
    toRequestBody: (params: Record<string, unknown>) => string;
    /** Injectable so a test can watch what would be downloaded instead of downloading it. */
    download?: typeof downloadBlob;
    pollInterval?: number;
  },
): Promise<void> {
  const download = options.download ?? downloadBlob;
  const listedDocuments = await listCollectionDocuments(remote, options.projectId, options.collectionId);
  const documents = listedDocuments.filter(
    (document) => options.exportPages || document.documentType !== 'LIVE_REPORT',
  );
  if (documents.length === 0) {
    console.warn('No documents found in the collection.');
    return;
  }

  const outcomes = await Promise.allSettled(
    documents.map(async (document) => {
      const result = await convertPdf(
        remote,
        options.toRequestBody({
          ...options.params,
          projectId: document.projectId,
          locationPath: `${document.spaceId}/${document.documentName}`,
          revision: document.revision,
          documentType: document.documentType,
        }),
        options.pollInterval,
      );
      const fallbackName = `${document.projectId}_${document.spaceId}_${document.documentName}.pdf`;
      download(result.blob, document.fileName || fallbackName);
    }),
  );

  const firstFailure = outcomes.find((outcome) => outcome.status === 'rejected');
  if (firstFailure?.status === 'rejected') {
    throw firstFailure.reason instanceof Error ? firstFailure.reason : new Error(String(firstFailure.reason));
  }
}
