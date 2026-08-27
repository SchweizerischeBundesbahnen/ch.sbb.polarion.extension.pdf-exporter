import { useCallback, useRef, useState } from 'react';
import {
  ConfigurationButtons,
  ConfigurationsPane,
  type ConfigurationsPaneHandle,
  PageLayout,
  RevisionsTable,
  useConfirm,
} from '@sbb-polarion/react-sbb-polarion';
import { toast } from 'sonner';
import useLocalizationFiles, { type TranslationsMap, saveBlob } from '../services/localization';
import { getScope } from '../services/scope';
import useNamedSettings from '../services/settings';

/** One translation of an English text, as the settings document stores it. */
interface TranslationEntry {
  language: string;
  value: string;
}

/** Content of one named `localization` configuration: English text -> its translations. */
interface LocalizationSettings {
  translations?: Record<string, TranslationEntry[]>;
}

/** The languages the exporter translates work item statuses and severities into, plus the source one. */
const TRANSLATED = ['de', 'fr', 'it'] as const;
type TranslatedLanguage = (typeof TRANSLATED)[number];

const COLUMNS: { key: 'en' | TranslatedLanguage; label: string }[] = [
  { key: 'en', label: 'English' },
  { key: 'de', label: 'German' },
  { key: 'fr', label: 'French' },
  { key: 'it', label: 'Italian' },
];

/** One row of the table: the English text and its three translations, plus a key React can hold on to. */
interface TranslationRow {
  id: number;
  en: string;
  de: string;
  fr: string;
  it: string;
}

const FEATURE = 'localization';

function isTranslated(language: string): language is TranslatedLanguage {
  return (TRANSLATED as readonly string[]).includes(language);
}

/** A copy of the row with one cell replaced. Written out because the key is a union of column names. */
function withCell(row: TranslationRow, key: 'en' | TranslatedLanguage, value: string): TranslationRow {
  const next = { ...row };
  next[key] = value;
  return next;
}

/**
 * PDF Exporter: Localization - the German, French and Italian translations of the work item statuses
 * and severities printed into the PDF, one named configuration at a time.
 *
 * The table is the page: a row per English text, editable in place, with rows added and removed by the
 * administrator. On top of the usual named-settings toolbar each language can be exported as an XLIFF
 * 2.0 file and re-imported after editing it elsewhere - the interchange format the legacy page offered,
 * unchanged. An import only fills the table; nothing is stored until Save.
 */
