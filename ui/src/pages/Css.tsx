import { useCallback, useEffect, useRef, useState } from 'react';
import {
  CodeEditor,
  ConfigurationButtons,
  ConfigurationsPane,
  type ConfigurationsPaneHandle,
  PageLayout,
  RevisionsTable,
  Tabs,
  useConfirm,
} from '@sbb-polarion/react-sbb-polarion';
import { toast } from 'sonner';
import CompareWithDefault from '../components/CompareWithDefault';
import { getScope } from '../services/scope';
import useNamedSettings from '../services/settings';

/** Content of one named `css` configuration. */
interface CssSettings {
  css: string;
  disableDefaultCss: boolean;
  /** The hash of the default CSS the custom CSS was copied from, when it was. */
  defaultHash?: string;
  /** Whether the default CSS changed since, computed by the extension on reading. */
  defaultChanged?: boolean;
}

type CssTab = 'custom' | 'default';

const FEATURE = 'css';
const COMPARED_FIELDS = [{ key: 'css', label: 'CSS:' }];
/** A new configuration has no custom CSS: the default CSS alone applies until the administrator adds some. */
const INITIAL_CONTENT: CssSettings = { css: '', disableDefaultCss: false };

/**
 * PDF Exporter: CSS - the stylesheet of the generated PDF's, one named configuration at a time.
 *
 * The default CSS comes with the extension, so a newer version of it reaches every export by itself, and the
 * custom CSS of the configuration follows it and wins where both style the same thing. Or the custom CSS is the
 * only CSS: then it usually starts as a copy of the default CSS, which the page offers when that choice is made,
 * and the page says when a newer default CSS changed since.
 *
 * Two tabs: the editable custom stylesheet and the built-in one read-only for reference. The built-in one is fetched once and cached for the lifetime of the page - it is the
 * same document for every configuration and every scope.
 *
 * The toolbar has no Default button: this setting's defaults are what the second tab shows, and the
 * legacy page hid the button for the same reason.
 */
