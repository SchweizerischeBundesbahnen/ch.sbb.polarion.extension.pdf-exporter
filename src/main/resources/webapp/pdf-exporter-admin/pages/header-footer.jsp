<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%! String bundleTimestamp = ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestampDigitsOnly(); %>

<head>
    <title>PDF Exporter: Header/Footer</title>
    <link rel="stylesheet" href="../ui/generic/css/prism.css?bundle=<%= bundleTimestamp %>">
    <script type="text/javascript" src="../ui/generic/js/prism.js?bundle=<%= bundleTimestamp %>"></script>
    <link rel="stylesheet" href="../ui/generic/css/code-input.min.css?bundle=<%= bundleTimestamp %>">
    <script type="text/javascript" src="../ui/generic/js/code-input.min.js?bundle=<%= bundleTimestamp %>"></script>
    <link rel="stylesheet" href="../ui/generic/css/common.css?bundle=<%= bundleTimestamp %>">
    <link rel="stylesheet" href="../ui/generic/css/custom-select.css?bundle=<%= bundleTimestamp %>">
    <link rel="stylesheet" href="../ui/generic/css/configurations.css?bundle=<%= bundleTimestamp %>">
    <style type="text/css">
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
    </style>
</head>

<body>
<div class="standard-admin-page">
    <h1>PDF Exporter: Header/Footer</h1>

    <jsp:include page='/common/jsp/notifications.jsp' />

    <jsp:include page='/common/jsp/configurations.jsp' />

    <h2 class="align-left">Header Template</h2>
    <div class="input-container">
        <div class="input-block left">
            <div class="label-block"><span>Left</span></div>
            <code-input class="html-input" id="top-left" lang="HTML" placeholder=""></code-input>
        </div>
        <div class="input-block">
            <div class="label-block"><span>Center</span></div>
            <code-input class="html-input" id="top-center" lang="HTML" placeholder=""></code-input>
        </div>
        <div class="input-block right">
            <div class="label-block"><span>Right</span></div>
            <code-input class="html-input" id="top-right" lang="HTML" placeholder=""></code-input>
        </div>
    </div>

    <h2 class="align-left">Footer Template</h2>
    <div class="input-container">
        <div class="input-block left">
            <div class="label-block"><span>Left</span></div>
            <code-input class="html-input" id="bottom-left" lang="HTML" placeholder=""></code-input>
        </div>
        <div class="input-block">
            <div class="label-block"><span>Center</span></div>
            <code-input class="html-input" id="bottom-center" lang="HTML" placeholder=""></code-input>
        </div>
        <div class="input-block right">
            <div class="label-block"><span>Right</span></div>
            <code-input class="html-input" id="bottom-right" lang="HTML" placeholder=""></code-input>
        </div>
    </div>

    <input id="scope" type="hidden" value="<%= request.getParameter("scope")%>"/>
    <input id="bundle-timestamp" type="hidden" value="<%= ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestamp() %>"/>
</div>

<jsp:include page='/common/jsp/buttons.jsp'>
    <jsp:param name="saveFunction" value="saveHeaderFooter()"/>
    <jsp:param name="cancelFunction" value="SbbCommon.cancelEdit()"/>
    <jsp:param name="defaultFunction" value="revertToDefault()"/>
</jsp:include>

<div class="standard-admin-page help">
    <h2 class="align-left">Quick Help</h2>

    <div>
        <h3>How-to configure PDF header and footer</h3>
        <p>
            Header and footer divided into 3 parts: left, center and right sections. Each section can be configured using HTML,
            where you can insert special variables (upper case, exactly like in table below) and document's custom fields
            (case-sensitive custom field ID, exactly how it's configured in administration pane), both enclosed in double curly brackets,
            eg.: {{ DOCUMENT_TITLE }} for special variables or {{ docRevision }} for document's custom fields.
        </p>
        <p>During PDF generation provided special variables and/or custom fields of a document will be replaced with actual values.</p>
        <p>Header and footer parts can contain velocity expressions which will be evaluated during PDF generation. For example: $document.getId()</p>

        <jsp:include page="../pages/placeholders.jsp"/>
    </div>
</div>

<script type="text/javascript" src="../ui/generic/js/common.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../ui/generic/js/custom-select.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../ui/generic/js/configurations.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../js/header-footer.js?bundle=<%= bundleTimestamp %>"></script>
</body>
</html>