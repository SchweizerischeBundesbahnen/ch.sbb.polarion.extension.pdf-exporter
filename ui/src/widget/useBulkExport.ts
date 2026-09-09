import { useCallback, useRef, useState } from 'react';
import type { ExportParamsJson } from '../export/exportParams';
import { toRequestBody } from '../export/exportParams';
import type { Remote } from '../services/conversion';
import {
  ConversionInterrupted,
  cancelJob,
  convertCollectionDocuments,
  convertMergePdf,
  convertPdf,
  downloadBlob,
  downloadTestRunAttachments,
  listCollectionDocuments,
} from '../services/conversion';
import type { BulkExportItem, DocumentType } from './types';

export type BulkExportStatus = 'closed' | 'in-progress' | 'interrupted' | 'finished';
export type ExportRowState = 'paused' | 'in-progress' | 'finished' | 'interrupted' | 'error';

export interface ExportRow {
  item: BulkExportItem;
  state: ExportRowState;
  error?: string;
}

export interface BulkExportState {
  status: BulkExportStatus;
  rows: ExportRow[];
  /**
   * How many items the run is done with, failures included. The vanilla widget counted successes only,
   * which left the progress bar short of the end whenever an export failed.
   */
  processed: number;
  errors: boolean;
  /** True while the selection is being converted into one merged PDF rather than one file per item. */
  merge: boolean;
}

const CLOSED: BulkExportState = { status: 'closed', rows: [], processed: 0, errors: false, merge: false };
const DEFAULT_MERGE_FILENAME = 'merged-document.pdf';

/** The document type of a single item, which may differ from the widget's when it lists collections. */
export function documentTypeOf(item: BulkExportItem): DocumentType | '' {
  switch (item.type) {
    case 'Module':
      return 'LIVE_DOC';
    case 'RichPage':
      return 'LIVE_REPORT';
    case 'TestRun':
      return 'TEST_RUN';
    case 'BaselineCollection':
      return 'BASELINE_COLLECTION';
    default:
      return '';
  }
}

/** What the progress list writes in front of an item's name. */
export function itemTypeLabel(item: BulkExportItem): string {
  switch (item.type) {
    case 'Module':
      return 'Document: ';
    case 'RichPage':
      return 'Report: ';
    case 'TestRun':
      return 'Test Run: ';
    case 'BaselineCollection':
      return 'Collection: ';
    default:
      return '';
  }
}

/** Collections are named, everything else is addressed by its space and id. */
export function itemName(item: BulkExportItem): string {
  if (documentTypeOf(item) === 'BASELINE_COLLECTION') {
    return item.name ?? '';
  }
  const space = !item.spaceId || item.spaceId === '_default' ? '' : `${item.spaceId} / `;
  return `${space}${item.id ?? ''}`;
}

/** What a failed export says. Anything the server did not explain reads as a plain failure. */
const failureOf = (error: unknown): string =>
  error instanceof Error && error.message ? error.message : 'Export failed';

/** One merge document as the endpoint takes it: the params without what is not set, as `toRequestBody` sends a single one. */
const withoutEmpty = (params: ExportParamsJson): Record<string, unknown> =>
  Object.fromEntries(Object.entries(params).filter(([, value]) => value !== null && value !== undefined));

/** What the run reaches outside itself for, so the tests can watch it instead of converting. */
export interface BulkExportDependencies {
  convert?: typeof convertPdf;
  convertCollection?: typeof convertCollectionDocuments;
  convertMerge?: typeof convertMergePdf;
  cancel?: typeof cancelJob;
  listCollection?: typeof listCollectionDocuments;
  download?: typeof downloadBlob;
  downloadAttachments?: typeof downloadTestRunAttachments;
}

/**
 * Exports the selected items one after another, the way the widget's vanilla predecessor did: a single
 * conversion at a time, each downloaded as soon as it is ready, and the run stoppable in between.
 *
 * The conversion protocol itself is `services/conversion.ts`, which the export dialog and the document
 * properties side panel run through as well - only the sequencing and the progress state live here.
 */
