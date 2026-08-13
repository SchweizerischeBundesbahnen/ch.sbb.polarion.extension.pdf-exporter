import { useCallback, useEffect, useMemo, useState } from 'react';
import type { DocIdentifier } from '../export/exportData';
import type { ExportParamsJson } from '../export/exportParams';
import ExportPopupModal from '../popup/ExportPopupModal';
import type { ExportPopupDependencies } from '../popup/ExportPopupModal';
import type { DocumentIdentity } from '../services/exportContext';
import useRemote from '../services/useRemote';
import BulkExportProgressModal from './BulkExportProgressModal';
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

const ITEMS_URL = '/widgets/bulk-export/items';
const LOAD_ERROR = 'Could not load the items of this widget.';

/**
 * The Bulk PDF Export widget: the table of what the widget's data set found, a checkbox per row, and the
 * export button that hands the selection to the export parameters dialog.
 *
 * The rows are asked for over REST rather than rendered into the page, which is what makes the widget
 * testable and previewable outside Polarion. The cells arrive as the HTML Polarion rendered for them, so
 * that fields keep the icons, links and colors they have in every other Polarion table.
 */
export default function BulkExportWidget({ shim, deps = {} }: Props) {
  const { sendRequest, sendAbsoluteRequest } = useRemote();
  const remote = useMemo(() => ({ sendRequest, sendAbsoluteRequest }), [sendRequest, sendAbsoluteRequest]);
  const [data, setData] = useState<BulkExportItems | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [queryShown, setQueryShown] = useState(false);
  /** The selection the export dialog is open for, or null while it is closed. */
  const [exporting, setExporting] = useState<BulkExportItem[] | null>(null);
  const bulk = useBulkExport(shim.exportPages, remote, deps);

  const loadFromRest: LoadItems = useCallback(async () => {
    const response = await sendRequest({
      method: 'POST',
      url: ITEMS_URL,
      contentType: 'application/json',
      body: JSON.stringify({ descriptor: shim.descriptor, signature: shim.signature }),
    });
    const payload = await response.json().catch(() => null);
    if (!response.ok) {
      // The endpoint answers 400 for a descriptor it did not sign, which is what an open page sees
      // after a server restart
      throw new Error(payload?.message ?? payload?.errorMessage ?? LOAD_ERROR);
    }
    return payload as BulkExportItems;
  }, [sendRequest, shim.descriptor, shim.signature]);

  const load = deps.loadItems ?? loadFromRest;

  useEffect(() => {
    let active = true;
    load().then(
      (items) => active && setData(items),
      (failure: Error) => active && setError(failure.message || LOAD_ERROR),
    );
    return () => {
      active = false;
    };
  }, [load]);

  const selectableIndexes = useMemo(
    () => (data?.items ?? []).map((item, index) => ({ item, index })).filter(({ item }) => item.readable),
    [data],
  );
  const selectedItems: BulkExportItem[] = useMemo(
    () => selectableIndexes.filter(({ index }) => selected.has(index)).map(({ item }) => item),
    [selectableIndexes, selected],
  );
  const allSelected = selectableIndexes.length > 0 && selected.size === selectableIndexes.length;

  const toggle = (index: number) =>
    setSelected((previous) => {
      const next = new Set(previous);
      if (!next.delete(index)) {
        next.add(index);
      }
      return next;
    });

  const toggleAll = () => setSelected(allSelected ? new Set() : new Set(selectableIndexes.map(({ index }) => index)));

  const openDialog = useCallback(() => {
    if (selectedItems.length === 0) {
      return;
    }
    setExporting(selectedItems);
  }, [selectedItems]);

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
    (params: ExportParamsJson) => {
      const items = exporting ?? [];
      setExporting(null);
      void bulk.start(items, params);
    },
    [bulk, exporting],
  );

  const columns = data?.columns ?? [];
  const exportDisabled = selectedItems.length === 0;

  return (
    <>
      <div className="header">
        <h3>{shim.title}</h3>
        <span
          className="polarion-TestsExecutionButton-link"
          title={exportDisabled ? 'Please, select at least one item to be exported first' : undefined}
        >
          <a>
            <div
              id="bulk-export-pdf"
              role="button"
              tabIndex={0}
              aria-disabled={exportDisabled}
              className={`polarion-TestsExecutionButton-buttons${
                exportDisabled ? ' polarion-TestsExecutionButton-buttons-defaultCursor' : ''
              }`}
              onClick={openDialog}
              onKeyDown={(event) => (event.key === 'Enter' || event.key === ' ') && openDialog()}
            >
              <table className="polarion-TestsExecutionButton-buttons-content">
                <tbody>
                  <tr>
                    <td className="polarion-TestsExecutionButton-buttons-content-labelCell">
                      <div className="polarion-TestsExecutionButton-labelTextNew">Export to PDF</div>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </a>
        </span>
        <div>
          <p>Please select {shim.title} below which you want to export and click button above</p>
        </div>
      </div>

      {error && <div className="widget-error">{error}</div>}

      {!error && !data && (
        <div className="widget-loading">
          <span className="sbb-spinner" role="img" aria-label="Loading" />
        </div>
      )}

      {data && (
        <div className="export-items">
          <table className="polarion-rpw-table-main">
            <tbody>
              <tr>
                <td>
                  <table className="polarion-rpw-table-content">
                    <tbody>
                      <tr className="polarion-rpw-table-header-row">
                        <th>
                          <input
                            type="checkbox"
                            id="export-all"
                            aria-label="Select all"
                            checked={allSelected}
                            ref={(input) => {
                              if (input) {
                                input.indeterminate = selected.size > 0 && !allSelected;
                              }
                            }}
                            onChange={toggleAll}
                          />
                        </th>
                        {columns.map((column) => (
                          <th key={column.id}>{column.label}</th>
                        ))}
                      </tr>
                      {data.items.map((item, index) =>
                        item.readable ? (
                          <tr className="polarion-rpw-table-content-row" key={`${item.projectId}/${item.id}/${index}`}>
                            <td>
                              <input
                                type="checkbox"
                                className="export-item"
                                aria-label={item.name ?? item.id ?? ''}
                                data-type={item.type ?? undefined}
                                data-project={item.projectId ?? undefined}
                                data-space={item.spaceId ?? undefined}
                                data-id={item.id ?? undefined}
                                checked={selected.has(index)}
                                onChange={() => toggle(index)}
                              />
                            </td>
                            {(item.cells ?? []).map((cell, cellIndex) => (
                              // The cell is the HTML Polarion rendered for that field of that item
                              <td
                                key={columns[cellIndex]?.id ?? cellIndex}
                                dangerouslySetInnerHTML={{ __html: cell }}
                              />
                            ))}
                          </tr>
                        ) : (
                          <tr className="polarion-rpw-table-content-row" key={`unreadable-${index}`}>
                            <td colSpan={columns.length + 1} className="polarion-rpw-table-not-readable-cell">
                              {item.message}
                            </td>
                          </tr>
                        ),
                      )}
                    </tbody>
                  </table>
                </td>
              </tr>
              <tr>
                <td className="polarion-rpw-table-footer">
                  <div className="polarion-rpw-table-counts">
                    {data.openInTableUrl ? (
                      <a href={data.openInTableUrl} target="_top">
                        {data.countMessage}
                      </a>
                    ) : (
                      data.countMessage
                    )}
                  </div>
                  {data.openInTableUrl && (
                    <div className="polarion-rpw-table-open-in-table">
                      <a href={data.openInTableUrl} target="_blank" rel="noreferrer" aria-label="Open in Table">
                        {/* Polarion's own icons, by the URL its tables use. Decorative on purpose: the
                            accessible name sits on the control, and where the icon is not served - the
                            dev harness without a Polarion, the reference screenshots - an empty alt
                            leaves a blank instead of stray words in the middle of the footer. */}
                        <img
                          src="/polarion/ria/images/portlet/portletOpenInTable.png"
                          title="Open in Table"
                          alt=""
                          width={16}
                          height={16}
                        />
                      </a>
                    </div>
                  )}
                  {data.query && (
                    <div className="polarion-rpw-table-show-query">
                      <img
                        src="/polarion/ria/images/portlet/info.png"
                        title="Show Query"
                        alt=""
                        width={16}
                        height={16}
                        role="button"
                        aria-label="Show Query"
                        onClick={() => setQueryShown((shown) => !shown)}
                      />
                    </div>
                  )}
                  {data.query && queryShown && <div className="polarion-rpw-table-query">{data.query}</div>}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      )}

      {exporting && (
        <ExportPopupModal
          document={dialogDocument}
          exportType="BULK"
          identifiers={identifiers}
          onBulkExport={startBulkExport}
          onClose={() => setExporting(null)}
          deps={deps.popup}
        />
      )}

      <BulkExportProgressModal state={bulk.state} onStop={bulk.stop} onClose={bulk.close} />
    </>
  );
}
