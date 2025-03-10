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
    <link rel="stylesheet" href="../css/tabs.css?bundle=<%= bundleTimestamp %>">
    <script type="module" src="../js/modules/header-footer.js?bundle=<%= bundleTimestamp %>"></script>
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
        .standard-admin-page h2 {
            margin: 24px 24px 12px;
            padding: 8px 10px;
        }
        .standard-admin-page.help {
            display: unset;
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
        code-input.code-input_pre-element-styled {
            height: 220px;
        }
    </style>
</head>

<body>
<div class="standard-admin-page">
    <h1>PDF Exporter: Header/Footer</h1>

    <jsp:include page='/common/jsp/notifications.jsp' />

    <jsp:include page='/common/jsp/configurations.jsp' />

    <div class="flex-container" style="border-top: 1px solid #ccc; margin-top: 20px; padding-top: 15px;">
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
        <input type="radio" id="custom-templates" name="cover-page-template" checked>
        <input type="radio" id="default-templates" name="cover-page-template">

        <ul class="tabs">
            <li class="tab"><label for="custom-templates">Custom Templates</label></li>
            <li class="tab"><label for="default-templates">Default Templates</label></li>
        </ul>

        <div class="tab-content">
            <p style="margin: 20px 25px 30px">
                Here you can define your custom header and footer templates, and force them to be used instead of default ones by ticking checkbox above.
            </p>
            <h2 class="align-left">Header</h2>
            <div class="input-container">
                <div class="input-block left">
                    <div class="label-block"><span>Left</span></div>
                    <code-input class="html-input" id="custom-top-left" lang="HTML" placeholder="Enter template of header's left part here"></code-input>
                </div>
                <div class="input-block">
                    <div class="label-block"><span>Center</span></div>
                    <code-input class="html-input" id="custom-top-center" lang="HTML" placeholder="Enter template of header's center part here"></code-input>
                </div>
                <div class="input-block right">
                    <div class="label-block"><span>Right</span></div>
                    <code-input class="html-input" id="custom-top-right" lang="HTML" placeholder="Enter template of header's right part here"></code-input>
                </div>
            </div>

            <h2 class="align-left">Footer</h2>
            <div class="input-container">
                <div class="input-block left">
                    <div class="label-block"><span>Left</span></div>
                    <code-input class="html-input" id="custom-bottom-left" lang="HTML" placeholder="Enter template of footer's left part here"></code-input>
                </div>
                <div class="input-block">
                    <div class="label-block"><span>Center</span></div>
                    <code-input class="html-input" id="custom-bottom-center" lang="HTML" placeholder="Enter template of footer's center part here"></code-input>
                </div>
                <div class="input-block right">
                    <div class="label-block"><span>Right</span></div>
                    <code-input class="html-input" id="custom-bottom-right" lang="HTML" placeholder="Enter template of footer's right part here"></code-input>
                </div>
            </div>
        </div>
        <div class="tab-content">
            <p style="margin: 20px 25px 30px">
                Here are displayed default header and footer templates, which will be used unless checkbox above is ticked. They are displayed here
                only for informational purposes and can't be modified.
            </p>
            <h2 class="align-left">Header</h2>
            <div class="input-container">
                <div class="input-block left">
                    <div class="label-block"><span>Left</span></div>
                    <code-input class="html-input" id="default-top-left" lang="HTML" readonly></code-input>
                </div>
                <div class="input-block">
                    <div class="label-block"><span>Center</span></div>
                    <code-input class="html-input" id="default-top-center" lang="HTML" readonly></code-input>
                </div>
                <div class="input-block right">
                    <div class="label-block"><span>Right</span></div>
                    <code-input class="html-input" id="default-top-right" lang="HTML" readonly></code-input>
                </div>
            </div>

            <h2 class="align-left">Footer</h2>
            <div class="input-container">
                <div class="input-block left">
                    <div class="label-block"><span>Left</span></div>
                    <code-input class="html-input" id="default-bottom-left" lang="HTML" readonly></code-input>
                </div>
                <div class="input-block">
                    <div class="label-block"><span>Center</span></div>
                    <code-input class="html-input" id="default-bottom-center" lang="HTML" readonly></code-input>
                </div>
                <div class="input-block right">
                    <div class="label-block"><span>Right</span></div>
                    <code-input class="html-input" id="default-bottom-right" lang="HTML" readonly></code-input>
                </div>
            </div>

        </div>
    </div>

    <input id="scope" type="hidden" value="<%= request.getParameter("scope")%>"/>
    <input id="bundle-timestamp" type="hidden" value="<%= ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestamp() %>"/>
</div>

<jsp:include page='/common/jsp/buttons.jsp'/>

<div class="standard-admin-page help">
    <h2 class="align-left" style="margin: 24px 0 10px; padding: 8px 14px;">Quick Help</h2>

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
</body>
</html>
