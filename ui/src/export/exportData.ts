import type { SelectOption, SendRequest, SettingName } from '@grigoriev/react-sbb-polarion';
import type { DocumentIdentity } from '../services/exportContext';
import { CHILD_SETTINGS, type ChildNames, NO_CHILD_NAMES, type StylePackageSettings } from '../services/stylePackage';
import type { DocumentType, ExportType } from './documentType';
import { areRolesSelectable, isDocumentLanguageRead, isFileNameOffered } from './documentType';
import { toRequestBody } from './exportParams';

/**
 * Everything an export dialog needs before it can be shown, read over REST.
 *
 * Both dialogs read the same endpoints, and both used to get this data another way: the side panel arrived
 * with it substituted into its markup by `PdfExporterFormExtension`, and the toolbar popup filled its
 * `<select>`s one XHR callback at a time in `ExportPopup.loadFormData`. The reads are shared here; what is
 * not shared is how strict each dialog is about a failure, which is why there are two aggregates -
 * {@link loadPanelData} and {@link loadPopupData}. See each for why.
 */

/** One item the style packages are chosen for, in the shape `/settings/style-package/suitable-names` wants. */
export interface DocIdentifier {
  projectId?: string;
  spaceId?: string;
  /** Never omitted: the server dereferences it without a null check. */
  documentName: string;
}

/**
 * What `/permissions/export` said, including that it did not answer.
 *
 * `unknown` is not `denied`: both keep the export buttons disabled, but only `denied` is something a dialog
 * can tell the user a reason for. See {@link loadExportPermission} for why the failure is not read as granted.
 */
export type ExportPermission = 'granted' | 'denied' | 'unknown';

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

/** The item identifier the style package endpoint wants, from where the page says the item is. */
export const toDocIdentifier = (document: DocumentIdentity): DocIdentifier => ({
  ...(document.projectId ? { projectId: document.projectId } : {}),
  ...(document.spaceId ? { spaceId: document.spaceId } : {}),
  documentName: document.documentName ?? '',
});

/**
 * The style packages offered for the given items, best match first (the server orders them by weight), which
 * is why both dialogs preselect the head of this list.
 *
 * The endpoint takes a list because a bulk export asks for the packages that suit **every** selected item.
 */
