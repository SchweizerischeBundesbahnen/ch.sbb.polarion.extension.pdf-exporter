import { useCallback, useMemo } from 'react';
import useRemote from './useRemote';

/** The translations of one language, keyed by the English text - what the import endpoint returns. */
export type TranslationsMap = Record<string, string>;

/**
 * Saves a blob under the given file name, the way a download link would. The exported XLIFF arrives as
 * a response body rather than a navigable URL (the endpoint needs the session headers), so the page
 * has to hand it to the browser itself.
 */
export function saveBlob(blob: Blob, fileName: string): void {
  const objectUrl = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = objectUrl;
  anchor.download = fileName;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(objectUrl);
}

/**
 * The two file endpoints of the localization setting, which are not part of the generic named-settings
 * REST shape `useNamedSettings` covers: XLIFF 2.0 export of one language, and import of such a file.
 *
 * Import does not change the stored setting - the backend only parses the file and returns the
 * translations it found, which the page merges into the table the administrator is editing. Nothing is
 * persisted until Save, exactly as in the JSP page this replaces.
 */
export default function useLocalizationFiles() {
  const { sendRequest } = useRemote();

  const downloadTranslations = useCallback(
    async (name: string, scope: string, language: string): Promise<Blob> => {
      const response = await sendRequest({
        method: 'GET',
        url:
          `/settings/localization/names/${encodeURIComponent(name)}/download` +
          `?language=${encodeURIComponent(language)}&scope=${encodeURIComponent(scope)}`,
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return response.blob();
    },
    [sendRequest],
  );

  const uploadTranslations = useCallback(
    async (file: File, language: string, scope: string): Promise<TranslationsMap> => {
      const body = new FormData();
      body.append('file', file);
      // No explicit content type: the browser has to set multipart/form-data with its own boundary.
      const response = await sendRequest({
        method: 'POST',
        url: `/settings/localization/upload?language=${encodeURIComponent(language)}&scope=${encodeURIComponent(scope)}`,
        body,
      });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      return (await response.json()) as TranslationsMap;
    },
    [sendRequest],
  );

  return useMemo(() => ({ downloadTranslations, uploadTranslations }), [downloadTranslations, uploadTranslations]);
}
