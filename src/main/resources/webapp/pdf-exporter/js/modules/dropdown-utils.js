import SearchableDropdown from "/polarion/pdf-exporter/ui/generic/js/modules/SearchableDropdown.js";

// Wrap the given native <select>s with the shared Polarion-styled dropdown. `singleIds` are
// single-select; the optional `multiSelectId` is a <select multiple> rendered as a multi-select
// dropdown. Shared by the export side panel and the export popup (their id lists differ).
export function initSearchableDropdowns(ctx, singleIds, multiSelectId) {
    singleIds.forEach(id => {
        const element = ctx.getElementById(id);
        if (element) {
            new SearchableDropdown({element: element, placeholder: '', rememberSelection: false});
        }
    });
    if (multiSelectId) {
        const rolesElement = ctx.getElementById(multiSelectId);
        if (rolesElement) {
            new SearchableDropdown({element: rolesElement, placeholder: '', rememberSelection: false, multiselect: true});
        }
    }
}
