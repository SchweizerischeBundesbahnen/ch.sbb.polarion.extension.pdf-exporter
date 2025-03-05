import ExtensionContext from '../../ui/generic/js/modules/ExtensionContext.js';
import StylePackageUtils from './style-package-utils.js';
import '../sortable.js';

const ctx = new ExtensionContext({
    extension: 'pdf-exporter',
    scopeFieldId: 'scope'
});

ctx.getElementById("default-toolbar-button").style.display = "none";
ctx.getElementById("revisions-toolbar-button").style.display = "none";

const StylePackageWeights = {

    ID_PREFIX: 'input.weight.',
    STYLE_INPUT: '.weight-input',
    STYLE_ITEM: '.sortable-item',

    init: function (listId) {
        this.name = 'StylePackageWeights';
        this.sortableList = ctx.getElementById(listId);

        this.sortable = new Sortable(this.sortableList, {
            animation: 150,
            filter: '.static,.weight-input',
            preventOnFilter: false,
            onEnd: (evt) => {
                this.afterPositionChange(evt.oldDraggableIndex, evt.newDraggableIndex);
            }
        });
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
        const result = [];

        this.sortableList.querySelectorAll(this.STYLE_ITEM).forEach(item => {
            if (item.classList.contains('static')) {
                return;
            }
            const input = item.querySelector('input');
            const name = input.id.replace(this.ID_PREFIX, '');
            const weight = parseFloat(input.value);
            const scope = ctx.scope;

            result.push({
                name: name, scope: scope, weight: weight
            });
        });

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
        this.data = JSON.parse(jsonString);
        const weightStyleName = this.STYLE_INPUT.replace('.', '');
        this.sortableList.innerHTML = '';
        this.data.forEach((item) => {
            const li = document.createElement("li");
            li.classList.add(this.STYLE_ITEM.replace('.', ''));
            const globalScopedItem = item.scope === "" && ctx.scope !== "";
            if (globalScopedItem) {
                li.classList.add("static"); // Add static class when scope is empty
            }
            const globalSuffix = globalScopedItem ? " (global)" : "";
            li.innerHTML = `${item.name}${globalSuffix} <input id="${this.ID_PREFIX}${item.name}" type="number" class="${weightStyleName}" value="${item.weight}" min="0" max="100" step="0.1"/>`;
            this.sortableList.appendChild(li);
        });

        this.sortableList.querySelectorAll(this.STYLE_INPUT).forEach(input => {
            input.addEventListener('keydown', (event) => this.handleManualChange(event)); // Listen for 'Enter'
            input.addEventListener('blur', (event) => this.handleManualChange(event)); // Listen for 'blur' (focus change)
        });
        this.sortList();
    },

    sortList: function () {
        const listItems = Array.from(this.sortableList.querySelectorAll(this.STYLE_ITEM));

        // Sort the items
        listItems.sort((a, b) => {
            let inputB = b.querySelector(this.STYLE_INPUT);
            let inputA = a.querySelector(this.STYLE_INPUT);
            const weightDifference = parseFloat(inputB.value) - parseFloat(inputA.value);
            return weightDifference === 0 ? inputA.id.localeCompare(inputB.id) : weightDifference;
        });
        this.sortableList.innerHTML = '';
        listItems.forEach(item => this.sortableList.appendChild(item));
    },

    handleManualChange: function (event) {
        if (event.type === 'blur') {
            StylePackageUtils.adjustWeight(event.target);
            const modifiedSettingName = event.target.id.replace(this.ID_PREFIX, '');

            const previousPos = this.getPositionByName(modifiedSettingName);
            const items = Array.from(this.sortableList.querySelectorAll(this.STYLE_ITEM));
            items.sort((a, b) => {
                let inputB = b.querySelector(this.STYLE_INPUT);
                let inputA = a.querySelector(this.STYLE_INPUT);
                const weightDifference = parseFloat(inputB.value) - parseFloat(inputA.value);
                return weightDifference === 0 ? inputA.id.replace(this.ID_PREFIX, '').localeCompare(b.id.replace(this.ID_PREFIX, '')) : weightDifference;
            });

            const newPos = this.getPositionByName(modifiedSettingName, items);
            this.moveItem(previousPos, newPos);
        }
    },

    moveItem: function (fromPosition, toPosition) {
        if (fromPosition === toPosition) {
            this.afterPositionChange(fromPosition, toPosition);
            return;
        }
        const items = Array.from(this.sortableList.children);

        const fromItem = items[fromPosition];
        const toItem = items[toPosition];

        const fromItemRect = fromItem.getBoundingClientRect();
        const toItemRect = toItem.getBoundingClientRect();
        const distance = fromItemRect.top - toItemRect.top;

        // Temporarily apply transition for smooth movement
        fromItem.style.transition = 'transform 0.5s ease';
        fromItem.style.transform = `translateY(${-distance}px)`;

        const self = this;
        fromItem.addEventListener('transitionend', function handleTransitionEnd() {
            fromItem.style.transition = '';
            fromItem.style.transform = '';

            self.sortableList.insertBefore(fromItem, fromPosition < toPosition ? toItem.nextSibling : toItem);

            fromItem.removeEventListener('transitionend', handleTransitionEnd);

            self.afterPositionChange(fromPosition, toPosition);
        });
    },

    afterPositionChange: function (fromPosition, toPosition) {
        if (fromPosition !== toPosition) {
            this.updateWeightForNewPosition(toPosition);
        }
        // We have to sort list again even if its position hasn't changed.
        // For example:
        // 1. We have sequence like "ccc 45, aaa 42, bbb 42, ddd 42"
        // 2. Manually change ccc value to 42
        // 3. Its position initially wasn't changed, but according to our rules when multiple items
        //    have the same weight they must be sorted alphabetically
        // 4. Sort the resulting list to get proper result: "aaa 42, bbb 42, ccc 42, ddd 42"
        this.sortList();
    },

    updateWeightForNewPosition: function (newPosition) {
        const items = this.sortableList.querySelectorAll(this.STYLE_ITEM);
        const changedItemName = items[newPosition].querySelector(this.STYLE_INPUT).id.replace(this.ID_PREFIX, '');

        // we are going to try to keep initial weight
        const initialWeight = this.data.find(i => i.name === changedItemName).weight;

        let newValue;
        if (newPosition === 0) {
            const nextItemValue = parseFloat(items[newPosition + 1].querySelector(this.STYLE_INPUT).value);
            newValue = initialWeight > nextItemValue ? initialWeight : nextItemValue + 1;
        } else if (newPosition === items.length - 1) {
            const prevItemValue = parseFloat(items[items.length - 2].querySelector(this.STYLE_INPUT).value);
            newValue = initialWeight < prevItemValue ? initialWeight : prevItemValue - 1;
        } else {
            const prevItemValue = parseFloat(items[newPosition - 1].querySelector(this.STYLE_INPUT).value);
            const nextItemValue = parseFloat(items[newPosition + 1].querySelector(this.STYLE_INPUT).value);
            newValue = initialWeight > nextItemValue && initialWeight < prevItemValue ? initialWeight :
                parseFloat((prevItemValue + (nextItemValue - prevItemValue) / 2).toFixed(1));
        }

        items[newPosition].querySelector(this.STYLE_INPUT).value = Math.max(0, Math.min(100, newValue));
    },

    getPositionByName: function (name, dynamicItems) {
        const items = dynamicItems === undefined ? this.sortableList.querySelectorAll(this.STYLE_ITEM) : dynamicItems;
        const id = this.ID_PREFIX + name;
        for (let i = 0; i < items.length; i++) {
            if (items[i].querySelector(this.STYLE_INPUT).id === id) {
                return i;
            }
        }
        throw new Error(`Unknown name "${name}"`);
    }
}

ctx.onClick(
    'save-toolbar-button', () => StylePackageWeights.saveWeights(),
    'cancel-toolbar-button', () => StylePackageWeights.loadPackageList()
);

StylePackageWeights.init('sortable-list');
StylePackageWeights.loadPackageList();
