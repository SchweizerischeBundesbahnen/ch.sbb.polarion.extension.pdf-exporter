let consts = {
    DEFAULT_SETTING_NAME: "Default",
    DOC_PDF_CONVERSION_PULL_INTERVAL: 1000
}

function stylePackageChanged() {
    console.log("stylePackageChanged");
    const selectedStylePackage = document.getElementById("style-package-select").value;
    const scope = document.querySelector("input[name='scope']").value;
    if (selectedStylePackage && scope) {
        document.querySelectorAll('button').forEach(actionButton => {
            actionButton.disabled = true;
        });

        const xhr = new XMLHttpRequest();
        xhr.open("GET", `/polarion/pdf-exporter/rest/internal/settings/style-package/names/${selectedStylePackage}/content?scope=${scope}`, true);
        xhr.responseType = "json";
        xhr.send();

        const $stylePackageError = $("#style-package-error");
        $stylePackageError.empty();

        xhr.onload = () => {
            if (xhr.status === 200) {
                stylePackageSelected(xhr.response);

                document.querySelectorAll('button').forEach(actionButton => {
                    actionButton.disabled = false;
                });
            } else {
                $stylePackageError.append("There was an error loading style package settings. Please, contact administrator");
            }
        };
    }
}

function stylePackageSelected(stylePackage) {
    if (!stylePackage) {
        return;
    }
    const documentLanguage = document.getElementById("document-language").value;

    setCheckbox("cover-page-checkbox", stylePackage.coverPage);

    setSelector("cover-page-selector", stylePackage.coverPage);
    displayIf("cover-page-selector", stylePackage.coverPage, "inline-block")

    setSelector("css-selector", stylePackage.css);
    setSelector("header-footer-selector", stylePackage.headerFooter);
    setSelector("localization-selector", stylePackage.localization);

    setValue("headers-color", stylePackage.headersColor);
    setValue("paper-size-selector", stylePackage.paperSize || 'A4');
    setValue("orientation-selector", stylePackage.orientation || 'PORTRAIT');
    setCheckbox("fit-to-page", stylePackage.fitToPage);
    setCheckbox("enable-comments-rendering", stylePackage.renderComments);
    setCheckbox("watermark", stylePackage.watermark);
    setCheckbox("mark-referenced-workitems", stylePackage.markReferencedWorkitems);
    setCheckbox("cut-empty-chapters", stylePackage.cutEmptyChapters);
    setCheckbox("cut-empty-wi-attributes", stylePackage.cutEmptyWorkitemAttributes);
    setCheckbox("cut-urls", stylePackage.cutLocalURLs);
    setCheckbox("presentational-hints", stylePackage.followHTMLPresentationalHints);

    setCheckbox("custom-list-styles", stylePackage.customNumberedListStyles);
    setValue("numbered-list-styles", stylePackage.customNumberedListStyles || "");
    displayIf("numbered-list-styles", stylePackage.customNumberedListStyles);

    setCheckbox("specific-chapters", stylePackage.specificChapters);
    setValue("chapters", stylePackage.specificChapters || "");
    displayIf("chapters", stylePackage.specificChapters);

    setCheckbox("localization", stylePackage.language);
    setValue("language", (stylePackage.exposeSettings && stylePackage.language && documentLanguage) ? documentLanguage : stylePackage.language);
    displayIf("language", stylePackage.language);

    setCheckbox("selected-roles", stylePackage.linkedWorkitemRoles);
    document.querySelectorAll(`#roles-selector option`).forEach(roleOption => {
        roleOption.selected = false;
    });
    if (stylePackage.linkedWorkitemRoles) {
        for (const role of stylePackage.linkedWorkitemRoles) {
            document.querySelectorAll(`#roles-selector option[value='${role}']`).forEach(roleOption => {
                roleOption.selected = true;
            });
        }
    }
    displayIf("roles-wrapper", stylePackage.linkedWorkitemRoles);

    displayIf("style-package-content", stylePackage.exposeSettings);
    displayIf("page-width-validation", stylePackage.exposePageWidthValidation);
}

function setCheckbox(elementId, value) {
    document.getElementById(elementId).checked = !!value;
}

function setValue(elementId, value) {
    document.getElementById(elementId).value = value;
}

function setSelector(elementId, value) {
    const selector = document.getElementById(elementId);
    selector.value = containsOption(selector, value) ? value : consts.DEFAULT_SETTING_NAME;
}

function displayIf(elementId, condition, displayStyle = "block") {
    document.getElementById(elementId).style.display = condition ? displayStyle : "none";
}

function setClass(elementId, className) {
    document.getElementById(elementId).className = className;
}

function containsOption(selectElement, option) {
    return [...selectElement.options].map(o => o.value).includes(option);
}

