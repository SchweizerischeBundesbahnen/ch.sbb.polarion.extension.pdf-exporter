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
            overflow-y: auto;
        }
        .input-block {
            height: 100%;
            display: flex;
            flex-direction: column;
        }

        /* Sortable weights list — flat Polarion 2606 look, built on the shared --sbb-* control tokens
           (control-tokens.css, imported by common.css) so it stays in sync with the rest of the UI. */
        .weights-list {
            list-style-type: none;
            padding: 0;
            margin: 0;
            max-width: 500px;
            border: 1px solid var(--sbb-control-border);
            border-radius: 3px;
            overflow: hidden;
        }

        .weight-item {
            display: flex;
            align-items: center;
            gap: 9px;
            padding: 7px 10px;
            background-color: #fff;
            border-bottom: 1px solid #ededed;
            cursor: grab;
            transition: background-color 0.12s ease, box-shadow 0.08s ease;
        }

        .weight-item:last-child {
            border-bottom: none;
        }

        .weight-item:hover {
            background-color: var(--sbb-hover-tint);
        }

        .weight-item.dragging {
            opacity: 0.4;
        }

        /* Insertion indicator: an accent line on the edge the dragged item would land on. */
        .weight-item.drop-above {
            box-shadow: inset 0 3px 0 var(--sbb-accent);
        }

        .weight-item.drop-below {
            box-shadow: inset 0 -3px 0 var(--sbb-accent);
        }

        .weight-item.static {
            background-color: #f7f7f5;
            color: #9a9a9a;
            cursor: default;
        }

        .weight-item .name {
            flex: 1;
            min-width: 0;
            font-weight: 600;
        }

        /* Left slot — drag handle (movable rows) or lock marker (read-only global rows). Both occupy
           the same 14px box so names line up across all rows. */
        .weight-item .drag-handle,
        .weight-item .lock-marker {
            flex: 0 0 auto;
            width: 14px;
            padding: 2px 0;
            box-sizing: content-box;
            display: inline-flex;
            align-items: center;
            justify-content: center;
            line-height: 0;
        }

        .weight-item .drag-handle {
            color: #9a9a9a;
            cursor: grab;
            border-radius: 3px;
        }

        .weight-item .drag-handle:hover {
            color: #555;
            background-color: var(--sbb-hover-tint);
        }

        .weight-item .lock-marker {
            color: #8a8a8a;
            cursor: help;
        }

        .weight-item .drag-handle svg,
        .weight-item .lock-marker svg {
            display: block;
        }

        .weights-list .sbb-number {
            flex: 0 0 auto;
            width: 66px;
        }

        .weight-item.static .weight-input {
            background-color: #f1f1f1;
            color: #9a9a9a;
            pointer-events: none;
        }

        /* Up / down reorder buttons — keyboard-accessible alternative to drag. */
        .reorder-arrows {
            flex: 0 0 auto;
            display: flex;
            flex-direction: column;
            gap: 2px;
        }

        .reorder-arrows.placeholder {
            width: 22px;
        }

        .reorder-arrows button {
            width: 22px;
            height: 13px;
            padding: 0;
            margin: 0;
            border: 1px solid var(--sbb-btn-border);
            border-radius: 2px;
            background-color: var(--sbb-btn-control-bg);
            color: #595959;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            line-height: 0;
        }

        .reorder-arrows button:hover:not(:disabled) {
            background-color: var(--sbb-hover-tint);
            border-color: var(--sbb-btn-hover-border);
        }

        .reorder-arrows button:disabled {
            opacity: 0.35;
            cursor: default;
        }

        .reorder-arrows svg {
            display: block;
            width: 9px;
            height: 6px;
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
