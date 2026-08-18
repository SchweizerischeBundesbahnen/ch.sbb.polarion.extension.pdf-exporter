import { useCallback, useEffect, useRef, useState } from 'react';
import {
  ConfigurationButtons,
  ConfigurationsPane,
  type ConfigurationsPaneHandle,
  PageLayout,
  RevisionsTable,
  SearchableSelect,
  useConfirm,
} from '@sbb-polarion/react-sbb-polarion';
import { toast } from 'sonner';
import { getScope } from '../services/scope';
import useNamedSettings from '../services/settings';
import useRemote from '../services/useRemote';

/** How a webhook endpoint authenticates, as the settings document stores it. */
type AuthType = 'BEARER_TOKEN' | 'BASIC_AUTH';

const AUTH_TYPES = [
  { id: 'BEARER_TOKEN', name: 'Bearer Token' },
  { id: 'BASIC_AUTH', name: 'Basic' },
];

/** One configured webhook. No auth type means the endpoint is called without credentials. */
interface WebhookConfig {
  url?: string;
  authType?: AuthType | null;
  authTokenName?: string | null;
}

/** Content of one named `webhooks` configuration. */
interface WebhooksSettings {
  webhookConfigs?: WebhookConfig[];
}

/** A row of the table: a webhook plus a key React can hold on to (two blank rows look alike). */
interface WebhookRow {
  id: number;
  url: string;
  auth: boolean;
  authType: AuthType;
  authTokenName: string;
}

const FEATURE = 'webhooks';

