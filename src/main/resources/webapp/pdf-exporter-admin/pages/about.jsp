<%@ page import="ch.sbb.polarion.extension.generic.properties.CurrentExtensionConfiguration" %>
<%@ page import="ch.sbb.polarion.extension.generic.rest.model.Version" %>
<%@ page import="ch.sbb.polarion.extension.generic.util.ExtensionInfo" %>
<%@ page import="ch.sbb.polarion.extension.generic.util.VersionUtils" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Properties" %>
<%@ page import="java.io.InputStream" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="ch.sbb.polarion.extension.generic.rest.model.Context" %>
<%@ page import="ch.sbb.polarion.extension.pdf.exporter.util.configuration.ConfigurationStatusUtils" %>
<%@ page import="ch.sbb.polarion.extension.pdf.exporter.rest.model.configuration.ConfigurationStatus" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%!
    private static final String ABOUT_TABLE_ROW = "<tr><td>%s</td><td>%s</td></tr>";
    private static final String CONFIGURATION_PROPERTIES_TABLE_ROW = "<tr><td>%s</td><td>%s</td></tr>";
    private static final String CHECK_CONFIGURATION_TABLE_ROW = "<tr><td>%s</td><td>%s</td><td>%s</td></tr>";

    Context context = ExtensionInfo.getInstance().getContext();
    Version version = ExtensionInfo.getInstance().getVersion();
    Properties properties = CurrentExtensionConfiguration.getInstance().getExtensionConfiguration().getProperties();
%>

<head>
    <title></title>
    <link rel="stylesheet" href="../ui/generic/css/common.css?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>">
    <link rel="stylesheet" href="../ui/generic/css/about.css?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>">
    <link rel="stylesheet" href="../ui/generic/css/github-markdown-light.css?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>">
</head>

<body>
<div class="standard-admin-page about-page">
    <h1>About</h1>

    <div class="about-page-text">
        <img class="app-icon" src="../ui/images/app-icon.svg?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>" alt="" onerror="this.style.display='none'"/>

        <h3>Extension info</h3>

        <table>
            <thead>
            <tr>
                <th>Manifest entry</th>
                <th>Value</th>
            </tr>
            </thead>
            <tbody>
            <%
                out.println(ABOUT_TABLE_ROW.formatted(VersionUtils.BUNDLE_NAME, version.getBundleName()));
                out.println(ABOUT_TABLE_ROW.formatted(VersionUtils.BUNDLE_VENDOR, version.getBundleVendor()));
                if (version.getSupportEmail() != null) {
                    String mailToLink = "<a target=\"_blank\" href=\"mailto:%s\">%s</a>".formatted(version.getSupportEmail(), version.getSupportEmail());
                    out.println(ABOUT_TABLE_ROW.formatted(VersionUtils.SUPPORT_EMAIL, mailToLink));
                }
                out.println(ABOUT_TABLE_ROW.formatted(VersionUtils.AUTOMATIC_MODULE_NAME, version.getAutomaticModuleName()));
                out.println(ABOUT_TABLE_ROW.formatted(VersionUtils.BUNDLE_VERSION, version.getBundleVersion()));
                out.println(ABOUT_TABLE_ROW.formatted(VersionUtils.BUNDLE_BUILD_TIMESTAMP, version.getBundleBuildTimestamp()));
            %>
            </tbody>
        </table>

        <h3>Extension configuration properties</h3>

        <table>
            <thead>
            <tr>
                <th>Configuration property</th>
                <th>Value</th>
            </tr>
            </thead>
            <tbody>
            <%
                Set<Object> keySet = properties.keySet();
                List<String> propertyNames = new ArrayList<>();
                for (Object key : keySet) {
                    propertyNames.add((String) key);
                }
                Collections.sort(propertyNames);

                for (String key : propertyNames) {
                    String value = properties.getProperty(key);
                    String row = CONFIGURATION_PROPERTIES_TABLE_ROW.formatted(key, value);
                    out.println(row);
                }
            %>
            </tbody>
        </table>

        <h3>Extension configuration status</h3>

        <table>
            <thead>
            <tr>
                <th>Configuration</th>
                <th>Status</th>
                <th>Info</th>
            </tr>
            </thead>
            <tbody>
            <%
                String scope = request.getParameter("scope");
                ConfigurationStatus settingsStatus = ConfigurationStatusUtils.getSettingsStatus(scope);
                ConfigurationStatus documentPropertiesPaneStatus = ConfigurationStatusUtils.getDocumentPropertiesPaneStatus(scope);
                ConfigurationStatus dleToolbarStatus = ConfigurationStatusUtils.getDleToolbarStatus();
                ConfigurationStatus liveReportMainHeadStatus = ConfigurationStatusUtils.getLiveReportMainHeadStatus();
                ConfigurationStatus weasyPrintStatus = ConfigurationStatusUtils.getWeasyPrintStatus();
                ConfigurationStatus weasyPrintServiceStatus = ConfigurationStatusUtils.getWeasyPrintServiceStatus();
                ConfigurationStatus corsStatus = ConfigurationStatusUtils.getCORSStatus();

                List<String> rows = new ArrayList<>(4);
                rows.add(CHECK_CONFIGURATION_TABLE_ROW.formatted("Default Settings", settingsStatus.getStatus().toHtml(), settingsStatus.getDetails()));
                rows.add(CHECK_CONFIGURATION_TABLE_ROW.formatted("Document Properties Pane", documentPropertiesPaneStatus.getStatus().toHtml(), documentPropertiesPaneStatus.getDetails()));
                rows.add(CHECK_CONFIGURATION_TABLE_ROW.formatted("DLE Toolbar", dleToolbarStatus.getStatus().toHtml(), dleToolbarStatus.getDetails()));
                rows.add(CHECK_CONFIGURATION_TABLE_ROW.formatted("LiveReport Button", liveReportMainHeadStatus.getStatus().toHtml(), liveReportMainHeadStatus.getDetails()));
                rows.add(CHECK_CONFIGURATION_TABLE_ROW.formatted("WeasyPrint", weasyPrintStatus.getStatus().toHtml(), weasyPrintStatus.getDetails()));
                rows.add(CHECK_CONFIGURATION_TABLE_ROW.formatted("WeasyPrint Service", weasyPrintServiceStatus.getStatus().toHtml(), weasyPrintServiceStatus.getDetails()));
                rows.add(CHECK_CONFIGURATION_TABLE_ROW.formatted("CORS (Cross-Origin Resource Sharing)", corsStatus.getStatus().toHtml(), corsStatus.getDetails()));

                for (String row : rows) {
                    out.println(row);
                }
            %>
            </tbody>
        </table>

        <input id="scope" type="hidden" value="<%= request.getParameter("scope")%>"/>

        <article class="markdown-body">
            <%
                String extensionContext = context.getExtensionContext();
                try (InputStream inputStream = ExtensionInfo.class.getResourceAsStream("/webapp/" + extensionContext + "-admin/html/about.html")) {
                    if (inputStream != null) {
                        String configurationHelp = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        out.println(configurationHelp);
                    } else {
                        out.println("No help has been generated during build. Please check <a href=\"" + version.getProjectURL() + "/README.md\" target=\"_blank\">the online documentation</a>.");
                    }
                }
            %>
        </article>
    </div>
</div>
</body>
</html>
