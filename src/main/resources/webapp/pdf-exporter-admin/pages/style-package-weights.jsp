<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%! String bundleTimestamp = ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestampDigitsOnly(); %>

<head>
    <title>PDF Exporter: CSS</title>
    <link rel="stylesheet" href="../ui/generic/css/common.css?bundle=<%= bundleTimestamp %>">
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
            overflow-y: scroll;
        }
        .input-block {
            height: 100%;
            display: flex;
            flex-direction: column;
        }
        .sortable-list {
            flex: 1;
        }

        .sortable-list {
            list-style-type: none;
            padding: 0;
            margin: 0;
            max-width: 500px;
            border: 1px solid #ccc;
            border-radius: 5px;
        }

        .sortable-item {
            padding: 10px;
            margin: 5px 0;
            background-color: #f0f0f0;
            border: 1px solid #ddd;
            cursor: move;
            text-align: center;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .sortable-item.static {
            cursor: not-allowed;
        }

        .sortable-item.static input {
            pointer-events:none;
            background-color: #f1f1f1;
        }

        .weight-input {
            width: 60px;
            text-align: center;
        }
    </style>
</head>

<body>
<div class="standard-admin-page">
    <h1>PDF Exporter: Style Package Weights</h1>
    <span>The higher the number, the higher resulting item's position will be. The highest item will be pre-selected in the dropdown on the export panel.</span>
    <div class="input-container">
        <div class="input-block wide">
            <ul id="sortable-list" class="sortable-list"></ul>
        </div>
    </div>

    <input id="scope" type="hidden" value="<%= request.getParameter("scope")%>"/>
    <input id="bundle-timestamp" type="hidden" value="<%= ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestamp() %>"/>
</div>

<jsp:include page='/common/jsp/buttons.jsp'>
    <jsp:param name="saveFunction" value="StylePackageWeights.saveWeights()"/>
    <jsp:param name="cancelFunction" value="StylePackageWeights.loadPackageList()"/>
</jsp:include>

<script type="text/javascript">
    document.getElementById("default-toolbar-button").style.display = "none";
    document.getElementById("revisions-toolbar-button").style.display = "none";
</script>

<script type="text/javascript" src="../ui/generic/js/common.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../js/style-package-utils.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../js/sortable.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../js/style-package-weights.js?bundle=<%= bundleTimestamp %>"></script>
</body>
</html>