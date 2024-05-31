<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%! String bundleTimestamp = ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestampDigitsOnly(); %>

<head>
    <title>PDF Exporter: CSS</title>
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
        .input-container {
            flex: 1;
            margin-top: 20px;
        }
        .input-block {
            height: 100%;
            display: flex;
            flex-direction: column;
        }
        .html-input {
            flex: 1;
        }
    </style>
</head>

<body>
<div class="standard-admin-page">
    <h1>PDF Exporter: CSS</h1>

    <jsp:include page='/common/jsp/notifications.jsp' />

    <jsp:include page='/common/jsp/configurations.jsp' />

    <div class="input-container">
        <div class="input-block wide">
            <div class="label-block"><label>CSS</label></div>
            <code-input class="html-input" id="css-input" lang="css" placeholder=""></code-input>
        </div>
    </div>

    <input id="scope" type="hidden" value="<%= request.getParameter("scope")%>"/>
    <input id="bundle-timestamp" type="hidden" value="<%= ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestamp() %>"/>
</div>

<jsp:include page='/common/jsp/buttons.jsp'>
    <jsp:param name="saveFunction" value="saveCss()"/>
    <jsp:param name="cancelFunction" value="SbbCommon.cancelEdit()"/>
    <jsp:param name="defaultFunction" value="revertToDefault()"/>
</jsp:include>

<script type="text/javascript" src="../ui/generic/js/common.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../ui/generic/js/custom-select.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../ui/generic/js/configurations.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../js/css.js?bundle=<%= bundleTimestamp %>"></script>
</body>
</html>