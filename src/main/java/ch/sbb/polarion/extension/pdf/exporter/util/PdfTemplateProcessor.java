package ch.sbb.polarion.extension.pdf.exporter.util;

import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.ExportParams;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.Orientation;
import ch.sbb.polarion.extension.pdf.exporter.rest.model.conversion.PaperSize;
import com.polarion.core.boot.PolarionProperties;
import com.polarion.core.config.Configuration;
import com.polarion.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class PdfTemplateProcessor {

    private static final String LOCALHOST = "localhost";
    public static final String HTTP_PROTOCOL_PREFIX = "http://";
    public static final String HTTPS_PROTOCOL_PREFIX = "https://";

    @NotNull
    public String processUsing(@NotNull ExportParams exportParams, @NotNull String documentName, @NotNull String css, @NotNull String content) {
        if (exportParams.isWatermark()) {
            css += """
            @media print {
                body::before {
                  content: "Confidential";
                  font-size: 8em;
                  text-transform: uppercase;
                  color: rgba(255, 5, 5, 0.17);
                  position: fixed;
                  top: 50%;
                  left: 50%;
                  transform: translate(-50%, -50%) rotate(-45deg);
                }
            }
            """;
        }

        css += buildSizeCss(exportParams.getOrientation(), exportParams.getPaperSize());

        if (exportParams.isMarkReferencedWorkitems()) {
            css += """
                .polarion-dle-workitem-basic-external {
                    padding: 5px 10px;
                    border-left: 2px dashed #aaa;
                }
            """;
        }

        if (exportParams.isFitToPage()) {
            css += """
                img {
                    max-width: 100%;
                }
            """;
        }

        return ScopeUtils.getFileContent("webapp/pdf-exporter/html/pdfTemplate.html")
                .replace("{DOC_NAME}", documentName)
                .replace("{BASE_URL}", buildBaseUrlHeader())
                .replace("{CSS}", css)
                .replace("{DOC_CONTENT}", content);
    }

    public String buildSizeCss(Orientation orientation, PaperSize paperSize) {
        if ((paperSize != null && paperSize != PaperSize.A4)
                || (orientation != null && orientation != Orientation.PORTRAIT)) {
            return String.format(" @page {size: %s %s;}", Optional.ofNullable(paperSize).orElse(PaperSize.A4).toCssString(),
                    Optional.ofNullable(orientation).orElse(Orientation.PORTRAIT).toCssString());
        } else {
            return "";
        }
    }

    public String buildBaseUrlHeader() {
        String baseUrl = getBaseUrl();
        return baseUrl != null ? String.format("<base href='%s' />", baseUrl) : "";
    }

    private String getBaseUrl() {
        String polarionBaseUrl = System.getProperty(PolarionProperties.BASE_URL, LOCALHOST);
        if (!polarionBaseUrl.contains(LOCALHOST)) {
            return enrichByProtocolPrefix(polarionBaseUrl);
        }
        String hostname = Configuration.getInstance().cluster().nodeHostname();
        return enrichByProtocolPrefix(hostname);
    }

    private static String enrichByProtocolPrefix(String hostname) {
        if (StringUtils.isEmpty(hostname) || hostname.startsWith(HTTP_PROTOCOL_PREFIX) || hostname.startsWith(HTTPS_PROTOCOL_PREFIX)) {
            return hostname;
        } else {
            return HTTP_PROTOCOL_PREFIX + hostname;
        }
    }
}
