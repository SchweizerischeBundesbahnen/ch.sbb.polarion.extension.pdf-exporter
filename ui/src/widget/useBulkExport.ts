import { useCallback, useRef, useState } from 'react';
import type { ExportParamsJson } from '../export/exportParams';
import { toRequestBody } from '../export/exportParams';
import type { Remote } from '../services/conversion';
import {
  convertCollectionDocuments,
  convertPdf,
  downloadBlob,
  downloadTestRunAttachments,
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
}

const CLOSED: BulkExportState = { status: 'closed', rows: [], processed: 0, errors: false };

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

/** What the run reaches outside itself for, so the tests can watch it instead of converting. */
export interface BulkExportDependencies {
  convert?: typeof convertPdf;
  convertCollection?: typeof convertCollectionDocuments;
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
  const download = deps.download ?? downloadBlob;
  const downloadAttachments = deps.downloadAttachments ?? downloadTestRunAttachments;

  const [state, setState] = useState<BulkExportState>(CLOSED);
  const stopped = useRef(false);

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

  /** Runs the export of the given items. Resolves when the run is over, stopped or not. */
  const start = useCallback(
    async (items: BulkExportItem[], exportParams: ExportParamsJson) => {
      stopped.current = false;
      setState({
        status: 'in-progress',
        rows: items.map((item) => ({ item, state: 'paused' })),
        processed: 0,
        errors: false,
      });

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
    [exportItem, updateRow],
  );

  /** Stops after the item currently being converted: a running conversion cannot be recalled. */
  const stop = useCallback(() => {
    stopped.current = true;
    setState((previous) => ({
      ...previous,
      status: 'interrupted',
      rows: previous.rows.map((row) => (row.state === 'paused' ? { ...row, state: 'interrupted' } : row)),
    }));
  }, []);

  const close = useCallback(() => {
    stopped.current = true;
    setState(CLOSED);
  }, []);

  return { state, start, stop, close };
}
