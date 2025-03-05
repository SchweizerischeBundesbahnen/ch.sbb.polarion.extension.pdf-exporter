<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%! String bundleTimestamp = ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestampDigitsOnly(); %>

<head>
    <title>PDF Exporter: Cover Page</title>
    <link rel="stylesheet" href="../ui/generic/css/prism.css?bundle=<%= bundleTimestamp %>">
    <script type="text/javascript" src="../ui/generic/js/prism.js?bundle=<%= bundleTimestamp %>"></script>
    <link rel="stylesheet" href="../ui/generic/css/code-input.min.css?bundle=<%= bundleTimestamp %>">
    <script type="text/javascript" src="../ui/generic/js/code-input.min.js?bundle=<%= bundleTimestamp %>"></script>
    <link rel="stylesheet" href="../ui/generic/css/common.css?bundle=<%= bundleTimestamp %>">
    <link rel="stylesheet" href="../ui/generic/css/custom-select.css?bundle=<%= bundleTimestamp %>">
    <link rel="stylesheet" href="../ui/generic/css/configurations.css?bundle=<%= bundleTimestamp %>">
    <script type="module" src="../js/modules/cover-page.js?bundle=<%= bundleTimestamp %>"></script>
    <style type="text/css">
        html {
            height: 100%;
        }

        body {
            font-size: 13px;
        }

        table {
            border-collapse: collapse;
        }
        table th {
            height: 12px;
            padding: 5px;
            text-align: left;
            border: 1px solid #CCCCCC;
            font-weight: bold;
            vertical-align: top;
            white-space: nowrap;
            background-color: #F0F0F0;
        }
        table td {
            height: 12px;
            text-align: left;
            vertical-align: top;
            line-height: 18px;
            border: 1px solid #CCCCCC;
            padding: 5px;
        }
        table td:first-child {
            padding-left: 10px
        }

        .standard-admin-page {
            flex: 1;
            display: flex;
            flex-direction: column;
        }
        .standard-admin-page.help {
            display: unset;
        }

        .w-48 {
            width: 48%;
        }
    </style>
</head>

<body>
<div class="standard-admin-page">
    <h1>PDF Exporter: Cover Page</h1>

    <jsp:include page='/common/jsp/notifications.jsp' />

    <jsp:include page='/common/jsp/configurations.jsp' />

    <h2 class="align-left">Cover Page Template</h2>
    <div class="input-container">
        <div class="input-block left w-48">
            <div class="label-block"><span>Template HTML</span></div>
            <code-input class="html-input" id="template-html-input" lang="HTML" placeholder=""></code-input>
        </div>
        <div class="input-block right w-48">
            <div class="label-block"><span>Template CSS</span></div>
            <code-input class="html-input" id="template-css-input" lang="CSS" placeholder=""></code-input>
        </div>
    </div>

    <input id="scope" type="hidden" value="<%= request.getParameter("scope")%>"/>
    <input id="bundle-timestamp" type="hidden" value="<%= ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestamp() %>"/>
</div>

<jsp:include page='/common/jsp/buttons.jsp'/>

<div class="standard-admin-page hide-on-edit-configuration" id="templates-pane" style="display: none">
    <h2 class="align-left">Predefined Templates</h2>
    <div>
        <p>In addition to a default template, there are more predefined ones which you can persist. Please select one from the dropdown below and click 'Persist' button.</p>

        <label for="templates-select" id="templates-label">Predefined Templates:</label>
        <div id="templates-select"></div>
        <div class="action-buttons">
            <button id="persist-selected-template" class="toolbar-button">
                <img class="button-image" src="/polarion/ria/images/actions/save.gif?bundle=<%= bundleTimestamp %>" alt="">Persist
            </button>
        </div>
        <div class="action-alerts" style="display: inline-block">
            <div id="templates-load-error" class="alert alert-error" style="display: none">
                There was an error loading template names.
            </div>
            <div id="template-save-error" class="alert alert-error" style="display: none">
                Error occurred when persisting selected template.
            </div>
            <div id="template-save-success" class="alert alert-success" style="display: none">
                Selected template successfully persisted.
            </div>
        </div>
    </div>
</div>

<div class="standard-admin-page help">
    <h2 class="align-left">Quick Help</h2>

    <div>
        <h3>How-to configure PDF cover page</h3>
        <p>PDF cover page can be configured using HTML and CSS where you can insert special variables (upper case, exactly like in table below)
            and document's custom fields (case-sensitive custom field ID, exactly how it's configured in administration pane), both enclosed in double curly brackets,
            eg.: {{ DOCUMENT_TITLE }} for special variables or {{ docRevision }} for document's custom fields.</p>
        <p>During PDF generation provided special variables and/or custom fields of a document will be replaced with actual values.</p>
        <p>Cover page can contain velocity expressions which will be evaluated during PDF generation. For example: $document.getId()</p>

        <jsp:include page="../pages/placeholders.jsp"/>
    </div>
</div>
</body>
</html>
