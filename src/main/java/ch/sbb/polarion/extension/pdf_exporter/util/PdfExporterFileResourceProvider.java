package ch.sbb.polarion.extension.pdf_exporter.util;

import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.tracker.internal.url.IUrlResolver;
import com.polarion.alm.tracker.internal.url.PolarionUrlResolver;
import com.polarion.core.util.StreamUtils;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.internal.ExecutionThreadMonitor;

import java.io.IOException;
import java.io.InputStream;

/**
 * Initial code taken from {@link com.polarion.alm.tracker.web.internal.server.CustomFileResourceProvider}
 */
public class PdfExporterFileResourceProvider implements FileResourceProvider {

    private static final Logger logger = Logger.getLogger(PdfExporterFileResourceProvider.class);
    private static final IUrlResolver[] resolvers = {PolarionUrlResolver.getInstance(), new CustomImageUrlResolver()};

    public byte[] getResourceAsBytes(String resource) {
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

    private byte[] getResourceAsBytesImpl(String resource) throws IOException {
        for (IUrlResolver resolver : resolvers) {
            if (resolver.canResolve(resource)) {
                InputStream stream = resolver.resolve(resource);
                if (stream != null) {
                    byte[] result = StreamUtils.suckStreamThenClose(stream);
                    if (result.length > 0) {
                        return result;
                    }
                }
            }
        }
        return new byte[0];
    }
}