function prepareRequest(projectId, locationPath) {
    let selectedChapters = null;
    if (document.getElementById("specific-chapters").checked) {
        selectedChapters = getSelectedChapters();
        setClass("chapters", selectedChapters ? "" : "error");
        if (!selectedChapters) {
            $("#export-error").append("Please, provide comma separated list of integer values in chapters field");
            return undefined;
        }
    }
    let numberedListStyles = null;
    if (document.getElementById("custom-list-styles").checked) {
        numberedListStyles = document.getElementById("numbered-list-styles").value;
        const error = validateNumberedListStyles(numberedListStyles);
        setClass("numbered-list-styles", error ? "error" : "");
        if (error) {
            $("#export-error").append(error);
            return undefined;
        }
    }

    const selectedRoles = [];
    if (document.getElementById("selected-roles").checked) {
        for (const opt of document.getElementById("roles-selector").options) {
            if (opt.selected) {
                selectedRoles.push(opt.value);
            }
        }
    }

    return JSON.stringify({
        projectId: projectId,
        locationPath: locationPath,
        revision: new URL(window.location.href.replace('#', '/')).searchParams.get('revision'),
        coverPage: document.getElementById("cover-page-checkbox").checked ? document.getElementById("cover-page-selector").value : null,
        css: document.getElementById("css-selector").value,
        headerFooter: document.getElementById("header-footer-selector").value,
        localization: document.getElementById("localization-selector").value,
        headersColor: document.getElementById("headers-color").value,
        paperSize: document.getElementById("paper-size-selector").value,
        orientation: document.getElementById("orientation-selector").value,
        fitToPage: document.getElementById('fit-to-page').checked,
        enableCommentsRendering: document.getElementById('enable-comments-rendering').checked,
        watermark: document.getElementById("watermark").checked,
        markReferencedWorkitems: document.getElementById("mark-referenced-workitems").checked,
        cutEmptyChapters: document.getElementById("cut-empty-chapters").checked,
        cutEmptyWIAttributes: document.getElementById('cut-empty-wi-attributes').checked,
        cutLocalUrls: document.getElementById("cut-urls").checked,
        followHTMLPresentationalHints: document.getElementById("presentational-hints").checked,
        numberedListStyles: numberedListStyles,
        chapters: selectedChapters,
        language: document.getElementById('localization').checked ? document.getElementById("language").value : null,
        liveDocumentLanguage: new URL(window.location.href.replace('#', '/')).searchParams.get('language'),
        linkedWorkitemRoles: selectedRoles,
    });
}

function getSelectedChapters() {
    const chaptersValue = document.getElementById("chapters").value;
    let chapters = (chaptersValue?.replaceAll(" ", "") || "").split(",");
    if (chapters && chapters.length > 0) {
        for (const chapter of chapters) {
            const parsedValue = Number.parseInt(chapter);
            if (Number.isNaN(parsedValue) || parsedValue < 1 || String(parsedValue) !== chapter) {
                // Stop processing if not valid numbers
                return undefined;
            }
        }
    }
    return chapters;
}

function validateNumberedListStyles(numberedListStyles) {
    if (!numberedListStyles || numberedListStyles.trim().length === 0) {
        // Stop processing if empty
        return "Please, provide some value";
    } else if (numberedListStyles.match("[^1aAiI]+")) {
        // Stop processing if not valid styles
        return "Please, provide any combination of characters '1aAiI'";

    }
    return undefined;
}

function loadPdf(projectId, locationPath) {
    //clean previous errors
    $("#export-error").empty();
    $("#export-warning").empty();

    let request = prepareRequest(projectId, locationPath);
    if (request === undefined) {
        return;
    }

    let filename = document.getElementById("filename").value;
    if (!filename) {
        filename = document.getElementById("filename").dataset.default;
    }
    if (!filename.endsWith(".pdf")) {
        filename += ".pdf";
    }

    actionInProgress(true);

    const listsCheckXhr = new XMLHttpRequest();
    listsCheckXhr.open("POST", "/polarion/pdf-exporter/rest/internal/checknestedlists", true);
    listsCheckXhr.setRequestHeader("Content-Type", "application/json");
    listsCheckXhr.responseType = "json";
    listsCheckXhr.send(request);
    listsCheckXhr.onload = () => {
        if (listsCheckXhr.status === 200) {
            if (listsCheckXhr.response.containsNestedLists) {
                $("#export-warning").append("Document contains nested numbered lists which structures were not valid. " +
                    "We did our best to fix it, but be aware of it.");
            }
        } else {
            //display error body content
            $("#export-error").append("Error occurred validating nested lists" + (listsCheckXhr.response.message ? ":<br>" + listsCheckXhr.response.message : ""));
        }
    }

    asyncConvertPdf(request, successResponse => {
        actionInProgress(false);
        const objectURL = (window.URL ? window.URL : window.webkitURL).createObjectURL(successResponse);
        const anchorElement = document.createElement("a");
        anchorElement.href = objectURL;
        anchorElement.download = filename;
        anchorElement.target = "_blank";
        anchorElement.click();
        anchorElement.remove();
        setTimeout(() => URL.revokeObjectURL(objectURL), 100);
    }, errorResponse => {
        actionInProgress(false);
        errorResponse.text().then(errorJson => {
            const error = errorJson && JSON.parse(errorJson);
            const errorMessage = error && (error.message ? error.message : error.errorMessage);
            $("#export-error").append("Error occurred during PDF generation" + (errorMessage ? ":<br>" + errorMessage : ""));
        });
    });
}

