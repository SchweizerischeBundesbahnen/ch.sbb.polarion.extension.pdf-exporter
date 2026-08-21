import { useCallback, useMemo } from 'react';
import type { Revision, SettingName } from '@sbb-polarion/react-sbb-polarion';
import useRemote from './useRemote';

/** Extracts a readable message from a failed response, the way the legacy ExtensionContext did. */
async function errorMessage(response: Response): Promise<string> {
  const text = await response.text().catch(() => '');
  if (text) {
    try {
      const parsed = JSON.parse(text) as { message?: string; errorMessage?: string };
      if (parsed?.message) return parsed.message;
      if (parsed?.errorMessage) return parsed.errorMessage;
    } catch {
      return text;
    }
  }
  return `HTTP ${response.status}`;
}

async function jsonOrThrow<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new Error(await errorMessage(response));
  }
  return (await response.json()) as T;
}

async function okOrThrow(response: Response): Promise<void> {
  if (!response.ok) {
    throw new Error(await errorMessage(response));
  }
}

/**
 * The generic named-settings endpoints, for one feature and one content type.
 *
 * Every settings page of this extension speaks the same REST shape - `settings/<feature>/names`,
 * `.../names/<name>/content`, `.../default-content`, `.../names/<name>/revisions` - and differs only
 * in the feature id and in what the content document looks like. So this is one hook parameterised by
 * both, rather than a service per page: `useNamedSettings<CssSettings>('css')`.
 *
 * The calls satisfy RSP's `ConfigurationsService<T>` structurally, so the returned object can be
 * handed to `ConfigurationsPane` as it is.
 */
export default function useNamedSettings<T>(feature: string) {
  const { sendRequest } = useRemote();

  const path = useCallback((suffix: string): string => `/settings/${feature}${suffix}`, [feature]);

  const loadConfigurationNames = useCallback(
    (scope: string): Promise<SettingName[]> =>
      sendRequest({ method: 'GET', url: path(`/names?scope=${encodeURIComponent(scope)}`) }).then((r) =>
        jsonOrThrow<SettingName[]>(r),
      ),
    [sendRequest, path],
  );

  const loadContent = useCallback(
    (name: string, scope: string, revision?: string): Promise<T> => {
      let url = path(`/names/${encodeURIComponent(name)}/content?scope=${encodeURIComponent(scope)}`);
      if (revision) {
        url += `&revision=${encodeURIComponent(revision)}`;
      }
      return sendRequest({ method: 'GET', url }).then((r) => jsonOrThrow<T>(r));
    },
    [sendRequest, path],
  );

  const saveContent = useCallback(
    (name: string, scope: string, content: T): Promise<void> =>
      sendRequest({
        method: 'PUT',
        url: path(`/names/${encodeURIComponent(name)}/content?scope=${encodeURIComponent(scope)}`),
        contentType: 'application/json',
        body: JSON.stringify(content),
      }).then(okOrThrow),
    [sendRequest, path],
  );

  /** The built-in values, shown on the "Default" tab of the editor pages. Not scope-dependent. */
  const loadDefaultContent = useCallback(
    (): Promise<T> => sendRequest({ method: 'GET', url: path('/default-content') }).then((r) => jsonOrThrow<T>(r)),
    [sendRequest, path],
  );

  const loadRevisions = useCallback(
    (name: string, scope: string): Promise<Revision[]> =>
      sendRequest({
        method: 'GET',
        url: path(`/names/${encodeURIComponent(name)}/revisions?scope=${encodeURIComponent(scope)}`),
      }).then((r) => jsonOrThrow<Revision[]>(r)),
    [sendRequest, path],
  );

  /** Creates a named configuration: an empty content PUT, which makes the backend seed the defaults. */
  const createConfiguration = useCallback(
    (name: string, scope: string): Promise<void> =>
      sendRequest({
        method: 'PUT',
        url: path(`/names/${encodeURIComponent(name)}/content?scope=${encodeURIComponent(scope)}`),
        contentType: 'application/json',
      }).then(okOrThrow),
    [sendRequest, path],
  );

  /** Renames it: POST to the current name, the new one as the body. */
  const renameConfiguration = useCallback(
    (name: string, scope: string, newName: string): Promise<void> =>
      sendRequest({
        method: 'POST',
        url: path(`/names/${encodeURIComponent(name)}?scope=${encodeURIComponent(scope)}`),
        contentType: 'application/json',
        body: newName,
      }).then(okOrThrow),
    [sendRequest, path],
  );

  const deleteConfiguration = useCallback(
    (name: string, scope: string): Promise<void> =>
      sendRequest({
        method: 'DELETE',
        url: path(`/names/${encodeURIComponent(name)}?scope=${encodeURIComponent(scope)}`),
      }).then(okOrThrow),
    [sendRequest, path],
  );

  return useMemo(
    () => ({
      loadConfigurationNames,
      loadContent,
      saveContent,
      loadDefaultContent,
      loadRevisions,
      createConfiguration,
      renameConfiguration,
      deleteConfiguration,
    }),
    [
      loadConfigurationNames,
      loadContent,
      saveContent,
      loadDefaultContent,
      loadRevisions,
      createConfiguration,
      renameConfiguration,
      deleteConfiguration,
    ],
  );
}
