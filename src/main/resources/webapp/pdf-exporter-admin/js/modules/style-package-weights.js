import ExtensionContext from '../../ui/generic/js/modules/ExtensionContext.js';
import { enhanceNumericInput } from '../../ui/generic/js/modules/NumericSpinner.js';
import StylePackageUtils from './style-package-utils.js';

const ctx = new ExtensionContext({
    extension: 'pdf-exporter',
    scopeFieldId: 'scope'
});

ctx.getElementById("default-toolbar-button").style.display = "none";
ctx.getElementById("revisions-toolbar-button").style.display = "none";

// Six-dot drag handle for movable rows.
const HANDLE_SVG = '<svg width="10" height="16" viewBox="0 0 10 16" fill="currentColor" aria-hidden="true">'
    + '<circle cx="2" cy="3" r="1.4"/><circle cx="8" cy="3" r="1.4"/><circle cx="2" cy="8" r="1.4"/>'
    + '<circle cx="8" cy="8" r="1.4"/><circle cx="2" cy="13" r="1.4"/><circle cx="8" cy="13" r="1.4"/></svg>';
// Lock marker for read-only global rows (their order is defined at the global level).
const LOCK_SVG = '<svg width="13" height="14" viewBox="0 0 14 16" fill="none" stroke="currentColor" stroke-width="1.3" aria-hidden="true">'
    + '<rect x="2.2" y="7" width="9.6" height="7" rx="1.2" fill="currentColor" stroke="none"/>'
    + '<path d="M4.3 7V4.8a2.7 2.7 0 0 1 5.4 0V7"/></svg>';
const CARET_UP_SVG = '<svg viewBox="0 0 8 5" fill="currentColor" aria-hidden="true"><path d="M4 0l4 5H0z"/></svg>';
const CARET_DOWN_SVG = '<svg viewBox="0 0 8 5" fill="currentColor" aria-hidden="true"><path d="M0 0h8L4 5z"/></svg>';

const GLOBAL_LOCK_TITLE = 'Global scope — defined at the global level and cannot be reordered here';