async function asyncConvertPdf(request, successCallback, errorCallback) {
    const xhr = new XMLHttpRequest();
    xhr.open("POST", "/polarion/pdf-exporter/rest/internal/convert/jobs", true);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.responseType = "blob";
    xhr.send(request);

    xhr.onload = () => {
        if (xhr.status === 202) {
            pullAndGetResultPdf(xhr.getResponseHeader("Location"), successCallback, errorCallback);
        } else {
            errorCallback(xhr.response);
        }
    };
}

async function pullAndGetResultPdf(url, successCallback, errorCallback) {
    await new Promise(resolve => setTimeout(resolve, consts.DOC_PDF_CONVERSION_PULL_INTERVAL));
    const xhr = new XMLHttpRequest();
    xhr.responseType = "blob";
    xhr.open("GET", url, true);
    xhr.send();

    xhr.onload = () => {
        if (xhr.status === 202) {
            console.log('Async PDF convertion: still in progress, retrying...');
            pullAndGetResultPdf(url, successCallback, errorCallback);
        } else if (xhr.status === 200) {
            successCallback(xhr.response);
        } else {
            errorCallback(xhr.response);
        }
    }
}

function actionInProgress(inProgress) {
    if (inProgress) {
        //disable components
        $("#" + $("#export-pdf").parent().parent().attr("id") + " :input").attr("disabled", true);
        //show loading icon
        $("#export-pdf-progress").show();
    } else {
        //enable components
        $("#" + $("#export-pdf").parent().parent().attr("id") + " :input").attr("disabled", false);
        //hide loading icon
        $("#export-pdf-progress").hide();
    }
}

function validatePdf(projectId, locationPath) {
    //clean previous errors
    $("#validate-error").empty();
    $("#validate-ok").empty();

    let request = prepareRequest(projectId, locationPath);
    if (request === undefined) {
        return;
    }

    //disable components
    $("#" + $("#export-pdf").parent().parent().attr("id") + " :input").attr("disabled", true);
    //show loading icon
    $("#validate-pdf-progress").show();

    const xhr = new XMLHttpRequest();
    xhr.open("POST", "/polarion/pdf-exporter/rest/internal/validate?max-results=6", true);
    xhr.setRequestHeader("Content-Type", "application/json");
    xhr.responseType = "json";
    xhr.send(request);

    xhr.onload = () => {
        //enable components
        $("#" + $("#export-pdf").parent().parent().attr("id") + " :input").attr("disabled", false);

        //hide loading icon
        $("#validate-pdf-progress").hide();

        const container = document.getElementById('validate-error');
        container.replaceChildren(); //remove div content

        if (xhr.status !== 200) {
            //display error body content
            $("#validate-error").append("Error occurred validating pages width" + (xhr.response.message ? ":<br>" + xhr.response.message : ""));
            return;
        }
        let result = xhr.response;
        let pages = result.invalidPages.length;
        if (pages === 0) {
            $("#validate-ok").append("OK");
            return;
        }
        let message = (pages > 5 ? 'More than 5' : pages) + ' invalid page' + (pages === 1 ? '' : 's') + ' found:';
        container.appendChild(document.createTextNode(message));
        container.appendChild(document.createElement("br"));
        for (const page of result.invalidPages) {
            let img = document.createElement("img");
            img.className = 'validate-result-img';
            img.src = 'data:image/png;base64, ' + page.content;
            img.onclick = function () {
                this.classList.toggle("img-zoomed");
            };
            container.appendChild(img);
        }
        let suspects = result.suspiciousWorkItems.length;
        if (suspects > 0) {
            container.appendChild(document.createElement("br"));
            container.appendChild(document.createTextNode("Suspicious work items:"));
            let ul = document.createElement('ul');
            ul.className = 'suspicious-list';
            for (const suspect of result.suspiciousWorkItems) {
                let li = document.createElement('li');
                let link = document.createElement("a");
                link.href = suspect.link;
                link.text = suspect.id;
                link.target = '_blank';
                li.appendChild(link);
                ul.appendChild(li);
            }
            container.appendChild(ul);
        }
    };
}
