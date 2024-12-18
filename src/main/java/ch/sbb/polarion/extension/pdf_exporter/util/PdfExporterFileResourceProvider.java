package ch.sbb.polarion.extension.pdf_exporter.util;

import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.tracker.internal.url.GenericUrlResolver;
import com.polarion.alm.tracker.internal.url.IAttachmentUrlResolver;
import com.polarion.alm.tracker.internal.url.IUrlResolver;
import com.polarion.alm.tracker.internal.url.ParentUrlResolver;
import com.polarion.alm.tracker.internal.url.PolarionUrlResolver;
import com.polarion.alm.tracker.internal.url.WorkItemAttachmentUrlResolver;
import com.polarion.core.util.StreamUtils;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.eclipse.BundleHelper;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.internal.ExecutionThreadMonitor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ch.sbb.polarion.extension.pdf_exporter.util.exporter.Constants.MIME_TYPE_SVG;

/**
 * Initial code taken from {@link com.polarion.alm.tracker.web.internal.server.CustomFileResourceProvider}
 */
public class PdfExporterFileResourceProvider implements FileResourceProvider {

    private static final Logger logger = Logger.getLogger(PdfExporterFileResourceProvider.class);

    private final List<IUrlResolver> resolvers;

    public PdfExporterFileResourceProvider() {
        this.resolvers = Arrays.asList(getPolarionUrlResolverWithoutGenericUrlChildResolver(), new CustomResourceUrlResolver());
    }

    @VisibleForTesting
    public PdfExporterFileResourceProvider(List<IUrlResolver> resolvers) {
        this.resolvers = resolvers;
    }

    @SneakyThrows
    @Override
    @Nullable
    public String getResourceAsBase64String(@NotNull String resource, List<String> unavailableWorkItemAttachments) {
        if (MediaUtils.isDataUrl(resource)) { // do nothing if it's already has 'data' url
            return resource;
        }
        byte[] resourceBytes = getResourceAsBytes(resource, unavailableWorkItemAttachments);
        if (resourceBytes != null && resourceBytes.length != 0) { // Don't make any manipulations if resource wasn't resolved
            String mimeType = MediaUtils.guessMimeType(resource, resourceBytes);
            if (MIME_TYPE_SVG.equals(mimeType)) {
                resourceBytes = processPossibleSvgImage(resourceBytes);
            }
            return String.format("data:%s;base64,%s", mimeType, Base64.getEncoder().encodeToString(resourceBytes));
        }
        return null;
    }

    public byte[] getResourceAsBytes(@NotNull String resource, List<String> unavailableWorkItemAttachments) {
        // Non-default icons are getting via project and thus requires open transaction
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> {
            try {
                return getResourceAsBytesImpl(resource, unavailableWorkItemAttachments);
            } catch (Exception e) {
                logger.error("Error loading resource '" + resource + "' for PDF export.", e);
            } finally {
                ExecutionThreadMonitor.checkForInterruption();
            }
            return new byte[0];
        });
    }

    private byte[] getResourceAsBytesImpl(String resource, List<String> unavailableWorkItemAttachments) throws IOException {
        for (IUrlResolver resolver : resolvers) {
            if (resolver.canResolve(resource)) {
                InputStream stream = resolver.resolve(resource);
                if (stream != null) {
                    byte[] result = StreamUtils.suckStreamThenClose(stream);
                    if (result.length > 0) {
                        if (WorkItemAttachmentUrlResolver.isWorkItemAttachmentUrl(resource) && isMediaTypeMismatch(resource, result)) {
                            unavailableWorkItemAttachments.add(readAttachmentInfo(resource));
                            return getDefaultContent(resource);
                        }
                        return result;
                    }
                }
            }
        }
        return new byte[0];
    }

    private boolean isMediaTypeMismatch(String resource, byte[] content) {
        String detectedMimeType = MediaUtils.getMimeTypeUsingTikaByContent(resource, content);
        String expectedMimeType = MediaUtils.getMimeTypeUsingTikaByResourceName(resource, null);
        return expectedMimeType != null && !expectedMimeType.equals(detectedMimeType);
    }

    private byte[] getDefaultContent(String resource) throws IOException {
        String sadBearPath;
        if (WorkItemAttachmentUrlResolver.isSvg(resource)) {
            sadBearPath = "/webapp/ria/images/image_not_accessible_svg.png";
        } else if (!StringUtils.isEmpty(MediaUtils.getImageFormat(resource))) {
            sadBearPath = "/webapp/ria/images/image_not_accessible.png";
        } else {
            return new byte[0];
        }
        File sorryBear = new File(BundleHelper.getPath("com.polarion.alm.ui", sadBearPath));
        return StreamUtils.suckStreamThenClose(new FileInputStream(sorryBear));
    }

    private String readAttachmentInfo(@NotNull String url) {
        String suffix = getSuffix(url, "/polarion/wi-attachment/");
        Pattern pattern = Pattern.compile("([^/]*)/([^/]*)/([^/\\?]*)(?:\\?(.*))?");
        Matcher matcher = pattern.matcher(suffix);
        if (matcher.matches()) {
            return PolarionUrlResolver.decode(matcher.group(2));
        }
        return null;
    }

    private String getSuffix(@NotNull String url, @NotNull String prefix) {
        if (!url.startsWith(prefix)) {
            try {
                URI uri = new URI(url);
                url = StringUtils.stripDuplicateLeadingSlashes(uri.getPath());
                String query = uri.getQuery();
                if (query != null) {
                    url = url + "?" + query;
                }
            } catch (URISyntaxException var4) {
                return "";
            }
        }

        return url.substring(prefix.length());
    }

    @VisibleForTesting
    @SuppressWarnings("squid:S1166") // no need to log or rethrow exception by design
    public byte[] processPossibleSvgImage(byte[] possibleSvgImageBytes) {
        try {
            String svgContent = new String(possibleSvgImageBytes, StandardCharsets.UTF_8);
            return MediaUtils.removeSvgUnsupportedFeatureHint(svgContent).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            // not a valid string, just nvm
        }
        return possibleSvgImageBytes;
    }

    /**
     * Remove GenericUrlResolver because it has no explicit timeouts declared
     */
    @SneakyThrows
    private IAttachmentUrlResolver getPolarionUrlResolverWithoutGenericUrlChildResolver() {
        IAttachmentUrlResolver attachmentUrlResolver = PolarionUrlResolver.getInstance();

        if (attachmentUrlResolver instanceof ParentUrlResolver parentUrlResolver) {
            Field childResolversField = ParentUrlResolver.class.getDeclaredField("childResolvers");
            childResolversField.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<IUrlResolver> childResolvers = ((List<IUrlResolver>) childResolversField.get(parentUrlResolver)).stream()
                    .filter(resolver -> !(resolver instanceof GenericUrlResolver))
                    .toList();

            return new ParentUrlResolver(childResolvers);
        }

        return attachmentUrlResolver;
    }
}