export default function Css() {
  const scope = getScope();
  const settings = useNamedSettings<CssSettings>(FEATURE, INITIAL_CONTENT);
  const { confirm, confirmDialog } = useConfirm();
  const paneRef = useRef<ConfigurationsPaneHandle>(null);

  /** Which load is the current one; only the newest writes (see CustomTemplatesPage for why). */
  const latestLoad = useRef(0);

  const [css, setCss] = useState('');
  const [disableDefaultCss, setDisableDefaultCss] = useState(false);
  const [defaultHash, setDefaultHash] = useState<string | undefined>(undefined);
  const [defaultChanged, setDefaultChanged] = useState(false);
  const [defaultCss, setDefaultCss] = useState<string | null>(null);
  const [defaultCssHash, setDefaultCssHash] = useState<string | undefined>(undefined);
  const [comparing, setComparing] = useState(false);
  const [selectedConfig, setSelectedConfig] = useState<string | null>(null);
  const [editingName, setEditingName] = useState(false);
  const [activeTab, setActiveTab] = useState<CssTab>('custom');
  const [showRevisions, setShowRevisions] = useState(false);
  const [revisionsToken, setRevisionsToken] = useState(0);
  // Two independent reads feed this page - the built-in values and the selected configuration - and a
  // banner that says "could not read the data" must not be taken down by whichever of them succeeded
  // last. Each owns its flag; the banner is their union.
  const [defaultsError, setDefaultsError] = useState(false);
  const [contentError, setContentError] = useState(false);
  const loadingError = defaultsError || contentError;

  const applyContent = useCallback((content: CssSettings) => {
    latestLoad.current += 1;
    setCss(content.css ?? '');
    setDisableDefaultCss(!!content.disableDefaultCss);
    setDefaultHash(content.defaultHash);
    setDefaultChanged(!!content.defaultChanged);
    // A load that succeeded after an earlier failure would otherwise keep the banner up over good data.
    setContentError(false);
  }, []);

  // The built-in stylesheet never changes; load it once, when the page opens.
  useEffect(() => {
    let cancelled = false;
    settings
      .loadDefaultContent()
      .then((content) => {
        if (cancelled) return;
        setDefaultCss(content.css ?? '');
        setDefaultCssHash(content.defaultHash);
        setDefaultsError(false);
      })
      .catch(() => {
        if (!cancelled) setDefaultsError(true);
      });
    return () => {
      cancelled = true;
    };
  }, [settings]);

  /** Puts the default CSS in front of the custom CSS, which keeps what the administrator wrote. */
  const insertDefaultCss = (current: string) => {
    const builtIn = defaultCss ?? '';
    setCss(current.trim() === '' ? builtIn : `${builtIn}\n\n${current}`);
    setDefaultHash(defaultCssHash);
    setDefaultChanged(false);
  };

  /** Takes the current default CSS as the one the custom CSS is up to date with. */
  const markReviewed = () => {
    setDefaultHash(defaultCssHash);
    setDefaultChanged(false);
    toast.success('Marked as reviewed. Remember to save the configuration.');
  };

  const chooseCustomCssOnly = async () => {
    setDisableDefaultCss(true);
    const current = css;
    if (
      defaultCss &&
      !current.includes(defaultCss.trim()) &&
      (await confirm(
        'The custom CSS becomes the only CSS of the export. Do you want to put the default CSS in front of it, to keep the default styling?',
      ))
    ) {
      insertDefaultCss(current);
    }
  };

  const handleSave = async () => {
    if (!selectedConfig) return;
    toast.dismiss();
    const content: CssSettings = { css, disableDefaultCss };
    if (defaultHash) content.defaultHash = defaultHash;
    try {
      await settings.saveContent(selectedConfig, scope, content);
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
      setContentError(true);
    }
  };

  return (
    <PageLayout title="PDF Exporter: CSS">
      <div className="notifications">
        {loadingError && (
          <div className="alert alert-error">
            Error occurred loading the data. Be sure Polarion is started and accessible.
          </div>
        )}
      </div>

      <ConfigurationsPane<CssSettings>
        ref={paneRef}
        scope={scope}
        service={settings}
        cookieKey={`selected-configuration-${FEATURE}`}
        onContentLoaded={applyContent}
        onSelectedChange={setSelectedConfig}
        onEditingNameChange={setEditingName}
      />

      <fieldset className="css-page" disabled={editingName}>
        <div className="mode-options input-group" role="radiogroup" aria-label="CSS">
          <label htmlFor="use-default-css">
            <input
              id="use-default-css"
              type="radio"
              name="css-mode"
              checked={!disableDefaultCss}
              onChange={() => setDisableDefaultCss(false)}
            />
            Use default CSS and custom CSS
          </label>
          <label htmlFor="disable-default-css">
            <input
              id="disable-default-css"
              type="radio"
              name="css-mode"
              checked={disableDefaultCss}
              onChange={() => void chooseCustomCssOnly()}
            />
            Use custom CSS only
          </label>
        </div>

        {disableDefaultCss && defaultChanged && (
          <div className="alert alert-warning default-changed">
            The default CSS changed since it was copied into the custom CSS. Compare them to take over what you need,
            then mark the change as reviewed.
            <button type="button" className="sbb-btn sbb-btn--control mark-as-reviewed" onClick={markReviewed}>
              <span>Mark as reviewed</span>
            </button>
          </div>
        )}

        <Tabs
          items={[
            { id: 'custom', label: 'Custom CSS' },
            { id: 'default', label: 'Default CSS' },
          ]}
          activeId={activeTab}
          onSelect={(id) => setActiveTab(id as CssTab)}
          name="css-tab"
          ariaLabel="CSS"
        />

        {activeTab === 'custom' && (
          <div className="tab-panel">
            <p>
              Here you can define your custom CSS. With the default CSS it is appended to the end of the default CSS, so
              you can add styling to the default one or overwrite it, and a newer default CSS still reaches your
              exports. With the custom CSS only, your custom CSS is totally responsible for how the resulting PDF looks.
            </p>
            {/* With the default CSS the custom CSS only adds to it: inserting or comparing the default CSS means
                something only once the custom CSS is the only one. */}
            {disableDefaultCss && (
              <div className="template-actions">
                <button
                  type="button"
                  className="sbb-btn sbb-btn--control insert-default-css"
                  onClick={() => insertDefaultCss(css)}
                >
                  <span>Insert default CSS</span>
                </button>
                <button
                  type="button"
                  className="sbb-btn sbb-btn--control compare-with-default-button"
                  onClick={() => setComparing(true)}
                >
                  <span>Compare with default</span>
                </button>
              </div>
            )}
            <CodeEditor
              language="css"
              id="custom-css-input"
              className="css-editor"
              value={css}
              onChange={setCss}
              placeholder="Enter your custom CSS here"
            />
          </div>
        )}
        {activeTab === 'default' && (
          <div className="tab-panel">
            <p>
              This is a default CSS, which covers most common cases to generate well looking PDF from Polarion
              documents, reports etc. It&apos;s not editable and shown here only for your information. If you need to
              customize something please add this using editor on &quot;Custom CSS&quot; tab. Also be aware that you can
              use your custom CSS only, choosing it above.
            </p>
            <CodeEditor
              language="css"
              id="default-css-input"
              className="css-editor"
              value={defaultCss ?? ''}
              onChange={() => {}}
              readOnly
            />
          </div>
        )}

        <ConfigurationButtons
          onSave={() => void handleSave()}
          onCancel={() => void handleCancel()}
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
      <CompareWithDefault
        open={comparing}
        fields={COMPARED_FIELDS}
        custom={{ css }}
        builtIn={{ css: defaultCss ?? '' }}
        onClose={() => setComparing(false)}
      />
      {confirmDialog}
    </PageLayout>
  );
}
