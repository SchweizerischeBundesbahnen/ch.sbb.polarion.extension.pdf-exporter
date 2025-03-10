<!DOCTYPE html>
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
    <link rel="stylesheet" href="../css/tabs.css?bundle=<%= bundleTimestamp %>">
    <script type="module" src="../js/modules/css.js?bundle=<%= bundleTimestamp %>"></script>
    <style>
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
            width: calc(100% - 16px);
        }
        .flex-container {
            display: flex;
            column-gap: 20px;
            flex-wrap: wrap;
        }
        .flex-column {
            width: 440px;
        }
        input[type="checkbox"] {
            width: auto;
            vertical-align: middle;
        }
        .checkbox.input-group label {
            width: auto;
        }
        .input-container {
            flex: 1;
            margin-top: 20px;
            height: 100%;
        }
        .input-block {
            height: 100%;
            display: flex;
            flex-direction: column;
            border: 1px solid #ddd;
            padding: 0;
        }
        .input-block.wide {
            width: 100%;
        }
        .input-container .input-block:last-child {
            padding-right: 0;
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

    <div class="flex-container" style="border-top: 1px solid #ccc; margin-top: 20px; padding-top: 15px;">
        <div class="flex-column">
            <div class='checkbox input-group'>
                <label for='disable-default-css'>
                    <input id='disable-default-css' type='checkbox'/>
                    Disable usage of default CSS
                </label>
            </div>
        </div>
    </div>

    <div class="tabbed">
        <input type="radio" id="custom-css" name="css" checked>
        <input type="radio" id="default-css" name="css">

        <ul class="tabs">
            <li class="tab"><label for="custom-css">Custom CSS</label></li>
            <li class="tab"><label for="default-css">Default CSS</label></li>
        </ul>

        <div class="tab-content">
            <p>
                Here you can define your custom CSS, which will be appended to the end of resulting CSS. This means that you can add additional styling to default one or even
                overwrite it. Also be aware that if default CSS is disabled then your custom CSS is totally responsible for how resulting PDF will look like.
            </p>
            <div class="input-container">
                <div class="input-block wide">
                    <code-input class="html-input" id="custom-css-input" lang="css" placeholder="Enter your custom CSS here"></code-input>
                </div>
            </div>
        </div>
        <div class="tab-content">
            <p>
                This is a default CSS, which covers most common cases to generate well looking PDF from Polarion documents, reports etc.
                It's not editable and shown here only for your information. If you need to customize something please add this using editor on "Custom CSS" tab.
                Also be aware that you can totally disable default CSS clicking checkbox above.
            </p>
            <div class="input-container">
                <div class="input-block wide">
                    <code-input class="html-input" id="default-css-input" lang="css" readonly></code-input>
                </div>
            </div>
        </div>
    </div>

    <input id="scope" type="hidden" value="<%= request.getParameter("scope")%>"/>
    <input id="bundle-timestamp" type="hidden" value="<%= ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestamp() %>"/>
</div>

<jsp:include page='/common/jsp/buttons.jsp' />

</body>
</html>
