<%@ page import="ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%! String bundleTimestamp = ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestampDigitsOnly(); %>
<%! Boolean webhooksEnabled = PdfExporterExtensionConfiguration.getInstance().getWebhooksEnabled(); %>

<head>
    <title>PDF Exporter: Style Packages</title>
    <link rel="stylesheet" href="../ui/generic/css/prism.css?bundle=<%= bundleTimestamp %>">
    <script type="text/javascript" src="../ui/generic/js/prism.js?bundle=<%= bundleTimestamp %>"></script>
    <link rel="stylesheet" href="../ui/generic/css/code-input.min.css?bundle=<%= bundleTimestamp %>">
    <script type="text/javascript" src="../ui/generic/js/code-input.min.js?bundle=<%= bundleTimestamp %>"></script>
    <link rel="stylesheet" href="../ui/generic/css/common.css?bundle=<%= bundleTimestamp %>">
    <link rel="stylesheet" href="../ui/generic/css/custom-select.css?bundle=<%= bundleTimestamp %>">
    <link rel="stylesheet" href="../ui/generic/css/configurations.css?bundle=<%= bundleTimestamp %>">
    <script type="module" src="../js/modules/style-packages.js?bundle=<%= bundleTimestamp %>"></script>
    <style type="text/css">
        html {
            height: 100%;
        }
        body {
            height: 100%;
            padding-left: 10px;
            padding-right: 10px;
            margin: 0;
            display: flex;
            flex-direction: column;
        }
        .standard-admin-page {
            flex: 1;
            display: flex;
            flex-direction: column;
        }
        .style-package-error {
            color: red;
        }
        .flex-container {
            display: flex;
            column-gap: 20px;
            flex-wrap: wrap;
        }
        .flex-column {
            width: 440px;
        }
        .input-group {
            margin-bottom: 10px;
        }
        input[type="checkbox"] {
            width: auto;
            vertical-align: middle;
        }
        .checkbox.input-group label {
            width: auto;
        }
        .flex-centered {
            display: flex;
            align-items: center;
        }
        .flex-centered label {
            width: auto;
            margin-right: 4px;
        }
        .flex-grow {
            flex-grow: 1;
        }
        .more-info {
            background: url(/polarion/ria/images/msginfo.png) no-repeat;
            display: inline-block;
            width: 17px;
            height: 17px;
            cursor: pointer;
        }
    </style>
</head>

