<%@ page import="ch.sbb.polarion.extension.pdf_exporter.util.RolesUtils" %>
<%@ page import="java.util.Collection" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%! String bundleTimestamp = ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestampDigitsOnly(); %>

<head>
    <title>PDF Exporter: Authorization</title>
    <link rel="stylesheet" href="../ui/generic/css/common.css?bundle=<%= bundleTimestamp %>">
    <script type="module" src="../js/modules/authorization.js?bundle=<%= bundleTimestamp %>"></script>
    <style type="text/css">
        .roles_table {
            margin: 20px;
        }

        .roles_table table td {
            vertical-align: middle;
            padding: 3px;
        }
    </style>
</head>

<body>

<div class="standard-admin-page">
    <h1>PDF Exporter: Authorization</h1>

    <jsp:include page='/common/jsp/notifications.jsp'/>

    <div class="input-container">
        <div class="input-block wide">
            <div id="global_roles" class="roles_table">
                <table>
                    <%
                        Collection<String> globalRoles = RolesUtils.getGlobalRoles();
                        out.println("<b>Global Roles</b>");
                        for (String role : globalRoles) {
                            out.println(
                                    "<tr>" +
                                    "  <td><input type='checkbox' id='" + role + "' name='" + role + "'></td>" +
                                    "  <td><label for='" + role + "'>" + role + "</label></td>" +
                                    "</tr>");
                        }
                    %>
                </table>
            </div>

            <div id="project_roles" class="roles_table">
                <table>
                    <%
                        Collection<String> projectRoles = RolesUtils.getProjectRoles(request.getParameter("scope"));
                        if (!projectRoles.isEmpty()) {
                            out.println("<b>Project Roles</b>");
                        }
                        for (String role : projectRoles) {
                            out.println(
                                    "<tr>" +
                                    "  <td><input type='checkbox' id='" + role + "' name='" + role + "'></td>" +
                                    "  <td><label for='" + role + "'>" + role + "</label></td>" +
                                    "</tr>");
                        }
                    %>
                </table>
            </div>
        </div>
    </div>

    <input id="scope" type="hidden" value="<%= request.getParameter("scope")%>"/>
    <input id="bundle-timestamp" type="hidden" value="<%= ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestamp() %>"/>
</div>

<jsp:include page='/common/jsp/buttons.jsp'/>

<div class="standard-admin-page">
    <h2>Quick Help</h2>

    <div class="quick-help-text">
        <h3>Permissions</h3>
        <p>Here you can restrict who is allowed to export documents to PDF, based on Polarion global or project roles.</p>
        <p>When no role is selected the export is <b>unrestricted</b> and available to every user.</p>
        <p>As soon as at least one role is selected, only users holding one of the selected global roles or one of the selected roles within this project are allowed to export. All other users receive an authorization error.</p>
        <p>Project administrators can further tune the allowed roles for their specific project.</p>
    </div>
</div>
</body>
</html>
