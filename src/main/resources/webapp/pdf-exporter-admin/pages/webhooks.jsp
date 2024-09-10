<%@ page import="ch.sbb.polarion.extension.pdf_exporter.properties.PdfExporterExtensionConfiguration" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%! String bundleTimestamp = ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestampDigitsOnly(); %>
<%! Boolean webhooksEnabled = PdfExporterExtensionConfiguration.getInstance().getWebhooksEnabled(); %>

<head>
    <title>PDF Exporter: Webhooks</title>
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

        #webhooks-table {
            width: 1100px;
            table-layout: fixed;
        }

        .webhook-row td {
            vertical-align: top;
        }

        .webhook-row td:first-child {
            width: 30px;
        }

        .webhook-row td:nth-child(2) {
            padding-top: 6px;
            width: 30px;
        }

        .webhook-row td:nth-child(3),
        .webhook-row td:nth-child(3) input {
            width: 400px;
        }


        .webhook-row td:nth-child(4) {
            padding-top: 4px;
            width: 55px;
            min-width: 55px;
        }

        .webhook-row td:nth-child(5) {
            width: 100px;
        }

        .webhook-row td:nth-child(6),
        .webhook-row td:nth-child(6) input {
            width: 400px;
        }

        .webhook-button {
            padding: 5px;
            width: 29px;
        }

        .invalid-webhook {
            color: #ddab19;
        }

        .invalid-webhook.hidden {
            visibility: hidden;
        }
    </style>
</head>

<body>
<div style="display: <%= webhooksEnabled ? "flex" : "none" %>; flex: 1; flex-direction: column">
    <div class="standard-admin-page">
        <h1>PDF Exporter: Webhooks</h1>

        <jsp:include page='/common/jsp/notifications.jsp'/>

        <jsp:include page='/common/jsp/configurations.jsp'/>

        <h2 class="align-left">List of webhooks <%= PdfExporterExtensionConfiguration.getInstance().getWebhooksEnabled() %></h2>
        <table id="webhooks-table"><!-- Filled by JS --></table>
        <button class="toolbar-button webhook-button" onclick="Webhooks.addWebhook()" title="Add a webhook" style="margin-top: 10px; margin-left: 3px;">
            <img src='/polarion/ria/images/control/tablePlus.png' alt="Plus">
        </button>

        <input id="scope" type="hidden" value="<%= request.getParameter("scope")%>"/>
        <input id="bundle-timestamp" type="hidden" value="<%= ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestamp() %>"/>
    </div>

    <jsp:include page='/common/jsp/buttons.jsp'>
        <jsp:param name="saveFunction" value="Webhooks.saveWebhooks()"/>
        <jsp:param name="cancelFunction" value="SbbCommon.cancelEdit()"/>
        <jsp:param name="defaultFunction" value="Webhooks.revertToDefault()"/>
    </jsp:include>

    <div class="standard-admin-page help">
        <h2 class="align-left">Quick Help</h2>

        <div>
            <p>On this page you can add, edit or remove a webhook applied to selected configuration.</p>

            <h3>What is a webhook</h3>
            <p>
                A webhook is a REST endpoint accepting initial HTML as a string (POST request), making some modification to this HTML and returning resulting HTML as a string back in body of response.
                A webhook endpoint can locate anywhere, either within Polarion itself or outside of it.
            </p>
            <h3>Webhook configuration</h3>
            <p>
                Each webhook has an URL and optional auth info. The URL is the endpoint to invoke. The auth info is a authentication for this endpoint.
                The auth info can be either a basic auth with username and password or a Bearer token. The auth info should be stored in Polarion Vault.
                Here should be provided a name of the vault entry with auth info.
            </p>
            <h3>Webhooks processing</h3>
            <p>
                Webhooks to run they should be selected in appropriate style package, or during PDF exporting. They are invoked in an order they entered on this page.
                If certain webhook fails with an error, it's just skipped, remaining webhooks will still be invoked.
            </p>
        </div>
    </div>
</div>
<div style="display: <%= webhooksEnabled ? "none" : "block" %>">
    <h1>PDF Exporter: Webhooks</h1>
    <p style="font-style: italic; color: palevioletred;">Webhooks are not enabled. Please contact system administrator if this functionality should be available.</p>
</div>

<script type="text/javascript" src="../ui/generic/js/common.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../ui/generic/js/custom-select.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../ui/generic/js/configurations.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../js/webhooks.js?bundle=<%= bundleTimestamp %>"></script>
</body>
</html>