<body>
<div class="standard-admin-page">
    <h1>PDF Exporter: Style Packages</h1>

    <jsp:include page='/common/jsp/notifications.jsp' />

    <jsp:include page='/common/jsp/configurations.jsp' />

    <div class="content-area">
        <div id="child-configs-load-error" class="style-package-error" style="display: none; margin-bottom: 15px">
            There was an error loading names of children configurations. Please, contact project/system administrator to solve the issue, a style package can't be configured without them.
        </div>
        <div id="link-roles-load-error" class="style-package-error" style="display: none; margin-bottom: 15px">
            There was an error loading link role names.
        </div>

        <div class="flex-container" style="border-top: 1px solid #ccc; margin-top: 20px; padding-top: 15px;">

            <div class="flex-column">
                <div class='input-group flex-centered'>
                    <label for='style-package-weight'>Weight:</label>
                    <div class='more-info' title="A float number from 0.0 to 100, which will determine the position of current style package in the resulting style packages list. The higher the number, the higher its position will be."></div>
                    <input id="style-package-weight" style="margin-left: 59px" type="number" min="1" max="100" step="0.1">
                </div>
            </div>

            <div class="flex-grow" id="matching-query-container">
                <div class='input-group flex-centered'>
                    <label for='matching-query'>Matching query:</label>
                    <div class='more-info' title="A query to select documents to which this style package will be relevant. For documents not matching this query the style package won't be visible. If you want to make this style package be available to all documents, just leave this field empty."></div>
                    <input id='matching-query' class="flex-grow" style="margin-left: 8px;"/>
                </div>
            </div>

        </div>

        <div class="flex-container" style="border-top: 1px solid #ccc; margin-top: 20px; padding-top: 15px;">
            <div class="flex-column">
                <div class='checkbox input-group'>
                    <label for='exposeSettings'>
                        <input id='exposeSettings' type='checkbox'/>
                        Expose style package settings to be redefined on UI
                    </label>
                </div>
            </div>
        </div>

        <div class="flex-container">
            <div class="flex-column">
                <div class='checkbox input-group'>
                    <label for='cover-page-checkbox' style="width: 120px;">
                        <input id="cover-page-checkbox" onchange='document.getElementById("cover-page-select").style.display = this.checked ? "inline-block" : "none"' type='checkbox'/>
                        Cover page
                    </label>
                    <div id="cover-page-select" style="display: none"></div>
                </div>
                <div class="input-group">
                    <label for="css-select" id="css-select-label">CSS:</label>
                    <div id="css-select"></div>
                </div>
            </div>
            <div class="flex-column">
                <div class="input-group">
                    <label for="header-footer-select" id="header-footer-select-label">Header/Footer:</label>
                    <div id="header-footer-select"></div>
                </div>
                <div class="input-group">
                    <label for="localization-select" id="localization-select-label">Localization:</label>
                    <div id="localization-select"></div>
                </div>
            </div>
        </div>

        <div class="flex-container" style="border-top: 1px solid #ccc; margin-top: 20px; padding-top: 15px; display: <%= webhooksEnabled ? "flex" : "none" %>;">
            <div class="flex-column">
                <div class="input-group">
                    <label for='webhooks-checkbox' style="width: 120px;">
                        <input id="webhooks-checkbox" onchange='document.getElementById("webhooks-select").style.display = this.checked ? "inline-block" : "none"' type='checkbox'/>
                        Use webhooks
                    </label>
                    <div id="webhooks-select"></div>
                </div>
            </div>
        </div>

        <div class="flex-container" style="border-top: 1px solid #ccc; margin-top: 20px; padding-top: 15px;">
            <div class="flex-column">
                <div class='input-group'>
                    <label for='headers-color'>Headings color:</label>
                    <input id='headers-color' type='color' value='#004d73' style="width: 30px"/>
                </div>
            </div>
            <div class="flex-column">
                <div class='input-group'>
                    <label for="paper-size-select" id='paper-size-label'>Paper Size:</label>
                    <div id="paper-size-select"></div>
                </div>
                <div class='input-group'>
                    <label for="orientation-select" id='orientation-label'>Orientation:</label>
                    <div id="orientation-select"></div>
                </div>
                <div class='input-group'>
                    <label for="pdf-variant-select" id='pdf-variant-label'>PDF Variant:</label>
                    <div id="pdf-variant-select"></div>
                </div>
            </div>
        </div>

        <div class="flex-container">
            <div class="flex-column">
                <div class='checkbox input-group'>
                    <label for='fit-to-page'>
                        <input id="fit-to-page" type='checkbox' />
                        Fit images and tables to page
                    </label>
                </div>
                <div class='checkbox input-group'>
                    <label for='presentational-hints'>
                        <input id='presentational-hints' type='checkbox'/>
                        Follow HTML presentational hints
                    </label>
                </div>
                <div class='checkbox input-group'>
                    <label for='render-comments' id='render-comments-label'>
                        <input id="render-comments" onchange='document.getElementById("render-comments-select").style.visibility = this.checked ? "visible" : "hidden"' type='checkbox' />
                        Comments rendering
                    </label>
                    <div id="render-comments-select" style="visibility: hidden; margin-left: 10px; width: 200px"></div>
                </div>
                <div class='checkbox input-group'>
                    <label for='watermark'>
                        <input id='watermark' type='checkbox'/>
                        Watermark
                    </label>
                </div>
            </div>
            <div class="flex-column">
                <div class='checkbox input-group'>
                    <label for='cut-empty-chapters'>
                        <input id='cut-empty-chapters' type='checkbox'/>
                        Cut empty chapters (any level)
                    </label>
                </div>
                <div class='checkbox input-group'>
                    <label for='cut-empty-wi-attributes'>
                        <input id="cut-empty-wi-attributes" type='checkbox' />
                        Cut empty Workitem attributes
                    </label>
                </div>
                <div class='checkbox input-group'>
                    <label for='cut-urls'>
                        <input id='cut-urls' type='checkbox'/>
                        Cut local Polarion URLs
                    </label>
                </div>
                <div class='checkbox input-group'>
                    <label for='mark-referenced-workitems'>
                        <input id='mark-referenced-workitems' type='checkbox'/>
                        Mark referenced Workitems
                    </label>
                </div>
            </div>
        </div>
        <div class="flex-container">
            <div class="flex-column">
                <div class='checkbox input-group'>
                    <label for='custom-list-styles'>
                        <input id='custom-list-styles' onchange='document.getElementById("numbered-list-styles").style.visibility = this.checked ? "visible" : "hidden"' type='checkbox'/>
                        Custom styles of numbered lists
                    </label>
                    <input id='numbered-list-styles' placeholder='eg. 1ai' type='text' style="visibility: hidden; margin-left: 10px; width: 100px"/>
                </div>
                <div class='checkbox input-group'>
                    <label for='specific-chapters'>
                        <input id='specific-chapters' onchange='document.getElementById("chapters").style.visibility = this.checked ? "visible" : "hidden"' type='checkbox'/>
                        Specific higher level chapters
                    </label>
                    <input id='chapters' placeholder='eg. 1,2,4 etc.' type='text' style="visibility: hidden; margin-left: 10px; width: 202px"/>
                </div>
                <div class='checkbox input-group'>
                    <label for='metadata-fields'>
                        <input id='metadata-fields' onchange='document.getElementById("metadata-fields-input").style.visibility = this.checked ? "visible" : "hidden"' type='checkbox'/>
                        Metadata fields
                    </label>
                    <input id='metadata-fields-input' placeholder='e.g. docOwner, docLanguage, customField*' type='text' style='visibility: hidden; margin-left: 10px; width: 280px'/>
                </div>
            </div>
            <div class="flex-column">
                <div class='checkbox input-group'>
                    <label for='localization'>
                        <input id="localization" onchange='document.getElementById("language-select").style.visibility = this.checked ? "visible" : "hidden"' type='checkbox'/>
                        Localize enums
                    </label>
                    <div id="language-select" style="visibility: hidden; margin-left: 10px; width: 200px"></div>
                </div>
                <div class='checkbox input-group'>
                    <label for='selected-roles' style="margin-top: 5px">
                        <input id="selected-roles" onchange='document.getElementById("roles-select").style.display = this.checked ? "inline-block" : "none"' type='checkbox'/>
                        Specific Workitem roles
                    </label>
                    <div id="roles-select" style="display: none; margin-left: 10px; width: 152px"></div>
                </div>
            </div>
        </div>
        <div class="flex-container" style="border-top: 1px solid #ccc; margin-top: 20px; padding-top: 15px;">
            <div class="flex-column">
                <div class='checkbox input-group'>
                    <label for='download-attachments' style="margin-top: 5px">
                        <input id='download-attachments' onchange='
                                document.getElementById("attachments-filter-container").style.display = this.checked ? "block" : "none";
                                document.getElementById("attachments-filter").value = document.getElementById("attachments-filter").value || "*.*";
                                document.getElementById("testcase-field-id-container").style.display = this.checked ? "block" : "none";
                                document.getElementById("embed-attachments-label").style.display = this.checked ? "block" : "none";' type='checkbox'/>
                        Download attachments
                    </label>
                </div>
                <div class='input-group' id="attachments-filter-container" style="display: none;">
                    <label for="attachments-filter">Attachments filter:</label>
                    <input id='attachments-filter' title="Filter for attachments to be downloaded, example: '*.pdf'" placeholder='*.*' type='text' />
                </div>
            </div>
            <div class="flex-column">
                <div class='checkbox input-group'>
                    <label for='embed-attachments' id='embed-attachments-label' style="margin-top: 12px;">
                        <input id='embed-attachments' type='checkbox'/>
                        Embed attachments into resulted PDF
                    </label>
                </div>
                <div class='input-group' id="testcase-field-id-container" style="display: none;">
                    <label for="testcase-field-id">Custom field ID:</label>
                    <input id='testcase-field-id' title='A boolean testcase field ID. Attachments will be downloaded only from the testcases which have True value in the provided field. Leaving field empty will process all testcases.' type='text' />
                </div>
            </div>
        </div>

        <h2 class="align-left">PDF Exporter dialog configuration</h2>
        <div class="flex-container">
            <div class="flex-column">
                <div class='checkbox input-group'>
                    <label for='expose-page-width-validation'>
                        <input id='expose-page-width-validation' type='checkbox'/>
                        Expose page width validation controls
                    </label>
                </div>
            </div>
        </div>
    </div>

    <input id="scope" type="hidden" value="<%= request.getParameter("scope")%>"/>
    <input id="bundle-timestamp" type="hidden" value="<%= ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestamp() %>"/>
</div>

<jsp:include page='/common/jsp/buttons.jsp'/>

</body>
</html>
