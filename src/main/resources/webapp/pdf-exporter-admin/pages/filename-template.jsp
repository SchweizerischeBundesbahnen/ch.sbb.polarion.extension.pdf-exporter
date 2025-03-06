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
    <link rel="stylesheet" href="../css/tabs.css?bundle=<%= bundleTimestamp %>">
    <script type="module" src="../js/modules/filename-template.js?bundle=<%= bundleTimestamp %>"></script>
    <script type="text/javascript" defer src="../js/prism-velocity.min.js?bundle=<%= bundleTimestamp %>"></script>

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

        .w-32 {
            width: 32%;
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

    </style>
</head>

<body>
<div class="standard-admin-page">
    <h1>PDF Exporter: Filename template</h1>
    <jsp:include page='/common/jsp/notifications.jsp' />

    <div class="flex-container">
        <div class="flex-column">
            <div class='checkbox input-group'>
                <label for='use-custom-values'>
                    <input id='use-custom-values' type='checkbox'/>
                    Use custom templates
                </label>
            </div>
        </div>
    </div>

    <div class="tabbed">
        <input type="radio" id="custom-templates" name="file-templates" checked>
        <input type="radio" id="default-templates" name="file-templates">

        <ul class="tabs">
            <li class="tab"><label for="custom-templates">Custom Templates</label></li>
            <li class="tab"><label for="default-templates">Default Templates</label></li>
        </ul>

        <div class="tab-content">
            <p style="margin: 20px 25px 30px">
                Here you can define your custom filename templates, and force them to be used instead of default ones by ticking checkbox above.
            </p>
            <div class="input-container">
                <div class="input-block left w-32">
                    <div class="label-block"><span>Document filename template:</span></div>
                    <code-input class="html-input" id="custom-document-name-template" lang="velocity" placeholder="Enter file name template for exported Live Document"></code-input>
                </div>

                <div class="input-block w-32">
                    <div class="label-block"><span>Report filename template:</span></div>
                    <code-input class="html-input" id="custom-report-name-template" lang="velocity" placeholder="Enter file name template for exported Live Report"></code-input>
                </div>

                <div class="input-block right w-32">
                    <div class="label-block"><span>Test run filename template:</span></div>
                    <code-input class="html-input" id="custom-testrun-name-template" lang="velocity" placeholder="Enter file name template for exported Test Run"></code-input>
                </div>
            </div>
        </div>
        <div class="tab-content">
            <p style="margin: 20px 25px 30px">
                Here are displayed default filename templates, which will be used unless checkbox above is ticked. They are displayed here
                only for informational purposes, you can't modify them.
            </p>
            <div class="input-container">
                <div class="input-block left w-32">
                    <div class="label-block"><span>Document filename template:</span></div>
                    <code-input class="html-input" id="default-document-name-template" lang="velocity" placeholder=""></code-input>
                </div>

                <div class="input-block w-32">
                    <div class="label-block"><span>Report filename template:</span></div>
                    <code-input class="html-input" id="default-report-name-template" lang="velocity" placeholder=""></code-input>
                </div>

                <div class="input-block right w-32">
                    <div class="label-block"><span>Test run filename template:</span></div>
                    <code-input class="html-input" id="default-testrun-name-template" lang="velocity" placeholder=""></code-input>
                </div>
            </div>
        </div>
    </div>

    <input id="scope" type="hidden" value="<%= request.getParameter("scope")%>"/>
    <input id="bundle-timestamp" type="hidden" value="<%= ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestamp() %>"/>
</div>

<jsp:include page='/common/jsp/buttons.jsp'/>

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
</body>
</html>
