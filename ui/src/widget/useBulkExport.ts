import { useCallback, useRef, useState } from 'react';
import type { ConversionResult, ExportContextLike, ExportParamsLike } from './productModules';
import { createExportContext } from './productModules';
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
const START_ERROR = 'Could not start the export.';

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

async function errorMessageOf(response: Response): Promise<string> {
  try {
    const text = await response.text();
    const error = text ? JSON.parse(text) : null;
    return error?.message ?? error?.errorMessage ?? 'Export failed';
  } catch {
    return 'Export failed';
  }
}

function convert(context: ExportContextLike, exportParams: ExportParamsLike): Promise<ConversionResult> {
  return new Promise((resolve, reject) => {
    context.asyncConvertPdf(exportParams.toJSON(), resolve, (response) => {
      errorMessageOf(response).then(reject, reject);
    });
  });
}

function convertCollection(
  context: ExportContextLike,
  exportParams: ExportParamsLike,
  collectionId: string,
): Promise<void> {
  return new Promise((resolve, reject) => {
    context.convertCollectionDocuments(exportParams, collectionId, resolve, (response) => {
      errorMessageOf(response).then(reject, reject);
    });
  });
}

/**
 * Exports the selected items one after another, the way the widget's vanilla predecessor did: a single
 * conversion at a time, each downloaded as soon as it is ready, and the run stoppable in between.
 *
 * The conversion protocol itself stays in the product's ExportContext, which the DLE toolbar and the
 * document export use as well - only the sequencing and the progress state live here.
 */
export default function useBulkExport(
  exportPages: boolean,
  hostSelector: string,
  createContext: typeof createExportContext = createExportContext,
) {
  const [state, setState] = useState<BulkExportState>(CLOSED);
  const stopped = useRef(false);

  const updateRow = useCallback((index: number, row: Partial<ExportRow>) => {
    setState((previous) => ({
      ...previous,
      rows: previous.rows.map((existing, position) => (position === index ? { ...existing, ...row } : existing)),
    }));
  }, []);

  const exportItem = useCallback(
    async (context: ExportContextLike, item: BulkExportItem, exportParams: ExportParamsLike) => {
      const documentType = documentTypeOf(item);
      exportParams.projectId = item.projectId;
      exportParams.documentType = documentType;

      if (documentType === 'BASELINE_COLLECTION') {
        await convertCollection(context, exportParams, item.id ?? '');
        return;
      }

      if (documentType === 'TEST_RUN') {
        exportParams.urlQueryParameters = { ...(exportParams.urlQueryParameters ?? {}), id: item.id ?? '' };
        // Attachments are downloaded next to the PDF unless they are embedded into it
        if (exportParams.attachmentsFilter !== null && !exportParams.embedAttachments) {
          context.downloadTestRunAttachments(
            item.projectId ?? '',
            item.id ?? '',
            exportParams.revision ?? null,
            exportParams.attachmentsFilter ?? null,
            exportParams.testcaseFieldId,
          );
        }
      } else {
        exportParams.locationPath = `${item.spaceId ?? ''}/${item.id ?? ''}`;
      }

      const result = await convert(context, exportParams);
      const fallbackName = `${item.spaceId ? `${item.spaceId}_` : ''}${item.id ?? ''}.pdf`;
      context.downloadBlob(result.response, result.fileName || fallbackName);
    },
    [],
  );

  /** Runs the export of the given items. Resolves when the run is over, stopped or not. */
  const start = useCallback(
    async (items: BulkExportItem[], exportParams: ExportParamsLike) => {
      stopped.current = false;
      setState({
        status: 'in-progress',
        rows: items.map((item) => ({ item, state: 'paused' })),
        processed: 0,
        errors: false,
      });

      let context: ExportContextLike;
      try {
        context = await createContext({ exportPages, rootComponentSelector: hostSelector });
      } catch {
        // The product's export JS is loaded at runtime and can be gone by the time the user gets here -
        // a redeployed server, an expired session. Without this the run would keep every row paused and
        // report the failure as an interruption once the user closes the dialog. A user who stopped the
        // run while the load was still pending keeps their own outcome, as at the end of the run below.
        setState((previous) =>
          previous.status === 'interrupted'
            ? previous
            : {
                ...previous,
                status: 'finished',
                errors: true,
                rows: previous.rows.map((row) => ({ ...row, state: 'error', error: START_ERROR })),
              },
        );
        return;
      }

      for (let index = 0; index < items.length; index++) {
        if (stopped.current) {
          return;
        }
        updateRow(index, { state: 'in-progress' });
        try {
          await exportItem(context, items[index], exportParams);
          updateRow(index, { state: 'finished' });
          setState((previous) => ({ ...previous, processed: previous.processed + 1 }));
        } catch (error) {
          updateRow(index, { state: 'error', error: typeof error === 'string' ? error : 'Export failed' });
          setState((previous) => ({ ...previous, errors: true, processed: previous.processed + 1 }));
        }
      }

      setState((previous) => (previous.status === 'interrupted' ? previous : { ...previous, status: 'finished' }));
    },
    [createContext, exportItem, exportPages, hostSelector, updateRow],
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
