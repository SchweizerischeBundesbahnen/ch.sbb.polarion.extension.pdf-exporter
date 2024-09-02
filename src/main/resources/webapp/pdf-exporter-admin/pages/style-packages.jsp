<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%! String bundleTimestamp = ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestampDigitsOnly(); %>
<%! Boolean webhooksEnabled = ch.sbb.polarion.extension.pdf.exporter.properties.PdfExporterExtensionConfiguration.getInstance().getWebhooksEnabled(); %>

<head>
    <title>PDF Exporter: Style Packages</title>
    <link rel="stylesheet" href="../ui/generic/css/prism.css?bundle=<%= bundleTimestamp %>">
    <script type="text/javascript" src="../ui/generic/js/prism.js?bundle=<%= bundleTimestamp %>"></script>
    <link rel="stylesheet" href="../ui/generic/css/code-input.min.css?bundle=<%= bundleTimestamp %>">
    <script type="text/javascript" src="../ui/generic/js/code-input.min.js?bundle=<%= bundleTimestamp %>"></script>
    <link rel="stylesheet" href="../ui/generic/css/common.css?bundle=<%= bundleTimestamp %>">
    <link rel="stylesheet" href="../ui/generic/css/custom-select.css?bundle=<%= bundleTimestamp %>">
    <link rel="stylesheet" href="../ui/generic/css/configurations.css?bundle=<%= bundleTimestamp %>">
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

        <div class="flex-container" id="matching-query-container" style="border-top: 1px solid #ccc; margin-top: 20px; padding-top: 15px;">
            <p>Here you can specify a query to select documents to which this style package will be relevant. For documents not matching this query the style package won't be visible.
                If you want to make this style package be available to all documents, just leave this field empty.</p>
            <div class='input-group'>
                <label for='matching-query'>Matching query:</label>
                <input id='matching-query' style="width: 725px;"/>
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
                    <label for='headers-color'>Headers color:</label>
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
                    <label for='enable-comments-rendering'>
                        <input id="enable-comments-rendering" type='checkbox' />
                        Comments rendering
                    </label>
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
                    <input id='chapters' placeholder='eg. 1,2,4 etc.' type='text' style="visibility: hidden; margin-left: 10px; width: 117px"/>
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

<jsp:include page='/common/jsp/buttons.jsp'>
    <jsp:param name="saveFunction" value="saveStylePackage()"/>
    <jsp:param name="cancelFunction" value="SbbCommon.cancelEdit()"/>
    <jsp:param name="defaultFunction" value="revertToDefault()"/>
</jsp:include>

<script type="text/javascript" src="../ui/generic/js/common.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../ui/generic/js/custom-select.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../ui/generic/js/configurations.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../js/style-packages.js?bundle=<%= bundleTimestamp %>"></script>
</body>
</html>