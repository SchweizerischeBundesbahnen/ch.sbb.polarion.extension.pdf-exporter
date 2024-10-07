SbbCommon.init({
    extension: 'pdf-exporter',
    scope: SbbCommon.getValueById('scope')
});

const StylePackageWeights = {

    ID_PREFIX: 'input.weight.',
    STYLE_INPUT: '.weight-input',
    STYLE_ITEM: '.sortable-item',

    init: function (listId) {
        this.name = 'StylePackageWeights';
        this.sortableList = document.getElementById(listId);

        this.sortable = new Sortable(this.sortableList, {
            animation: 150,
            filter: '.static,.weight-input',
            preventOnFilter: false,
            onEnd: (evt) => {
                if (evt.newDraggableIndex !== evt.oldDraggableIndex) {
                    this.updateWeightForNewPosition(evt.newDraggableIndex);
                    this.sortList();
                }
            }
        });
    },

    loadPackageList: function () {
        SbbCommon.callAsync({
            method: 'GET',
            url: `/polarion/${SbbCommon.extension}/rest/internal/settings/style-package/weights?scope=${SbbCommon.scope}`,
            contentType: 'application/json',
            onOk: (responseText) => {
                this.setData(responseText);
            },
            onError: () => SbbCommon.setLoadingErrorNotificationVisible(true)
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
            const scope = SbbCommon.scope;

            result.push({
                name: name, scope: scope, weight: weight
            });
        });

        SbbCommon.callAsync({
            method: 'POST',
            url: `/polarion/${SbbCommon.extension}/rest/internal/settings/style-package/weights`,
            contentType: 'application/json',
            body: JSON.stringify(result),
            onOk: () => {
                SbbCommon.showSaveSuccessAlert();
            },
            onError: () => SbbCommon.showSaveErrorAlert()
        });
    },

    setData: function (jsonString) {
        const data = JSON.parse(jsonString);
        const weightStyleName = this.STYLE_INPUT.replace('.', '');
        this.sortableList.innerHTML = '';
        data.forEach((item) => {
            const li = document.createElement("li");
            li.classList.add(this.STYLE_ITEM.replace('.', ''));
            const globalScopedItem = item.scope === "" && SbbCommon.scope !== "";
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
        });
    },

    updateWeightForNewPosition: function (newPosition) {
        const items = this.sortableList.querySelectorAll(this.STYLE_ITEM);

        let newValue;
        if (newPosition === 0) {
            const nextItemValue = parseFloat(items[newPosition + 1].querySelector(this.STYLE_INPUT).value);
            newValue = nextItemValue + 1;
        } else if (newPosition === items.length - 1) {
            const prevItemValue = parseFloat(items[items.length - 2].querySelector(this.STYLE_INPUT).value);
            newValue = prevItemValue - 1;
        } else {
            const prevItemValue = parseFloat(items[newPosition - 1].querySelector(this.STYLE_INPUT).value);
            const nextItemValue = parseFloat(items[newPosition + 1].querySelector(this.STYLE_INPUT).value);
            newValue = parseFloat((prevItemValue + (nextItemValue - prevItemValue) / 2).toFixed(1));
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

StylePackageWeights.init('sortable-list');
StylePackageWeights.loadPackageList();
