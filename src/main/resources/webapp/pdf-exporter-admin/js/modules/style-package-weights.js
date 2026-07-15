import ExtensionContext from '../../ui/generic/js/modules/ExtensionContext.js';
import StylePackageWeights from '../../ui/generic/js/modules/StylePackageWeights.js';

const ctx = new ExtensionContext({
    extension: 'pdf-exporter',
    scopeFieldId: 'scope'
});

new StylePackageWeights({ ctx, listId: 'sortable-list' });
