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
import { getScope } from '../services/scope';
import useNamedSettings from '../services/settings';

/** Content of one named `css` configuration. */
interface CssSettings {
  css: string;
  disableDefaultCss: boolean;
}

const FEATURE = 'css';

/**
 * PDF Exporter: CSS - the custom stylesheet appended to the generated PDF's, one named configuration
 * at a time.
 *
 * Two tabs, as in the JSP page it replaces: the editable custom stylesheet, and the built-in one shown
 * read-only for reference. The built-in one is fetched once and cached for the lifetime of the page -
 * it is the same document for every configuration and every scope.
 *
 * The toolbar has no Default button: this setting's defaults are what the second tab shows, and the
 * legacy page hid the button for the same reason.
 */
export default function Css() {
  const scope = getScope();
  const settings = useNamedSettings<CssSettings>(FEATURE);
  const { confirm, confirmDialog } = useConfirm();
  const paneRef = useRef<ConfigurationsPaneHandle>(null);

  /** Which load is the current one; only the newest writes (see CustomTemplatesPage for why). */
  const latestLoad = useRef(0);

  const [css, setCss] = useState('');
  const [disableDefaultCss, setDisableDefaultCss] = useState(false);
  const [defaultCss, setDefaultCss] = useState<string | null>(null);
  const [selectedConfig, setSelectedConfig] = useState<string | null>(null);
  const [editingName, setEditingName] = useState(false);
  const [activeTab, setActiveTab] = useState<'custom' | 'default'>('custom');
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
        setDefaultsError(false);
      })
      .catch(() => {
        if (!cancelled) setDefaultsError(true);
      });
    return () => {
      cancelled = true;
    };
  }, [settings]);

  const handleSave = async () => {
    if (!selectedConfig) return;
    toast.dismiss();
    try {
      await settings.saveContent(selectedConfig, scope, { css, disableDefaultCss });
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
        <div className="checkbox input-group">
          <label htmlFor="disable-default-css">
            <input
              id="disable-default-css"
              type="checkbox"
              checked={disableDefaultCss}
              onChange={(e) => setDisableDefaultCss(e.target.checked)}
            />
            Disable usage of default CSS
          </label>
        </div>

        <Tabs
          items={[
            { id: 'custom', label: 'Custom CSS' },
            { id: 'default', label: 'Default CSS' },
          ]}
          activeId={activeTab}
          onSelect={(id) => setActiveTab(id as 'custom' | 'default')}
          name="css-tab"
          ariaLabel="CSS"
        />

        {activeTab === 'custom' ? (
          <div className="tab-panel">
            <p>
              Here you can define your custom CSS, which will be appended to the end of resulting CSS. This means that
              you can add additional styling to default one or even overwrite it. Also be aware that if default CSS is
              disabled then your custom CSS is totally responsible for how resulting PDF will look like.
            </p>
            <CodeEditor
              language="css"
              id="custom-css-input"
              className="css-editor"
              value={css}
              onChange={setCss}
              placeholder="Enter your custom CSS here"
            />
          </div>
        ) : (
          <div className="tab-panel">
            <p>
              This is a default CSS, which covers most common cases to generate well looking PDF from Polarion
              documents, reports etc. It&apos;s not editable and shown here only for your information. If you need to
              customize something please add this using editor on &quot;Custom CSS&quot; tab. Also be aware that you can
              totally disable default CSS clicking checkbox above.
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
      {confirmDialog}
    </PageLayout>
  );
}