const StylePackageWeights = {

    ID_PREFIX: 'input.weight.',

    init: function (listId) {
        this.name = 'StylePackageWeights';
        this.sortableList = ctx.getElementById(listId);
        this.items = [];
        this.dragIndex = null;
    },

    loadPackageList: function () {
        ctx.callAsync({
            method: 'GET',
            url: `/polarion/${ctx.extension}/rest/internal/settings/style-package/weights?scope=${ctx.scope}`,
            contentType: 'application/json',
            onOk: (responseText) => {
                this.setData(responseText);
            },
            onError: () => ctx.setLoadingErrorNotificationVisible(true)
        });
    },

    saveWeights: function () {
        const result = this.items
            .filter(item => !item.static)
            .map(item => ({ name: item.name, scope: ctx.scope, weight: item.weight }));

        ctx.callAsync({
            method: 'POST',
            url: `/polarion/${ctx.extension}/rest/internal/settings/style-package/weights`,
            contentType: 'application/json',
            body: JSON.stringify(result),
            onOk: () => {
                ctx.showSaveSuccessAlert();
            },
            onError: () => ctx.showSaveErrorAlert()
        });
    },

    setData: function (jsonString) {
        // A global-scoped entry shown in a non-global scope is read-only here: it is managed at the
        // global level and only serves as a fixed reference point in the ordering.
        this.items = JSON.parse(jsonString).map(item => {
            const isStatic = item.scope === "" && ctx.scope !== "";
            return {
                name: item.name,
                scope: item.scope,
                weight: item.weight,
                originalWeight: item.weight, // server snapshot, kept to preserve weight when it still fits
                static: isStatic
            };
        });
        this.render();
    },

    sortItems: function () {
        // Higher weight first; ties resolved alphabetically to keep a stable, predictable order.
        this.items.sort((a, b) => (b.weight - a.weight) || a.name.localeCompare(b.name));
    },

    render: function () {
        this.sortItems();
        this.sortableList.innerHTML = '';

        this.items.forEach((item, index) => {
            const li = document.createElement("li");
            li.classList.add("weight-item");
            if (item.static) {
                li.classList.add("static");
            }

            // Left slot: lock (read-only global) or drag handle.
            const marker = document.createElement("span");
            if (item.static) {
                marker.className = "lock-marker";
                marker.title = GLOBAL_LOCK_TITLE;
                marker.innerHTML = LOCK_SVG;
            } else {
                marker.className = "drag-handle";
                marker.title = "Drag to reorder";
                marker.innerHTML = HANDLE_SVG;
            }
            li.appendChild(marker);

            const name = document.createElement("span");
            name.className = "name";
            name.textContent = item.name;
            li.appendChild(name);

            // Weight input, wrapped by the shared 2606 caret spinner.
            const input = document.createElement("input");
            input.id = `${this.ID_PREFIX}${item.name}`;
            input.type = "number";
            input.className = "weight-input";
            input.min = "0";
            input.max = "100";
            input.step = "0.1";
            input.value = item.weight;
            if (item.static) {
                input.readOnly = true;
            } else {
                input.addEventListener("change", () => this.commitWeight(item, input));
                input.addEventListener("keydown", (event) => {
                    if (event.key === "Enter") {
                        this.commitWeight(item, input);
                    }
                });
            }
            li.appendChild(input);
            enhanceNumericInput(input);

            li.appendChild(this.buildArrows(item, index));

            this.wireDrag(li, index, item);
            this.sortableList.appendChild(li);
        });
    },

    buildArrows: function (item, index) {
        if (item.static) {
            const placeholder = document.createElement("span");
            placeholder.className = "reorder-arrows placeholder";
            return placeholder;
        }
        const box = document.createElement("span");
        box.className = "reorder-arrows";

        const up = document.createElement("button");
        up.type = "button";
        up.title = "Move up";
        up.innerHTML = CARET_UP_SVG;
        up.disabled = index === 0;
        up.addEventListener("click", () => {
            if (this.placeAt(index, index - 1)) {
                this.render();
            }
        });

        const down = document.createElement("button");
        down.type = "button";
        down.title = "Move down";
        down.innerHTML = CARET_DOWN_SVG;
        down.disabled = index === this.items.length - 1;
        down.addEventListener("click", () => {
            if (this.placeAt(index, index + 2)) {
                this.render();
            }
        });

        box.appendChild(up);
        box.appendChild(down);
        return box;
    },

    // Any row is a valid drop target (including a static global): the drop position is decided by the
    // top / bottom half of the hovered row, so a package can be placed directly above or below the
    // read-only global entry. Only non-static rows are draggable.
    wireDrag: function (li, index, item) {
        if (!item.static) {
            li.setAttribute("draggable", "true");
            li.addEventListener("dragstart", (event) => {
                this.dragIndex = index;
                li.classList.add("dragging");
                event.dataTransfer.effectAllowed = "move";
                try {
                    event.dataTransfer.setData("text/plain", String(index));
                } catch (ignored) { /* some browsers require a payload */ }
            });
            li.addEventListener("dragend", () => {
                li.classList.remove("dragging");
                this.clearDropIndicators();
                this.dragIndex = null;
            });
        }
        li.addEventListener("dragover", (event) => {
            if (this.dragIndex === null) {
                return;
            }
            event.preventDefault();
            event.dataTransfer.dropEffect = "move";
            this.clearDropIndicators();
            li.classList.add(this.isBottomHalf(event, li) ? "drop-below" : "drop-above");
        });
        li.addEventListener("dragleave", () => {
            li.classList.remove("drop-above", "drop-below");
        });
        li.addEventListener("drop", (event) => {
            if (this.dragIndex === null) {
                return;
            }
            event.preventDefault();
            const insertIndex = this.isBottomHalf(event, li) ? index + 1 : index;
            const changed = this.placeAt(this.dragIndex, insertIndex);
            this.dragIndex = null;
            this.clearDropIndicators();
            if (changed) {
                this.render();
            }
        });
    },

    isBottomHalf: function (event, element) {
        const rect = element.getBoundingClientRect();
        return (event.clientY - rect.top) > rect.height / 2;
    },

    clearDropIndicators: function () {
        this.sortableList.querySelectorAll(".drop-above, .drop-below")
            .forEach(node => node.classList.remove("drop-above", "drop-below"));
    },

    // Move the item at fromIndex into the slot at insertIndex (0..length), recomputing its weight to
    // fit between its new neighbours. Works across a static global (which never moves). Returns true
    // when something actually changed.
    placeAt: function (fromIndex, insertIndex) {
        if (insertIndex === fromIndex || insertIndex === fromIndex + 1) {
            return false; // dropped back into its own slot
        }
        const moved = this.items[fromIndex];
        if (!moved || moved.static) {
            return false;
        }
        this.items.splice(fromIndex, 1);
        if (insertIndex > fromIndex) {
            insertIndex--;
        }
        insertIndex = Math.max(0, Math.min(this.items.length, insertIndex));
        this.items.splice(insertIndex, 0, moved);
        moved.weight = this.computeWeightForPosition(insertIndex);
        this.sortItems();
        return true;
    },

    // Weight that keeps the moved item at insertIndex: reuse its original weight when it still fits the
    // gap between the neighbours, otherwise place it in the middle of the gap (or just past the edge).
    computeWeightForPosition: function (insertIndex) {
        const items = this.items;
        if (items.length <= 1) {
            return items[insertIndex].weight;
        }
        const initial = items[insertIndex].originalWeight;
        let value;
        if (insertIndex === 0) {
            const next = items[1].weight;
            value = initial > next ? initial : next + 1;
        } else if (insertIndex === items.length - 1) {
            const prev = items[items.length - 2].weight;
            value = initial < prev ? initial : prev - 1;
        } else {
            const prev = items[insertIndex - 1].weight;
            const next = items[insertIndex + 1].weight;
            value = (initial > next && initial < prev)
                ? initial
                : parseFloat((prev + (next - prev) / 2).toFixed(1));
        }
        return Math.max(0, Math.min(100, value));
    },

    commitWeight: function (item, input) {
        StylePackageUtils.adjustWeight(input);
        item.weight = parseFloat(input.value);
        this.render();
    }
};

ctx.onClick(
    'save-toolbar-button', () => StylePackageWeights.saveWeights(),
    'cancel-toolbar-button', () => StylePackageWeights.loadPackageList()
);

StylePackageWeights.init('sortable-list');
StylePackageWeights.loadPackageList();
