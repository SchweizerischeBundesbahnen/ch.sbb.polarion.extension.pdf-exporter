const ExportPdf = {
    stylePackageChanged: function () {
        console.log("stylePackageChanged");
        const selectedStylePackage = document.getElementById("style-package-select").value;
        const scope = document.querySelector("input[name='scope']").value;
        if (selectedStylePackage && scope) {
            document.querySelectorAll('button').forEach(actionButton => {
                actionButton.disabled = true;
            });

            const $stylePackageError = $("#style-package-error");
            $stylePackageError.empty();

            SbbCommon.callAsync({
                method: "GET",
                url: `/polarion/pdf-exporter/rest/internal/settings/style-package/names/${selectedStylePackage}/content?scope=${scope}`,
                responseType: "json",
                onOk: (responseText, request) => {
                    ExportPdf.stylePackageSelected(request.response);
                    document.querySelectorAll('button').forEach(actionButton => {
                        actionButton.disabled = false;
                    });
                },
                onError: () => {
                    $stylePackageError.append("There was an error loading style package settings. Please, contact administrator");
                }
            });
        }
    },

    stylePackageSelected: function (stylePackage) {
        if (!stylePackage) {
            return;
        }
        const documentLanguage = document.getElementById("document-language").value;

        ExportCommon.setCheckbox("cover-page-checkbox", stylePackage.coverPage);

        ExportCommon.setSelector("cover-page-selector", stylePackage.coverPage);
        ExportCommon.displayIf("cover-page-selector", stylePackage.coverPage, "inline-block")

        ExportCommon.setSelector("css-selector", stylePackage.css);
        ExportCommon.setSelector("header-footer-selector", stylePackage.headerFooter);
        ExportCommon.setSelector("localization-selector", stylePackage.localization);

        ExportCommon.setValue("paper-size-selector", stylePackage.paperSize || 'A4');
        ExportCommon.setValue("headers-color", stylePackage.headersColor);
        ExportCommon.setValue("orientation-selector", stylePackage.orientation || 'PORTRAIT');
        ExportCommon.setCheckbox("fit-to-page", stylePackage.fitToPage);
        ExportCommon.setCheckbox("enable-comments-rendering", stylePackage.renderComments);
        ExportCommon.setCheckbox("watermark", stylePackage.watermark);
        ExportCommon.setCheckbox("mark-referenced-workitems", stylePackage.markReferencedWorkitems);
        ExportCommon.setCheckbox("cut-empty-chapters", stylePackage.cutEmptyChapters);
        ExportCommon.setCheckbox("cut-empty-wi-attributes", stylePackage.cutEmptyWorkitemAttributes);
        ExportCommon.setCheckbox("cut-urls", stylePackage.cutLocalURLs);
        ExportCommon.setCheckbox("presentational-hints", stylePackage.followHTMLPresentationalHints);

        ExportCommon.setCheckbox("custom-list-styles", stylePackage.customNumberedListStyles);
        ExportCommon.setValue("numbered-list-styles", stylePackage.customNumberedListStyles || "");
        ExportCommon.displayIf("numbered-list-styles", stylePackage.customNumberedListStyles);

        ExportCommon.setCheckbox("specific-chapters", stylePackage.specificChapters);
        ExportCommon.setValue("chapters", stylePackage.specificChapters || "");
        ExportCommon.displayIf("chapters", stylePackage.specificChapters);

        ExportCommon.setCheckbox("localization", stylePackage.language);
        ExportCommon.setValue("language", (stylePackage.exposeSettings && stylePackage.language && documentLanguage) ? documentLanguage : stylePackage.language);
        ExportCommon.displayIf("language", stylePackage.language);

        ExportCommon.setCheckbox("selected-roles", stylePackage.linkedWorkitemRoles);
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
        ExportCommon.displayIf("roles-wrapper", stylePackage.linkedWorkitemRoles);

        ExportCommon.displayIf("style-package-content", stylePackage.exposeSettings);
        ExportCommon.displayIf("page-width-validation", stylePackage.exposePageWidthValidation);
    },

    setClass: function (elementId, className) {
        document.getElementById(elementId).className = className;
    },

    prepareRequest: function (projectId, locationPath) {
        let selectedChapters = null;
        if (document.getElementById("specific-chapters").checked) {
            selectedChapters = this.getSelectedChapters();
            this.setClass("chapters", selectedChapters ? "" : "error");
            if (!selectedChapters) {
                $("#export-error").append("Please, provide comma separated list of integer values in chapters field");
                return undefined;
            }
        }

        let numberedListStyles = null;
        if (document.getElementById("custom-list-styles").checked) {
            numberedListStyles = document.getElementById("numbered-list-styles").value;
            const error = this.validateNumberedListStyles(numberedListStyles);
            this.setClass("numbered-list-styles", error ? "error" : "");
            if (error) {
                $("#export-error").append(error);
                return undefined;
            }
        }

        const selectedRoles = [];
        if (document.getElementById("selected-roles").checked) {
            const selectedOptions = Array.from(document.getElementById("roles-selector").options).filter(opt => opt.selected);
            selectedRoles.push(...selectedOptions.map(opt => opt.value));
        }

        return this.buildRequestJson(projectId, locationPath, selectedChapters, numberedListStyles, selectedRoles);
    },

    buildRequestJson: function (projectId, locationPath, selectedChapters, numberedListStyles, selectedRoles) {
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
    },

    getSelectedChapters: function () {
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
    },

    validateNumberedListStyles: function (numberedListStyles) {
        if (!numberedListStyles || numberedListStyles.trim().length === 0) {
            // Stop processing if empty
            return "Please, provide some value";
        } else if (numberedListStyles.match("[^1aAiI]+")) {
            // Stop processing if not valid styles
            return "Please, provide any combination of characters '1aAiI'";

        }
        return undefined;
    },

    loadPdf: function (projectId, locationPath) {
        //clean previous errors
        $("#export-error").empty();
        $("#export-warning").empty();

        let request = this.prepareRequest(projectId, locationPath);
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

        this.actionInProgress(true);

        SbbCommon.callAsync({
            method: "POST",
            url: "/polarion/pdf-exporter/rest/internal/checknestedlists",
            contentType: "application/json",
            responseType: "json",
            body: request,
            onOk: (responseText, request) => {
                if (request.response.containsNestedLists) {
                    $("#export-warning").append("Document contains nested numbered lists which structures were not valid. " +
                        "We did our best to fix it, but be aware of it.");
                }
            },
            onError: (status, errorMessage, request) => {
                $("#export-error").append("Error occurred validating nested lists" + (request.response.message ? ":<br>" + request.response.message : ""));
            }
        });

        ExportCommon.asyncConvertPdf(request, successResponse => {
            this.actionInProgress(false);
            const objectURL = (window.URL ? window.URL : window.webkitURL).createObjectURL(successResponse);
            const anchorElement = document.createElement("a");
            anchorElement.href = objectURL;
            anchorElement.download = filename;
            anchorElement.target = "_blank";
            anchorElement.click();
            anchorElement.remove();
            setTimeout(() => URL.revokeObjectURL(objectURL), 100);
        }, errorResponse => {
            this.actionInProgress(false);
            errorResponse.text().then(errorJson => {
                const error = errorJson && JSON.parse(errorJson);
                const errorMessage = error && (error.message ? error.message : error.errorMessage);
                $("#export-error").append("Error occurred during PDF generation" + (errorMessage ? ":<br>" + errorMessage : ""));
            });
        });
    },

    actionInProgress: function (inProgress) {
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
    },

    validatePdf: function (projectId, locationPath) {
        //clean previous errors
        $("#validate-error").empty();
        $("#validate-ok").empty();

        let request = this.prepareRequest(projectId, locationPath);
        if (request === undefined) {
            return;
        }

        //disable components
        $("#" + $("#export-pdf").parent().parent().attr("id") + " :input").attr("disabled", true);
        //show loading icon
        $("#validate-pdf-progress").show();

        const stopProgress = function () {
            //enable components
            $("#" + $("#export-pdf").parent().parent().attr("id") + " :input").attr("disabled", false);
            //hide loading icon
            $("#validate-pdf-progress").hide();
            document.getElementById('validate-error').replaceChildren(); //remove div content
        };

        SbbCommon.callAsync({
            method: "POST",
            url: "/polarion/pdf-exporter/rest/internal/validate?max-results=6",
            contentType: "application/json",
            responseType: "json",
            body: request,
            onOk: (responseText, request) => {
                stopProgress();
                const container = document.getElementById('validate-error');
                let result = request.response;
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
            },
            onError: (status, errorMessage, request) => {
                stopProgress();
                $("#validate-error").append("Error occurred validating pages width" + (request.response.message ? ":<br>" + request.response.message : ""));
            }
        });
    }
}