export default function useBulkExport(exportPages: boolean, remote: Remote, deps: BulkExportDependencies = {}) {
  const convert = deps.convert ?? convertPdf;
  const convertCollection = deps.convertCollection ?? convertCollectionDocuments;
  const convertMerge = deps.convertMerge ?? convertMergePdf;
  const cancel = deps.cancel ?? cancelJob;
  const listCollection = deps.listCollection ?? listCollectionDocuments;
  const download = deps.download ?? downloadBlob;
  const downloadAttachments = deps.downloadAttachments ?? downloadTestRunAttachments;

  const [state, setState] = useState<BulkExportState>(CLOSED);
  const stopped = useRef(false);
  // A merge export is a single backend job; keep what Stop needs to cancel it. Refs, not state, so that
  // the stop callback stays stable and sees the latest job url without re-running the export.
  const merging = useRef(false);
  const cancelMerge = useRef<(() => void) | null>(null);

  const updateRow = useCallback((index: number, row: Partial<ExportRow>) => {
    setState((previous) => ({
      ...previous,
      rows: previous.rows.map((existing, position) => (position === index ? { ...existing, ...row } : existing)),
    }));
  }, []);

  /** The shared export parameters, readdressed to one selected item. */
  const paramsFor = useCallback((item: BulkExportItem, shared: ExportParamsJson): ExportParamsJson => {
    const documentType = documentTypeOf(item);
    const params: ExportParamsJson = { ...shared, projectId: item.projectId, documentType };
    if (documentType === 'TEST_RUN') {
      return { ...params, urlQueryParameters: { ...(shared.urlQueryParameters ?? {}), id: item.id ?? '' } };
    }
    if (documentType === 'BASELINE_COLLECTION') {
      return params;
    }
    return { ...params, locationPath: `${item.spaceId ?? ''}/${item.id ?? ''}` };
  }, []);

  const exportItem = useCallback(
    async (item: BulkExportItem, shared: ExportParamsJson) => {
      const documentType = documentTypeOf(item);
      const params = paramsFor(item, shared);

      if (documentType === 'BASELINE_COLLECTION') {
        await convertCollection(remote, {
          projectId: item.projectId ?? '',
          collectionId: item.id ?? '',
          exportPages,
          params,
          toRequestBody,
        });
        return;
      }

      if (documentType === 'TEST_RUN' && params.attachmentsFilter !== null && !params.embedAttachments) {
        // Attachments are downloaded next to the PDF unless they are embedded into it
        void downloadAttachments(remote, {
          projectId: item.projectId ?? '',
          testRunId: item.id ?? '',
          revision: params.revision,
          filter: params.attachmentsFilter,
          testCaseFieldId: params.testcaseFieldId,
        });
      }

      const result = await convert(remote, toRequestBody(params));
      const fallbackName = `${item.spaceId ? `${item.spaceId}_` : ''}${item.id ?? ''}.pdf`;
      download(result.blob, result.fileName || fallbackName);
    },
    [convert, convertCollection, download, downloadAttachments, exportPages, paramsFor, remote],
  );

  /**
   * Merges the whole selection into one PDF. Unlike the per-item run this is a single backend job: the
   * documents are prepared here, submitted together, and the one merged file is downloaded once ready.
   * Collections are expanded into their member documents, the way the per-item run expands them too.
   */
  const runMerge = useCallback(
    async (items: BulkExportItem[], shared: ExportParamsJson, mergeFileName: string) => {
      const documents: ExportParamsJson[] = [];

      for (let index = 0; index < items.length; index++) {
        if (stopped.current) {
          return;
        }
        const item = items[index];
        const documentType = documentTypeOf(item);
        updateRow(index, { state: 'in-progress' });

        if (documentType === 'BASELINE_COLLECTION') {
          try {
            const collectionDocs = await listCollection(remote, item.projectId ?? '', item.id ?? '');
            // Live reports are only merged when the widget exports pages, matching the per-item run
            const members = exportPages
              ? collectionDocs
              : collectionDocs.filter((doc) => doc.documentType !== 'LIVE_REPORT');
            for (const member of members) {
              documents.push({
                ...shared,
                projectId: member.projectId,
                locationPath: `${member.spaceId}/${member.documentName}`,
                revision: member.revision,
                documentType: member.documentType,
              });
            }
          } catch {
            updateRow(index, { state: 'error', error: 'Failed to load collection documents' });
            setState((previous) => ({ ...previous, errors: true }));
          }
          continue;
        }

        documents.push(paramsFor(item, shared));
      }

      if (stopped.current) {
        return;
      }
      if (documents.length === 0) {
        setState((previous) => (previous.status === 'interrupted' ? previous : { ...previous, status: 'finished' }));
        return;
      }

      // The backend derives the merge parameters (the file name among them) from the first document
      documents[0] = { ...documents[0], fileName: mergeFileName };

      try {
        const result = await convertMerge(remote, JSON.stringify(documents.map(withoutEmpty)), {
          isInterrupted: () => stopped.current,
          onJobStarted: (jobUrl) => {
            cancelMerge.current = () => void cancel(remote, jobUrl);
            // A Stop pressed before the job url was known has not cancelled anything yet
            if (stopped.current) {
              cancelMerge.current();
            }
          },
        });
        // A result that arrives after the user stopped must not download or rewrite the outcome
        if (stopped.current) {
          return;
        }
        download(result.blob, result.fileName || mergeFileName);
        setState((previous) => ({
          ...previous,
          status: 'finished',
          processed: previous.rows.length,
          rows: previous.rows.map((row) => (row.state === 'in-progress' ? { ...row, state: 'finished' } : row)),
        }));
      } catch (error) {
        // A run stopped by the user keeps its own 'interrupted' outcome rather than reporting a failure
        if (error instanceof ConversionInterrupted || stopped.current) {
          return;
        }
        const message = failureOf(error);
        setState((previous) =>
          previous.status === 'interrupted'
            ? previous
            : {
                ...previous,
                status: 'finished',
                errors: true,
                rows: previous.rows.map((row) =>
                  row.state === 'in-progress' ? { ...row, state: 'error', error: message } : row,
                ),
              },
        );
      }
    },
    [cancel, convertMerge, download, exportPages, listCollection, paramsFor, remote, updateRow],
  );

  /** Runs the export of the given items. Resolves when the run is over, stopped or not. */
  const start = useCallback(
    async (
      items: BulkExportItem[],
      exportParams: ExportParamsJson,
      mergeIntoSinglePdf = false,
      mergeFileName: string | null = DEFAULT_MERGE_FILENAME,
    ) => {
      stopped.current = false;
      merging.current = mergeIntoSinglePdf;
      cancelMerge.current = null;
      setState({
        status: 'in-progress',
        rows: items.map((item) => ({ item, state: 'paused' })),
        processed: 0,
        errors: false,
        merge: mergeIntoSinglePdf,
      });

      if (mergeIntoSinglePdf) {
        await runMerge(items, exportParams, mergeFileName || DEFAULT_MERGE_FILENAME);
        return;
      }

      for (let index = 0; index < items.length; index++) {
        if (stopped.current) {
          return;
        }
        updateRow(index, { state: 'in-progress' });
        try {
          await exportItem(items[index], exportParams);
          updateRow(index, { state: 'finished' });
          setState((previous) => ({ ...previous, processed: previous.processed + 1 }));
        } catch (error) {
          updateRow(index, { state: 'error', error: failureOf(error) });
          setState((previous) => ({ ...previous, errors: true, processed: previous.processed + 1 }));
        }
      }

      setState((previous) => (previous.status === 'interrupted' ? previous : { ...previous, status: 'finished' }));
    },
    [exportItem, runMerge, updateRow],
  );

  /**
   * Stops the run. The per-item run stops after the item currently being converted; a merge run is a
   * single backend job, so it is actively cancelled and its in-progress rows are marked interrupted too.
   */
  const stop = useCallback(() => {
    stopped.current = true;
    if (merging.current && cancelMerge.current) {
      cancelMerge.current();
    }
    const stoppableStates: ExportRowState[] = merging.current ? ['paused', 'in-progress'] : ['paused'];
    setState((previous) => ({
      ...previous,
      status: 'interrupted',
      rows: previous.rows.map((row) => (stoppableStates.includes(row.state) ? { ...row, state: 'interrupted' } : row)),
    }));
  }, []);

  const close = useCallback(() => {
    stopped.current = true;
    setState(CLOSED);
  }, []);

  return { state, start, stop, close };
}
