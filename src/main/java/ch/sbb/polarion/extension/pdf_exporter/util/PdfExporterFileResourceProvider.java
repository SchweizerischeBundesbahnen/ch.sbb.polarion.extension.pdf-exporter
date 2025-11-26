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

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
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
    public String getResourceAsBase64String(@NotNull String resource) {
        if (MediaUtils.isDataUrl(resource)) { // do nothing if it's already has 'data' url
            return resource;
        }
        byte[] resourceBytes = getResourceAsBytes(resource);
        if (resourceBytes != null && resourceBytes.length != 0) { // Don't make any manipulations if resource wasn't resolved
            String mimeType = MediaUtils.guessMimeType(resource, resourceBytes);
            if (MIME_TYPE_SVG.equals(mimeType)) {
                // Additional check to verify that the content is indeed an SVG
                mimeType = MediaUtils.getMimeTypeUsingTikaByContent(resource, resourceBytes);
            }
            return String.format("data:%s;base64,%s", mimeType, Base64.getEncoder().encodeToString(resourceBytes));
        }
        return null;
    }

    public byte[] getResourceAsBytes(@NotNull String resource) {
        // Non-default icons are getting via project and thus requires open transaction
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> {
            try {
                return getResourceAsBytesImpl(resource);
            } catch (Exception e) {
                logger.error("Error loading resource '" + resource + "' for PDF export.", e);
            } finally {
                ExecutionThreadMonitor.checkForInterruption();
            }
            return new byte[0];
        });
    }

    @VisibleForTesting
    byte[] getResourceAsBytesImpl(String resource) throws IOException {
        for (IUrlResolver resolver : resolvers) {
            if (resolver.canResolve(resource)) {
                InputStream stream = resolver.resolve(resource);
                if (stream != null) {
                    byte[] result = StreamUtils.suckStreamThenClose(stream);
                    if (result.length > 0 && WorkItemAttachmentUrlResolver.isWorkItemAttachmentUrl(resource) &&
                            (!WorkItemAttachmentUrlResolver.isSvg(resource) && isUnexpectedlyResolvedAsHtml(resource, result))) {
                        ExportContext.addWorkItemIDsWithMissingAttachment(getWorkItemIdsWithUnavailableAttachments(resource));
                        return getDefaultContent(resource);
                    }

                    if (result.length > 0) {
                        return result;
                    }
                }
            }
        }
        return new byte[0];
    }

    @VisibleForTesting
    boolean isUnexpectedlyResolvedAsHtml(String resource, byte[] content) {
        String detectedMimeType = MediaUtils.getMimeTypeUsingTikaByContent(resource, content);
        String expectedMimeType = MediaUtils.getMimeTypeUsingTikaByResourceName(resource, null);

        if (detectedMimeType == null || expectedMimeType == null) {
            return false;
        }

        // if application/xhtml+xml response received, we should check whether this is what we expected
        // because it can be a redirect to Login Page in case of missing resource!
        if (detectedMimeType.equals(MediaType.APPLICATION_XHTML_XML)) {
            return !expectedMimeType.equals(detectedMimeType);
        }

        // in all other cases we just make the decision that everything is ok
        return false;
    }

    @VisibleForTesting
    @SuppressWarnings("java:S1075")
    byte[] getDefaultContent(String resource) throws IOException {
        String pathToDefaultImage;
        if (WorkItemAttachmentUrlResolver.isSvg(resource)) {
            pathToDefaultImage = "/webapp/ria/images/image_not_accessible_svg.png";
        } else if (!StringUtils.isEmpty(MediaUtils.getImageFormat(resource))) {
            pathToDefaultImage = "/webapp/ria/images/image_not_accessible.png";
        } else {
            return new byte[0];
        }
        File defaultImage = new File(BundleHelper.getPath("com.polarion.alm.ui", pathToDefaultImage));
        return StreamUtils.suckStreamThenClose(new FileInputStream(defaultImage));
    }

    @VisibleForTesting
    String getWorkItemIdsWithUnavailableAttachments(@NotNull String url) {
        String prefix = "/polarion/wi-attachment/";
        try {
            if (!url.startsWith(prefix)) {
                URI uri = new URI(url);
                url = StringUtils.stripDuplicateLeadingSlashes(uri.getPath());
                String query = uri.getQuery();
                if (query != null) {
                    url = url + "?" + query;
                }
            }

            String suffix = url.substring(prefix.length());
            Pattern pattern = Pattern.compile("([^/]*)/([^/]*)/([^/?]*)(?:\\?(.*))?");
            Matcher matcher = pattern.matcher(suffix);
            if (matcher.matches()) {
                return PolarionUrlResolver.decode(matcher.group(2));
            }
        } catch (URISyntaxException e) {
            return "";
        }
        return null;
    }

    /**
     * Remove GenericUrlResolver because it has no explicit timeouts declared
     */
    @VisibleForTesting
    @SneakyThrows
    IAttachmentUrlResolver getPolarionUrlResolverWithoutGenericUrlChildResolver() {
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
