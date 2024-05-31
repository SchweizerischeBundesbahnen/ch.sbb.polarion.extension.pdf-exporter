<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%! String bundleTimestamp = ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestampDigitsOnly(); %>

<head>
    <title>PDF Exporter: Localization</title>
    <link rel="stylesheet" href="../ui/generic/css/common.css?bundle=<%= bundleTimestamp %>">
    <link rel="stylesheet" href="../ui/generic/css/custom-select.css?bundle=<%= bundleTimestamp %>">
    <link rel="stylesheet" href="../ui/generic/css/configurations.css?bundle=<%= bundleTimestamp %>">
    <style type="text/css">

        .pdf-admin-text table {
            border-collapse: collapse;
        }

        .pdf-admin-text table th {
            height: 12px;
            padding: 5px;
            text-align: left;
            border: 1px solid #CCCCCC;
            font-weight: bold;
            vertical-align: top;
            white-space: nowrap;
            background-color: #F0F0F0;
        }

        .pdf-admin-text table td {
            height: 12px;
            text-align: left;
            vertical-align: top;
            line-height: 18px;
            border: 1px solid #CCCCCC;
            padding: 5px;
        }

        .pdf-admin-text table td:first-child {
            padding-left: 10px
        }

        input[type="file"] {
            display: none;
        }

        td.action {
            border: none !important;
            cursor: pointer;
            vertical-align: middle !important;
            text-align: center !important;
        }

        input.red-border {
            border: 1px solid red;
        }

        .w-100 {
            width: 100%;
        }

        div.w-100 input {
            box-sizing: border-box;
        }

        #translation-table table td {
            border-bottom: 1px solid #CCCCCC;
        }
    </style>
</head>

<body>
<div class="standard-admin-page">
    <h1>PDF Exporter: Localization</h1>

    <jsp:include page='/common/jsp/notifications.jsp' />

    <jsp:include page='/common/jsp/configurations.jsp' />

    <div id="translation-table" style="width: 100%; display: inline-block; margin-top: 15px" class="pdf-admin-text">
    </div>
    <table id="export-import-table" width="100%" style="border-collapse: collapse">
        <tr>
            <td style="width: 24%; text-align: center"></td>
            <td style="width: 24%; text-align: center">
                <span>
                    <button class="toolbar-button" id="lang-de" onclick="downloadLocalization('de')">Export</button>
                </span>
                <span>
                    <label for="file-de" class="toolbar-button label">Import</label>
                    <input id="file-de" name="file" type="file"/>
                </span>
            </td>
            <td style="width: 24%; text-align: center">
                <span>
                    <button class="toolbar-button" id="lang-fr" onclick="downloadLocalization('fr')">Export</button>
                </span>
                <span>
                    <label for="file-fr" class="toolbar-button label">Import</label>
                    <input id="file-fr" name="file" type="file"/>
                </span>
            </td>
            <td style="width: 24%; text-align: center">
                <span>
                    <button class="toolbar-button" id="lang-it" onclick="downloadLocalization('it')">Export</button>
                </span>
                <span>
                    <label for="file-it" class="toolbar-button label">Import</label>
                    <input id="file-it" name="file" type="file"/>
                </span>

            </td>
            <td class="action" onclick="createEmptyTableRow()" title="Add">
                <img src="/polarion/ria/images/control/tablePlus.png">
            </td>
        </tr>
    </table>

    <input id="scope" type="hidden" value="<%= request.getParameter("scope")%>"/>
</div>

<jsp:include page='/common/jsp/buttons.jsp'>
    <jsp:param name="saveFunction" value="saveLocalizations()"/>
    <jsp:param name="cancelFunction" value="SbbCommon.cancelEdit()"/>
    <jsp:param name="defaultFunction" value="revertToDefault()"/>
</jsp:include>

<div class="standard-admin-page">
    <h2 class="align-left">Quick Help</h2>

    <div class="pdf-admin-text">
        <h3>How-to configure localization</h3>

        <p>Supported localizations for workitems statuses and severities.</p>
        <p>Supported languages are German, French and Italian.</p>
        <p>Localization for each language can be imported in XLIFF 2.0 format <a href="https://docs.oasis-open.org/xliff/xliff-core/v2.0/os/xliff-core-v2.0-os.html" target="_blank">XLIFF 2.0 specification</a></p>

    </div>
</div>

<script type="text/javascript" src="../ui/generic/js/common.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../ui/generic/js/custom-select.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../ui/generic/js/configurations.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../js/localization.js?bundle=<%= bundleTimestamp %>"></script>
</body>
</html>