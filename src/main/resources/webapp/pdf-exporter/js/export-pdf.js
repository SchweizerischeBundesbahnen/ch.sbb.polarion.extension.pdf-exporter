const ExportPdf = {
    stylePackageChanged: function () {
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

        ExportCommon.setCheckbox("webhooks-checkbox", !!stylePackage.webhooks);
        ExportCommon.setSelector("webhooks-selector", stylePackage.webhooks);
        ExportCommon.displayIf("webhooks-selector", !!stylePackage.webhooks, "inline-block")

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

    prepareRequest: function (projectId, locationPath, revision, fileName) {
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

        return this.buildRequestJson(projectId, locationPath, revision, selectedChapters, numberedListStyles, selectedRoles, fileName);
    },

    buildRequestJson: function (projectId, locationPath, revision, selectedChapters, numberedListStyles, selectedRoles, fileName) {
        const urlSearchParams = new URL(window.location.href.replace('#', '/')).searchParams;
        return new ExportParams.Builder(ExportParams.DocumentType.LIVE_DOC)
            .setProjectId(projectId)
            .setLocationPath(locationPath)
            .setRevision(revision)
            .setCoverPage(document.getElementById("cover-page-checkbox").checked ? document.getElementById("cover-page-selector").value : null)
            .setCss(document.getElementById("css-selector").value)
            .setHeaderFooter(document.getElementById("header-footer-selector").value)
            .setLocalization(document.getElementById("localization-selector").value)
            .setWebhooks(document.getElementById("webhooks-checkbox").checked ? document.getElementById("webhooks-selector").value : null)
            .setHeadersColor(document.getElementById("headers-color").value)
            .setPaperSize(document.getElementById("paper-size-selector").value)
            .setOrientation(document.getElementById("orientation-selector").value)
            .setFitToPage(document.getElementById('fit-to-page').checked)
            .setEnableCommentsRendering(document.getElementById('enable-comments-rendering').checked)
            .setWatermark(document.getElementById("watermark").checked)
            .setMarkReferencedWorkitems(document.getElementById("mark-referenced-workitems").checked)
            .setCutEmptyChapters(document.getElementById("cut-empty-chapters").checked)
            .setCutEmptyWIAttributes(document.getElementById('cut-empty-wi-attributes').checked)
            .setCutLocalUrls(document.getElementById("cut-urls").checked)
            .setFollowHTMLPresentationalHints(document.getElementById("presentational-hints").checked)
            .setNumberedListStyles(numberedListStyles)
            .setChapters(selectedChapters)
            .setLanguage(document.getElementById('localization').checked ? document.getElementById("language").value : null)
            .setLinkedWorkitemRoles(selectedRoles)
            .setFileName(fileName)
            .setUrlQueryParameters(Object.fromEntries([...urlSearchParams]))
            .build()
            .toJSON();
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

    loadPdf: function (projectId, locationPath, revision) {
        //clean previous errors
        $("#export-error").empty();
        $("#export-warning").empty();

        let fileName = document.getElementById("filename").value;
        if (!fileName) {
            fileName = document.getElementById("filename").dataset.default;
        }
        if (fileName && !fileName.endsWith(".pdf")) {
            fileName += ".pdf";
        }

        let request = this.prepareRequest(projectId, locationPath, revision, fileName);
        if (request === undefined) {
            return;
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

            ExportCommon.downloadBlob(successResponse, fileName);
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

    validatePdf: function (projectId, locationPath, revision) {
        //clean previous errors
        $("#validate-error").empty();
        $("#validate-ok").empty();

        let request = this.prepareRequest(projectId, locationPath, revision);
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
                    img.src = 'data:image/png;base64,' + page.content;
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
