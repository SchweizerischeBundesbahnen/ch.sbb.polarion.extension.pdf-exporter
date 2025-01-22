<%@ page import="ch.sbb.polarion.extension.generic.rest.model.Context" %>
<%@ page import="ch.sbb.polarion.extension.generic.rest.model.Version" %>
<%@ page import="ch.sbb.polarion.extension.generic.util.ExtensionInfo" %>
<%@ page import="java.io.InputStream" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%!
    Context context = ExtensionInfo.getInstance().getContext();
    Version version = ExtensionInfo.getInstance().getVersion();
%>

<head>
    <title>Help</title>
    <link rel="stylesheet" href="../ui/generic/css/common.css?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>">
    <link rel="stylesheet" href="../ui/generic/css/github-markdown-light.css?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>">
    <link rel="stylesheet" href="../css/user-guide.css?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>">
</head>

<body>
<div class="standard-admin-page user-guide-page">
    <h1>Help</h1>

    <div>
        <article class="markdown-body">
            <%
                String extensionContext = context.getExtensionContext();
                try (InputStream inputStream = ExtensionInfo.class.getResourceAsStream("/webapp/" + extensionContext + "-admin/html/user-guide.html")) {
                    if (inputStream != null) {
                        out.println(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
                    } else {
                        out.println("No help has been generated during build. Please check <a href=\"" + version.getProjectURL() + "/USER_GUIDE.md\" target=\"_blank\">the online documentation</a>.");
                    }
                }
            %>
        </article>
    </div>
</div>
</body>
</html>
