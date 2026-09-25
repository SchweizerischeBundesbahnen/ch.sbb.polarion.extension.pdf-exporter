import { createAdminNav, docNodeForFeature } from '@sbb-polarion/react-sbb-polarion';
import { DOC_ORDER } from '../docs/manifest';

/**
 * The admin node that serves a feature: the documentation site's articles all live under the single
 * `documentation` node; About and the Usage Disclaimer are their own nodes. Everything else opens no node
 * (a settings page no `.html` doc link targets). Built by the shared RSP helper from the doc manifest.
 */
export const nodeForFeature = docNodeForFeature({ docs: DOC_ORDER, selfNodes: ['about', 'disclaimer'] });

// The shared admin-shell node sync, bound to this extension's admin context and feature->node mapping.
// switchToFeatureNode feeds the docs site's onDocLinkNavigate; resumePendingDoc runs before the first render.
const adminNav = createAdminNav({ adminBase: 'pdf-export', nodeForFeature });

export const switchToFeatureNode = adminNav.switchToFeatureNode;
export const resumePendingDoc = adminNav.resumePendingDoc;