export function loadStylePackageNames(sendRequest: SendRequest, identifiers: DocIdentifier[]): Promise<SelectOption[]> {
  return readJson<SettingName[]>(
    sendRequest,
    'POST',
    '/settings/style-package/suitable-names',
    JSON.stringify(identifiers),
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

/** The names one child setting offers in the given scope. */
export function loadSettingNames(sendRequest: SendRequest, setting: string, scope: string): Promise<SelectOption[]> {
  return readJson<SettingName[]>(
    sendRequest,
    'GET',
    `/settings/${setting}/names?scope=${encodeURIComponent(scope)}`,
  ).then(toOptions);
}

/** The work item link roles of a scope. Empty means the roles row is not offered at all. */
export function loadLinkRoles(sendRequest: SendRequest, scope: string): Promise<SelectOption[]> {
  return readJson<string[]>(sendRequest, 'GET', `/link-role-names?scope=${encodeURIComponent(scope)}`).then((roles) =>
    roles.map((role) => ({ id: role, name: role })),
  );
}

/** Whether webhooks are enabled installation-wide; the webhooks row is hidden when they are not. */
export function loadWebhooksEnabled(sendRequest: SendRequest): Promise<boolean> {
  return readJson<{ enabled?: boolean }>(sendRequest, 'GET', '/webhooks/status').then((status) => !!status?.enabled);
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

/**
 * The default file name for an export, as `/export-filename` builds it from the filename template.
 *
 * The request body is the caller's own: the two dialogs describe the item differently and always have, and
 * the endpoint reads whichever fields it needs out of what it is given.
 */
export function loadExportFileName(sendRequest: SendRequest, params: Record<string, unknown>): Promise<string> {
  return readText(sendRequest, 'POST', '/export-filename', toRequestBody(params));
}

/**
 * Whether the current user may export this project at all.
 *
 * Fails closed, the way the DLE toolbar's export button already does: generic's `dle-toolbar-starter.js`
 * documents `permitted !== true (or a non-OK status / error) disables the button (fail-closed)` and
 * pdf-exporter drives it with this very endpoint. Anything but an explicit `true` - a malformed body
 * included - is therefore not a grant, and a read that failed is `unknown` rather than `denied` so the
 * dialog does not claim a reason it does not have.
 */
export function loadExportPermission(sendRequest: SendRequest, projectId: string | null): Promise<ExportPermission> {
  return readJson<{ permitted?: boolean }>(
    sendRequest,
    'GET',
    `/permissions/export?projectId=${encodeURIComponent(projectId ?? '')}`,
  )
    .then((permission): ExportPermission => (permission?.permitted === true ? 'granted' : 'denied'))
    .catch((): ExportPermission => 'unknown');
}

/** The names every child dropdown offers, read in one round. */
async function loadChildNames(sendRequest: SendRequest, scope: string, requireNames: boolean): Promise<ChildNames> {
  const entries = await Promise.all(
    CHILD_SETTINGS.map(async (setting) => {
      const options = await loadSettingNames(sendRequest, setting, scope);
      if (requireNames && options.length === 0) {
        // The popup treated an empty child setting as a broken installation and refused to open rather than
        // offering an empty dropdown - its `loadSettingNames` rejected on a zero count.
        throw new Error(`No '${setting}' configurations in scope '${scope}'`);
      }
      return [setting, options] as const;
    }),
  );
  return { ...NO_CHILD_NAMES, ...Object.fromEntries(entries) } as ChildNames;
}

/** What the document properties side panel needs. */
export interface PanelData {
  stylePackages: SelectOption[];
  childNames: ChildNames;
  roles: SelectOption[];
  fileName: string;
  documentLanguage: string | null;
  webhooksEnabled: boolean;
  exportPermission: ExportPermission;
}

/**
 * Reads the side panel's data in one round.
 *
 * The reads that decide what the panel *looks* like - the option lists and the style packages - are
 * required: without them there is nothing to choose from, so a failure here is reported instead of an
 * empty dropdown. The three that only decide a detail (file name, document language, and whether the user
 * may export) fall back rather than fail: an unreachable `/export-filename` is no reason to withhold a
 * panel whose file name the user can type themselves.
 */
export async function loadPanelData(sendRequest: SendRequest, document: DocumentIdentity): Promise<PanelData> {
  const [stylePackages, childNames, roles] = await Promise.all([
    loadStylePackageNames(sendRequest, [toDocIdentifier(document)]),
    loadChildNames(sendRequest, document.scope, false),
    loadLinkRoles(sendRequest, document.scope).catch(() => []),
  ]);

  const [fileName, documentLanguage, webhooksEnabled, exportPermission] = await Promise.all([
    loadExportFileName(sendRequest, {
      documentType: 'LIVE_DOC',
      projectId: document.projectId,
      locationPath: document.locationPath,
      revision: document.revision,
    }).catch(() => ''),
    loadDocumentLanguage(sendRequest, document).catch(() => null),
    loadWebhooksEnabled(sendRequest).catch(() => false),
    loadExportPermission(sendRequest, document.projectId ?? null),
  ]);

  return { stylePackages, childNames, roles, fileName, documentLanguage, webhooksEnabled, exportPermission };
}

/** What the toolbar export popup needs. Its style packages come from one or many items - see {@link PopupDataRequest}. */
export interface PopupData {
  stylePackages: SelectOption[];
  childNames: ChildNames;
  /** Empty where link roles do not apply, in which case they were not read at all. */
  roles: SelectOption[];
  /** Empty for a bulk export, which names each file after its own item. */
  fileName: string;
  documentLanguage: string | null;
  webhooksEnabled: boolean;
}

export interface PopupDataRequest {
  documentType: DocumentType;
  exportType: ExportType;
  /** Where the page says the item is. Carries no location for a bulk export. */
  document: DocumentIdentity;
  /** The items a bulk export was started for; a single export derives its one identifier from `document`. */
  identifiers?: DocIdentifier[];
}

/**
 * Reads the export popup's data in one round.
 *
 * Stricter than {@link loadPanelData} on purpose: the popup showed one "Error occurred loading form data"
 * notification for any failure among these reads, an empty child setting included, and did not enable its
 * Export button. That is kept - the popup is the dialog a user reaches from a toolbar button, where an empty
 * dropdown is indistinguishable from a working one.
 *
 * Three reads are skipped rather than tolerated, exactly as they were: link roles where the document type
 * has none, the document language for reports, test runs and bulk exports, and the file name for a bulk
 * export.
 */
export async function loadPopupData(sendRequest: SendRequest, request: PopupDataRequest): Promise<PopupData> {
  const { documentType, exportType, document } = request;
  const identifiers = request.identifiers ?? [toDocIdentifier(document)];

  // The style packages are read after the rest, as the popup read them: the selected one decides every field
  // below it, so there is nothing to apply it to until the option lists are in.
  const [childNames, roles, documentLanguage, fileName, webhooksEnabled] = await Promise.all([
    loadChildNames(sendRequest, document.scope, true),
    areRolesSelectable(documentType) ? loadLinkRoles(sendRequest, document.scope) : Promise.resolve([]),
    isDocumentLanguageRead(documentType, exportType)
      ? loadDocumentLanguage(sendRequest, document)
      : Promise.resolve(null),
    isFileNameOffered(exportType)
      ? loadExportFileName(sendRequest, {
          documentType,
          projectId: document.projectId,
          locationPath: document.locationPath,
          baselineRevision: document.baselineRevision,
          revision: document.revision,
          urlQueryParameters: document.urlQueryParameters,
        })
      : Promise.resolve(''),
    loadWebhooksEnabled(sendRequest),
  ]);

  const stylePackages = await loadStylePackageNames(sendRequest, identifiers);
  if (stylePackages.length === 0) {
    throw new Error('No style packages are suitable for the selected items');
  }

  return { stylePackages, childNames, roles, fileName, documentLanguage, webhooksEnabled };
}
