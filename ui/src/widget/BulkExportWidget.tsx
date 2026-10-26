import { useCallback, useEffect, useId, useMemo, useRef, useState } from 'react';
import { BulkExportWidget as BulkExportTable, loadBulkExportItems } from '@sbb-polarion/react-sbb-polarion';
import type { DocIdentifier } from '../export/exportData';
import type { ExportParamsJson } from '../export/exportParams';
import ExportPopupModal from '../popup/ExportPopupModal';
import type { ExportPopupDependencies } from '../popup/ExportPopupModal';
import type { DocumentIdentity } from '../services/exportContext';
import useRemote from '../services/useRemote';
import BulkExportProgressModal from './BulkExportProgressModal';
import { registerBulkExportTarget } from './exportTargets';
import type { BulkExportItem, BulkExportItems, WidgetShim } from './types';
import useBulkExport from './useBulkExport';
import type { BulkExportDependencies } from './useBulkExport';

/** How the widget gets its rows. Rejects with an Error whose message the widget shows. */
export type LoadItems = () => Promise<BulkExportItems>;

/**
 * What the widget reaches outside itself for. Everything here has a working default; the dev harness
 * replaces the REST call, and the tests replace the conversion as well, which needs a running Polarion.
 */
export interface WidgetDependencies extends BulkExportDependencies {
  loadItems?: LoadItems;
  /** What the export dialog reaches outside itself for; the widget only passes it through. */
  popup?: ExportPopupDependencies;
}

interface Props {
  shim: WidgetShim;
  deps?: WidgetDependencies;
}

/**
 * The Bulk PDF Export widget: react-sbb-polarion's bulk export table, whose export button hands the
 * selection to the export parameters dialog, and the progress dialog of the run that follows.
 */
export default function BulkExportWidget({ shim, deps = {} }: Props) {
  const { sendRequest, sendAbsoluteRequest } = useRemote();
  const remote = useMemo(() => ({ sendRequest, sendAbsoluteRequest }), [sendRequest, sendAbsoluteRequest]);
  /** The selection the export dialog is open for, or null while it is closed. */
  const [exporting, setExporting] = useState<BulkExportItem[] | null>(null);
  const [selectedItems, setSelectedItems] = useState<BulkExportItem[]>([]);
  const bulk = useBulkExport(shim.exportPages, remote, deps);

  const injectedLoad = deps.loadItems;
  const loadItems: LoadItems = useCallback(
    () => (injectedLoad ? injectedLoad() : loadBulkExportItems(sendRequest, shim)),
    [injectedLoad, sendRequest, shim],
  );

  const openDialog = useCallback(() => {
    if (selectedItems.length === 0) {
      return;
    }
    setExporting(selectedItems);
  }, [selectedItems]);

  // The report's own "Export to PDF" buttons offer this selection as an alternative to the report (see
  // exportTargets.ts). The entry is registered once and reads the latest selection through refs, so that a
  // click on a checkbox does not replace it.
  const targetId = useId();
  const header = useRef<HTMLDivElement>(null);
  const latest = useRef({ selectedCount: 0, openDialog });
  useEffect(() => {
    latest.current = { selectedCount: selectedItems.length, openDialog };
  }, [selectedItems, openDialog]);
  useEffect(
    () =>
      registerBulkExportTarget({
        id: targetId,
        title: shim.title,
        // The shadow host in Polarion, which is what the page holds and removes; the widget itself where it
        // is rendered without one
        anchor: () => {
          const root = header.current?.getRootNode();
          return root instanceof ShadowRoot ? root.host : header.current;
        },
        selectedCount: () => latest.current.selectedCount,
        startExport: () => latest.current.openDialog(),
      }),
    [targetId, shim.title],
  );

  /**
   * The items the style packages have to suit: a bulk export is only offered the packages that apply to
   * every one of them, which is what the endpoint takes this list for.
   */
  const identifiers: DocIdentifier[] = useMemo(
    () =>
      (exporting ?? []).map((item) => ({
        ...(item.projectId ? { projectId: item.projectId } : {}),
        ...(item.spaceId ? { spaceId: item.spaceId } : {}),
        documentName: item.id ?? '',
      })),
    [exporting],
  );

  /**
   * Where the dialog reads its scope from. A bulk export has no page location of its own - each item is
   * readdressed as it is converted - so this carries the project and nothing else.
   */
  const dialogDocument: DocumentIdentity = useMemo(
    () => ({
      documentType: shim.documentType,
      projectId: identifiers[0]?.projectId ?? null,
      scope: identifiers[0]?.projectId ? `project/${identifiers[0].projectId}/` : '',
    }),
    [identifiers, shim.documentType],
  );

  const startBulkExport = useCallback(
    (params: ExportParamsJson, mergeIntoSinglePdf?: boolean, mergeFileName?: string | null) => {
      const items = exporting ?? [];
      setExporting(null);
      void bulk.start(items, params, mergeIntoSinglePdf, mergeFileName);
    },
    [bulk, exporting],
  );

  return (
    <BulkExportTable
      title={shim.title}
      loadItems={loadItems}
      exportLabel="Export to PDF"
      exportButtonId="bulk-export-pdf"
      onExport={setExporting}
      onSelectionChange={setSelectedItems}
      headerRef={header}
    >
      {exporting && (
        /* `form-wrapper` is what react-sbb-polarion scopes its control system to - the text fields, the
           colour picker, the checkboxes and their focus and hover states. The widget's own container
           deliberately carries no such class: its markup is Polarion's own table and buttons, which that
           system would restyle (see widget/main.tsx). The dialog, though, has to look exactly like the one a
           toolbar button opens, so it gets the class here, around itself alone. */
        <div className="form-wrapper">
          <ExportPopupModal
            document={dialogDocument}
            exportType="BULK"
            identifiers={identifiers}
            onBulkExport={startBulkExport}
            onClose={() => setExporting(null)}
            deps={deps.popup}
          />
        </div>
      )}

      <BulkExportProgressModal state={bulk.state} onStop={bulk.stop} onClose={bulk.close} />
    </BulkExportTable>
  );
}
