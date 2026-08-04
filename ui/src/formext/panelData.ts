import type { SelectOption, SendRequest, SettingName } from '@grigoriev/react-sbb-polarion';
import { CHILD_SETTINGS, type ChildNames, NO_CHILD_NAMES, type StylePackageSettings } from '../services/stylePackage';
import type { DocumentContext } from './exportParams';

/**
 * Everything the export panel needs before it can be shown, read over REST.
 *
 * The panel used to arrive fully populated: `PdfExporterFormExtension` rendered its markup with the style
 * packages, the child setting names, the link roles, the file name and the export permission already
 * substituted into it. The React panel reads the same data from the endpoints the DLE toolbar popup has
 * always read it from, so the server side is a plain fragment again and there is one source of this data
 * rather than two.
 */
export interface PanelData {
  /** The style packages that apply to this document, best match first (the server orders them by weight). */
  stylePackages: SelectOption[];
  /** The names each child dropdown offers. */
  childNames: ChildNames;
  /** The work item link roles of the project. Empty means the roles row is not shown at all. */
  roles: SelectOption[];
  /** The default file name for the document, as `/export-filename` builds it. */
  fileName: string;
  /** The document's `docLanguage` custom field, or null when it has none. */
  documentLanguage: string | null;
  /** Whether webhooks are enabled installation-wide; the webhooks row is hidden when they are not. */
  webhooksEnabled: boolean;
  /** Whether the current user may export this project at all. */
  exportPermitted: boolean;
}

/** Where the document lives, in the shape the endpoints want it. */
export interface DocumentIdentity extends DocumentContext {
  scope: string;
  spaceId?: string;
  documentName?: string;
}

async function readJson<T>(sendRequest: SendRequest, method: string, url: string, body?: string): Promise<T> {
  const response = await sendRequest({
    method,
    url,
    body,
    contentType: body === undefined ? undefined : 'application/json',
  });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return (await response.json()) as T;
}

async function readText(sendRequest: SendRequest, method: string, url: string, body?: string): Promise<string> {
  const response = await sendRequest({
    method,
    url,
    body,
    contentType: body === undefined ? undefined : 'application/json',
  });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }
  return await response.text();
}

const toOptions = (names: SettingName[]): SelectOption[] => names.map((name) => ({ id: name.name, name: name.name }));

/**
 * The style packages offered for this document.
 *
 * The endpoint takes the document identifier the widget's bulk export passes a list of, and answers with
 * the suitable packages in weight order - which is why the panel preselects the first one, exactly as the
 * server-rendered panel did (`getStylePackageNameToSelect` took the head of the same list).
 */
export function loadStylePackageNames(sendRequest: SendRequest, document: DocumentIdentity): Promise<SelectOption[]> {
  const identifier = {
    ...(document.projectId ? { projectId: document.projectId } : {}),
    ...(document.spaceId ? { spaceId: document.spaceId } : {}),
    documentName: document.documentName ?? '',
  };
  return readJson<SettingName[]>(
    sendRequest,
    'POST',
    '/settings/style-package/suitable-names',
    JSON.stringify([identifier]),
  ).then(toOptions);
}

/** The content of one style package. */
export function loadStylePackage(sendRequest: SendRequest, name: string, scope: string): Promise<StylePackageSettings> {
  return readJson<StylePackageSettings>(
    sendRequest,
    'GET',
    `/settings/style-package/names/${encodeURIComponent(name)}/content?scope=${encodeURIComponent(scope)}`,
  );
}

/**
 * Reads the panel's data in one round.
 *
 * The reads that decide what the panel *looks* like - the option lists and the style packages - are
 * required: without them there is nothing to choose from, so a failure here is reported instead of an
 * empty dropdown. The three that only decide a detail (file name, document language, and whether the user
 * may export) fall back rather than fail: an unreachable `/export-filename` is no reason to withhold a
 * panel whose file name the user can type themselves.
 */
export async function loadPanelData(sendRequest: SendRequest, document: DocumentIdentity): Promise<PanelData> {
  const scope = encodeURIComponent(document.scope);

  const [stylePackages, childEntries, roles] = await Promise.all([
    loadStylePackageNames(sendRequest, document),
    Promise.all(
      CHILD_SETTINGS.map(
        async (setting) =>
          [
            setting,
            toOptions(await readJson<SettingName[]>(sendRequest, 'GET', `/settings/${setting}/names?scope=${scope}`)),
          ] as const,
      ),
    ),
    readJson<string[]>(sendRequest, 'GET', `/link-role-names?scope=${scope}`).catch(() => []),
  ]);

  const [fileName, documentLanguage, webhooksEnabled, exportPermitted] = await Promise.all([
    readText(
      sendRequest,
      'POST',
      '/export-filename',
      JSON.stringify({
        documentType: 'LIVE_DOC',
        projectId: document.projectId,
        locationPath: document.locationPath,
        revision: document.revision,
      }),
    ).catch(() => ''),
    loadDocumentLanguage(sendRequest, document).catch(() => null),
    readJson<{ enabled?: boolean }>(sendRequest, 'GET', '/webhooks/status')
      .then((status) => !!status?.enabled)
      .catch(() => false),
    readJson<{ permitted?: boolean }>(
      sendRequest,
      'GET',
      `/permissions/export?projectId=${encodeURIComponent(document.projectId ?? '')}`,
    )
      // A permission that cannot be read is treated as granted: the conversion endpoints enforce it
      // themselves, so the worst case is an export that fails with the server's own message instead of a
      // button that was disabled up front. Locking the panel on an unreachable endpoint would be worse.
      .then((permission) => permission?.permitted !== false)
      .catch(() => true),
  ]);

  return {
    stylePackages,
    childNames: { ...NO_CHILD_NAMES, ...Object.fromEntries(childEntries) } as ChildNames,
    roles: roles.map((role) => ({ id: role, name: role })),
    fileName,
    documentLanguage,
    webhooksEnabled,
    exportPermitted,
  };
}

/** The document's `docLanguage` custom field. Returns null when the field is unset. */
export async function loadDocumentLanguage(
  sendRequest: SendRequest,
  document: DocumentIdentity,
): Promise<string | null> {
  const parameters = new URLSearchParams({
    projectId: document.projectId ?? '',
    spaceId: document.spaceId ?? '',
    documentName: document.documentName ?? '',
  });
  if (document.revision) {
    parameters.set('revision', document.revision);
  }
  const language = await readText(sendRequest, 'GET', `/document-language?${parameters.toString()}`);
  return language || null;
}
