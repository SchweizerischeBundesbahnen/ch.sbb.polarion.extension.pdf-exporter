<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%! String bundleTimestamp = ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestampDigitsOnly(); %>

<head>
    <title>PDF Exporter: Filename template</title>
    <link rel="stylesheet" href="../ui/generic/css/prism.css?bundle=<%= bundleTimestamp %>">
    <script type="text/javascript" src="../ui/generic/js/prism.js?bundle=<%= bundleTimestamp %>"></script>
    <link rel="stylesheet" href="../ui/generic/css/code-input.min.css?bundle=<%= bundleTimestamp %>">
    <script type="text/javascript" src="../ui/generic/js/code-input.min.js?bundle=<%= bundleTimestamp %>"></script>
    <link rel="stylesheet" href="../ui/generic/css/common.css?bundle=<%= bundleTimestamp %>">

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

        .w-48 {
            width: 48%;
        }
    </style>
</head>

<body>
<div class="standard-admin-page">
    <h1>PDF Exporter: Filename template</h1>
    <jsp:include page='/common/jsp/notifications.jsp' />

    <div class="input-container">
        <div class="input-block left w-48">
            <div class="label-block"><label>Document filename template:</label></div>
            <code-input class="html-input" id="document-name-template" lang="velocity" placeholder=""></code-input>
        </div>

        <div class="input-block right w-48">
            <div class="label-block"><label>Report filename template:</label></div>
            <code-input class="html-input" id="report-name-template" lang="velocity" placeholder=""></code-input>
        </div>
    </div>

    <input id="scope" type="hidden" value="<%= request.getParameter("scope")%>"/>
    <input id="bundle-timestamp" type="hidden" value="<%= ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestamp() %>"/>
</div>

<jsp:include page='/common/jsp/buttons.jsp'>
    <jsp:param name="saveFunction" value="saveSettings()"/>
    <jsp:param name="cancelFunction" value="SbbCommon.cancelEdit()"/>
    <jsp:param name="defaultFunction" value="revertToDefault()"/>
</jsp:include>

<div class="standard-admin-page help">
    <h2 class="align-left">Quick Help</h2>

    <div>
        <h3>How to configure Filename template</h3>
        <p>
            Filenames can be made scriptable by incorporating placeholders and velocity code. These filenames can contain Velocity expressions that are dynamically evaluated, allowing for the inclusion of dynamic values.
        </p>
        <p>
            For example: {{ PROJECT_NAME }} $page.spaceId $page.titleOrName $page.lastRevision
        </p>
        <jsp:include page="../pages/placeholders.jsp"/>
    </div>
</div>

<script type="text/javascript" src="../ui/generic/js/common.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../js/filename-template.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../js/prism-velocity.min.js?bundle=<%= bundleTimestamp %>"></script>
</body>
</html>