export default function Localization() {
  const scope = getScope();
  const settings = useNamedSettings<LocalizationSettings>(FEATURE);
  const files = useLocalizationFiles();
  const { confirm, confirmDialog } = useConfirm();
  const paneRef = useRef<ConfigurationsPaneHandle>(null);

  /** Which load is the current one; only the newest writes (see CustomTemplatesPage for why). */
  const latestLoad = useRef(0);
  /** Row keys. Rows have no identity of their own - two empty rows are indistinguishable by content. */
  const nextRowId = useRef(0);

  const [rows, setRows] = useState<TranslationRow[]>([]);
  const [selectedConfig, setSelectedConfig] = useState<string | null>(null);
  const [editingName, setEditingName] = useState(false);
  const [showRevisions, setShowRevisions] = useState(false);
  const [revisionsToken, setRevisionsToken] = useState(0);
  const [loadingError, setLoadingError] = useState(false);

  const emptyRow = useCallback(
    (en = ''): TranslationRow => ({ id: nextRowId.current++, en, de: '', fr: '', it: '' }),
    [],
  );

  const toRows = useCallback(
    (content: LocalizationSettings): TranslationRow[] =>
      Object.entries(content.translations ?? {}).map(([en, entries]) => {
        const row = emptyRow(en);
        for (const entry of entries ?? []) {
          if (isTranslated(entry.language)) {
            row[entry.language] = entry.value ?? '';
          }
        }
        return row;
      }),
    [emptyRow],
  );

  const applyContent = useCallback(
    (content: LocalizationSettings) => {
      latestLoad.current += 1;
      setRows(toRows(content));
      // A load that succeeded after an earlier failure would otherwise keep the banner up over good data.
      setLoadingError(false);
    },
    [toRows],
  );

  /**
   * A new selection invalidates whatever is in flight for the old one - and it has to do so at the
   * moment it is made, not when the new content lands: an upload that returns in between would
   * otherwise merge into the rows of the configuration the administrator has already left, and Save
   * would write them to the new one.
   */
  const handleSelectedChange = useCallback((name: string | null) => {
    latestLoad.current += 1;
    setSelectedConfig(name);
  }, []);

  const setCell = (id: number, key: 'en' | TranslatedLanguage, value: string) =>
    setRows((current) => current.map((row) => (row.id === id ? withCell(row, key, value) : row)));

  const handleSave = async () => {
    if (!selectedConfig) return;
    toast.dismiss();
    // Rows without an English text have no key to store them under, so they are dropped - as they were
    // by the legacy page, which is what lets an administrator leave a half-filled row behind.
    const translations: Record<string, TranslationEntry[]> = {};
    for (const row of rows) {
      const key = row.en.trim();
      if (key) {
        translations[key] = TRANSLATED.map((language) => ({ language, value: row[language].trim() }));
      }
    }
    try {
      await settings.saveContent(selectedConfig, scope, { translations });
      toast.success('Data successfully saved.');
      await paneRef.current?.reloadNames();
      setRevisionsToken((t) => t + 1);
    } catch (e) {
      toast.error((e as Error).message || 'Error occurred during saving the data.');
    }
  };

  const reload = async (revision?: string) => {
    if (!selectedConfig) return;
    const seq = ++latestLoad.current;
    const content = await settings.loadContent(selectedConfig, scope, revision);
    if (seq !== latestLoad.current) return;
    applyContent(content);
  };

  const handleCancel = async () => {
    if (!(await confirm('Are you sure you want to cancel editing and revert all changes made?'))) return;
    toast.dismiss();
    try {
      await reload();
    } catch {
      setLoadingError(true);
    }
  };

  const handleRevertToDefault = async () => {
    if (!(await confirm('Are you sure you want to return the default values?'))) return;
    toast.dismiss();
    const seq = ++latestLoad.current;
    try {
      const content = await settings.loadDefaultContent();
      if (seq !== latestLoad.current) return;
      applyContent(content);
      toast.success('Default values loaded. Save the data to apply them.');
    } catch {
      setLoadingError(true);
    }
  };

  const handleExport = async (language: TranslatedLanguage) => {
    if (!selectedConfig) return;
    toast.dismiss();
    try {
      saveBlob(await files.downloadTranslations(selectedConfig, scope, language), `${language}.xlf`);
    } catch {
      toast.error(`Error downloading translations file for language ${language}.`);
    }
  };

  /**
   * Merges an imported language into the table: known English texts get the imported value, texts the
   * file does not mention lose theirs (the file is the whole language, not a patch), and texts the
   * table does not have yet are appended as new rows.
   */
  const mergeImported = useCallback((language: TranslatedLanguage, imported: TranslationsMap) => {
    setRows((current) => {
      const merged = current.map((row) => withCell(row, language, imported[row.en] ?? ''));
      const known = new Set(current.map((row) => row.en));
      for (const [en, value] of Object.entries(imported)) {
        if (!known.has(en)) {
          merged.push(withCell({ id: nextRowId.current++, en, de: '', fr: '', it: '' }, language, value));
        }
      }
      return merged;
    });
  }, []);

  const handleImport = async (language: TranslatedLanguage, file: File | undefined) => {
    if (!file) return;
    toast.dismiss();
    // An import belongs to the table it was started from. The administrator can select another
    // configuration - or a revision, or the defaults - while the file is still on its way, and merging
    // then would put the translations into a document they were never meant for, which the next Save
    // would persist there. Selecting and loading both bump the sequence, so the merge is dropped.
    const seq = latestLoad.current;
    try {
      const imported = await files.uploadTranslations(file, language, scope);
      if (seq !== latestLoad.current) {
        toast.error(`Translation for language ${language} was discarded: other data was loaded meanwhile.`);
        return;
      }
      mergeImported(language, imported);
      toast.success(
        `Translation for language ${language} successfully uploaded. Don't forget to save the data before leaving.`,
      );
    } catch {
      toast.error(`Error occurred while uploading translation file for language ${language}.`);
    }
  };

  return (
    <PageLayout title="PDF Exporter: Localization">
      <div className="notifications">
        {loadingError && (
          <div className="alert alert-error">
            Error occurred loading the data. Be sure Polarion is started and accessible.
          </div>
        )}
      </div>

      <ConfigurationsPane<LocalizationSettings>
        ref={paneRef}
        scope={scope}
        service={settings}
        cookieKey={`selected-configuration-${FEATURE}`}
        onContentLoaded={applyContent}
        onSelectedChange={handleSelectedChange}
        onEditingNameChange={setEditingName}
      />

      <fieldset className="localization-page" disabled={editingName}>
        <table className="translations-table">
          <thead>
            <tr>
              {COLUMNS.map((column) => (
                <th key={column.key}>{column.label}</th>
              ))}
              <th className="row-actions" aria-label="Actions" />
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.id}>
                {COLUMNS.map((column) => (
                  <td key={column.key}>
                    <input
                      type="text"
                      className={row[column.key].trim() ? 'monospace' : 'monospace empty-value'}
                      aria-label={`${column.label} translation`}
                      data-language={column.key}
                      value={row[column.key]}
                      onChange={(e) => setCell(row.id, column.key, e.target.value)}
                    />
                  </td>
                ))}
                <td className="row-actions">
                  <button
                    type="button"
                    className="toolbar-button"
                    title="Delete this translation"
                    aria-label={`Delete translation of ${row.en || '(empty)'}`}
                    onClick={() => setRows((current) => current.filter((r) => r.id !== row.id))}
                  >
                    <span className="sbb-icon-table-minus" role="img" aria-label="Delete" />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
          {/* The export/import controls and the add button are a row of the same table rather than a
              block below it, so each pair sits under the language it belongs to and the add button
              lines up under the per-row delete buttons. */}
          <tfoot>
            <tr>
              <td />
              {TRANSLATED.map((language) => (
                <td className="language-files" key={language}>
                  <button type="button" className="toolbar-button" onClick={() => void handleExport(language)}>
                    Export
                  </button>
                  <label className="toolbar-button label" htmlFor={`file-${language}`}>
                    Import
                  </label>
                  <input
                    id={`file-${language}`}
                    type="file"
                    accept=".xlf,application/xliff+xml"
                    aria-label={`Import ${language} translations`}
                    onChange={(e) => {
                      const file = e.target.files?.[0];
                      // Re-selecting the same file must fire `change` again, so the input is cleared.
                      e.target.value = '';
                      void handleImport(language, file);
                    }}
                  />
                </td>
              ))}
              <td className="row-actions">
                <button
                  type="button"
                  className="toolbar-button"
                  title="Add a translation"
                  aria-label="Add a translation"
                  onClick={() => setRows((current) => [...current, emptyRow()])}
                >
                  <span className="sbb-icon-table-plus" role="img" aria-label="Add" />
                </button>
              </td>
            </tr>
          </tfoot>
        </table>

        <ConfigurationButtons
          onSave={() => void handleSave()}
          onCancel={() => void handleCancel()}
          onRevertToDefault={() => void handleRevertToDefault()}
          onToggleRevisions={() => setShowRevisions((v) => !v)}
          revisionsShown={showRevisions}
        />

        {showRevisions && selectedConfig && (
          <RevisionsTable
            name={selectedConfig}
            scope={scope}
            reloadToken={revisionsToken}
            loadRevisions={settings.loadRevisions}
            onRevert={(revision) => void reload(revision.name)}
          />
        )}
      </fieldset>

      {/* The same block RSP's own pages render, class names included, so the help reads identically
          wherever it appears. */}
      <div className="quick-help">
        <h2 className="align-left">Quick Help</h2>
        <div className="quick-help-text">
          <h3>How-to configure localization</h3>
          <p>Supported localizations for workitems statuses and severities.</p>
          <p>Supported languages are German, French and Italian.</p>
          <p>
            Localization for each language can be imported in XLIFF 2.0 format{' '}
            <a
              href="https://docs.oasis-open.org/xliff/xliff-core/v2.0/os/xliff-core-v2.0-os.html"
              target="_blank"
              rel="noopener noreferrer"
            >
              XLIFF 2.0 specification
            </a>
          </p>
        </div>
      </div>
      {confirmDialog}
    </PageLayout>
  );
}