/** The same shape the legacy page checked its URL field against, without the `g` flag and its lastIndex. */
const URL_PATTERN = /^(http(s)?:\/\/.)[-a-zA-Z0-9@:%._+~#=]{2,256}\b([-a-zA-Z0-9@:%_+.~#?&/=]*)$/;

/**
 * PDF Exporter: Webhooks - the REST endpoints the generated HTML is passed through before it is
 * rendered, one named configuration at a time.
 *
 * A row per webhook: the URL, and optionally the authentication to use, whose credentials are not held
 * here but in the Polarion Vault - this page only names the vault entry. The whole page is behind the
 * `webhooks.enabled` extension property, so it first asks the backend whether the feature is on at all
 * and otherwise shows the same note the JSP page did.
 */
export default function Webhooks() {
  const scope = getScope();
  const settings = useNamedSettings<WebhooksSettings>(FEATURE);
  const { sendRequest } = useRemote();
  const { confirm, confirmDialog } = useConfirm();
  const paneRef = useRef<ConfigurationsPaneHandle>(null);

  /** Which load is the current one; only the newest writes (see CustomTemplatesPage for why). */
  const latestLoad = useRef(0);
  const nextRowId = useRef(0);

  const [enabled, setEnabled] = useState<boolean | null>(null);
  const [statusError, setStatusError] = useState(false);
  const [rows, setRows] = useState<WebhookRow[]>([]);
  const [selectedConfig, setSelectedConfig] = useState<string | null>(null);
  const [editingName, setEditingName] = useState(false);
  const [showRevisions, setShowRevisions] = useState(false);
  const [revisionsToken, setRevisionsToken] = useState(0);
  const [loadingError, setLoadingError] = useState(false);

  // Whether webhooks are enabled at all is a property of the installation, not of the configuration.
  // A read that fails leaves `enabled` unknown and says so: answering "not enabled" to a network blip
  // would tell the administrator the feature is off, which is not something this page can know.
  useEffect(() => {
    let cancelled = false;
    sendRequest({ method: 'GET', url: '/webhooks/status' })
      .then((response) => {
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json() as Promise<{ enabled?: boolean }>;
      })
      .then((status) => {
        if (cancelled) return;
        setEnabled(!!status?.enabled);
        setStatusError(false);
      })
      .catch(() => {
        if (!cancelled) setStatusError(true);
      });
    return () => {
      cancelled = true;
    };
  }, [sendRequest]);

  const applyContent = useCallback((content: WebhooksSettings) => {
    latestLoad.current += 1;
    setRows(
      (content.webhookConfigs ?? []).map((config) => ({
        id: nextRowId.current++,
        url: config.url ?? '',
        auth: !!config.authType,
        authType: config.authType ?? 'BEARER_TOKEN',
        authTokenName: config.authTokenName ?? '',
      })),
    );
    // A load that succeeded after an earlier failure would otherwise keep the banner up over good data.
    setLoadingError(false);
  }, []);

  const patchRow = (id: number, patch: Partial<WebhookRow>) =>
    setRows((current) => current.map((row) => (row.id === id ? { ...row, ...patch } : row)));

  const handleSave = async () => {
    if (!selectedConfig) return;
    toast.dismiss();
    const webhookConfigs: WebhookConfig[] = rows.map((row) => ({
      url: row.url,
      // Unchecking "Auth" drops the credentials from the stored document rather than keeping values
      // nothing reads, exactly as the legacy page did.
      authType: row.auth ? row.authType : null,
      authTokenName: row.auth ? row.authTokenName : null,
    }));
    try {
      await settings.saveContent(selectedConfig, scope, { webhookConfigs });
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
    if (!(await confirm('Are you sure you want to return the default value?'))) return;
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

  if (enabled === false) {
    return (
      <PageLayout title="PDF Exporter: Webhooks">
        <p className="webhooks-disabled">
          Webhooks are not enabled. Please contact system administrator if this functionality should be available.
        </p>
      </PageLayout>
    );
  }

  return (
    <PageLayout title="PDF Exporter: Webhooks">
      <div className="notifications">
        {statusError && (
          <div className="alert alert-error">
            Error occurred reading whether webhooks are enabled. Be sure Polarion is started and accessible.
          </div>
        )}
        {loadingError && (
          <div className="alert alert-error">
            Error occurred loading the data. Be sure Polarion is started and accessible.
          </div>
        )}
      </div>

      {/* Rendered once the status is known to be on, so the pane does not load a configuration into a
          page that turns out to be unavailable - or one whose availability could not be read. */}
      {enabled && (
        <>
          <ConfigurationsPane<WebhooksSettings>
            ref={paneRef}
            scope={scope}
            service={settings}
            cookieKey={`selected-configuration-${FEATURE}`}
            onContentLoaded={applyContent}
            onSelectedChange={setSelectedConfig}
            onEditingNameChange={setEditingName}
          />

          <fieldset className="webhooks-page" disabled={editingName}>
            <h2 className="align-left">List of webhooks</h2>

            <table className="webhooks-table">
              <tbody>
                {rows.map((row) => (
                  <tr key={row.id} className="webhook-row">
                    <td className="row-actions">
                      <button
                        type="button"
                        className="toolbar-button"
                        title="Delete this webhook"
                        aria-label={`Delete webhook ${row.url || '(no URL)'}`}
                        onClick={() => setRows((current) => current.filter((r) => r.id !== row.id))}
                      >
                        <span className="sbb-icon-table-minus" role="img" aria-label="Delete" />
                      </button>
                    </td>
                    <td>
                      <label htmlFor={`webhook-url-${row.id}`}>URL:</label>
                    </td>
                    <td className="webhook-url">
                      <input
                        id={`webhook-url-${row.id}`}
                        type="text"
                        className="fs-14"
                        placeholder="https://my.domain.com/my-webhook"
                        value={row.url}
                        onChange={(e) => patchRow(row.id, { url: e.target.value })}
                      />
                      {row.url && !URL_PATTERN.test(row.url) && (
                        <div className="invalid-webhook">
                          WARNING: Entered value doesn&apos;t seem to be a valid URL
                        </div>
                      )}
                    </td>
                    <td>
                      <label htmlFor={`webhook-auth-${row.id}`}>
                        <input
                          id={`webhook-auth-${row.id}`}
                          type="checkbox"
                          checked={row.auth}
                          onChange={(e) => patchRow(row.id, { auth: e.target.checked })}
                        />
                        Auth
                      </label>
                    </td>
                    <td className="webhook-auth-type">
                      {row.auth && (
                        <SearchableSelect
                          id={`webhook-auth-type-${row.id}`}
                          options={AUTH_TYPES}
                          value={row.authType}
                          onChange={(value) => patchRow(row.id, { authType: value as AuthType })}
                        />
                      )}
                    </td>
                    <td>
                      {row.auth && (
                        <input
                          type="text"
                          aria-label="Polarion Vault entry name"
                          placeholder="Polarion Vault entry name"
                          value={row.authTokenName}
                          onChange={(e) => patchRow(row.id, { authTokenName: e.target.value })}
                        />
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>

            <button
              type="button"
              className="toolbar-button add-webhook"
              title="Add a webhook"
              aria-label="Add a webhook"
              onClick={() =>
                setRows((current) => [
                  ...current,
                  { id: nextRowId.current++, url: '', auth: false, authType: 'BEARER_TOKEN', authTokenName: '' },
                ])
              }
            >
              <span className="sbb-icon-table-plus" role="img" aria-label="Add" />
            </button>

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
        </>
      )}

      {/* The same block RSP's own pages render, class names included, so the help reads identically
          wherever it appears. */}
      <div className="quick-help">
        <h2 className="align-left">Quick Help</h2>
        <div className="quick-help-text">
          <p>On this page you can add, edit or remove a webhook applied to selected configuration.</p>
          <h3>What is a webhook</h3>
          <p>
            A webhook is a REST endpoint accepting initial HTML as a string (POST request), making some modification to
            this HTML and returning resulting HTML as a string back in body of response. A webhook endpoint can locate
            anywhere, either within Polarion itself or outside of it.
          </p>
          <h3>Webhook configuration</h3>
          <p>
            Each webhook has an URL and optional auth info. The URL is the endpoint to invoke. The auth info is a
            authentication for this endpoint. The auth info can be either a basic auth with username and password or a
            Bearer token. The auth info should be stored in Polarion Vault. Here should be provided a name of the vault
            entry with auth info.
          </p>
          <h3>Webhooks processing</h3>
          <p>
            Webhooks to run they should be selected in appropriate style package, or during PDF exporting. They are
            invoked in an order they entered on this page. If certain webhook fails with an error, it&apos;s just
            skipped, remaining webhooks will still be invoked.
          </p>
        </div>
      </div>
      {confirmDialog}
    </PageLayout>
  );
}
