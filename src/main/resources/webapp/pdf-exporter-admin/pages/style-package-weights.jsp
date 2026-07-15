<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%! String bundleTimestamp = ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestampDigitsOnly(); %>

<head>
    <title>PDF Exporter: CSS</title>
    <link rel="stylesheet" href="../ui/generic/css/common.css?bundle=<%= bundleTimestamp %>">
    <link rel="stylesheet" href="../ui/generic/css/style-package-weights.css?bundle=<%= bundleTimestamp %>">
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
            overflow: hidden;
        }
        .input-container {
            flex: 1;
            margin-top: 20px;
            overflow-y: auto;
        }
        .input-block {
            height: 100%;
            display: flex;
            flex-direction: column;
        }
    </style>
    <script type="module" src="../js/modules/style-package-weights.js?bundle=<%= bundleTimestamp %>"></script>
</head>

<body>
<div class="standard-admin-page">
    <h1>PDF Exporter: Style Package Weights</h1>
    <span>The higher the number, the higher resulting item's position will be. The highest item will be pre-selected in the dropdown on the export panel.</span>
    <div class="input-container">
        <div class="input-block wide">
            <ul id="sortable-list" class="weights-list"></ul>
        </div>
    </div>

    <input id="scope" type="hidden" value="<%= request.getParameter("scope")%>"/>
    <input id="bundle-timestamp" type="hidden" value="<%= ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestamp() %>"/>
</div>

<jsp:include page='/common/jsp/buttons.jsp'/>

</body>
</html>
