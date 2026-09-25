import type { DocEntry as SharedDocEntry } from '@sbb-polarion/react-sbb-polarion';
import config from './docs.config.json';

/** One documentation article: RSP's entry (feature id, display title, markdown source), with the source
 *  required here - every article of this extension is rendered from a repo-root markdown file. */
export type DocEntry = SharedDocEntry & { source: string };

/** Every article in reading order - fed to the shared DocsProvider (and to adminNav's docNodeForFeature) and
 *  used to build prev/next order. */
export const DOC_ORDER: DocEntry[] = config.items;
