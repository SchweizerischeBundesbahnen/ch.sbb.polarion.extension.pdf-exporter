package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.util.BundleJarsPrioritizingRunnable;
import com.polarion.core.util.logging.Logger;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypes;

import java.util.Map;
import java.util.Optional;

/**
 * Used to run fresh Apache Tika library bundled into extension jar isolated from Polarion's own (older) Tika version.
 */
public class TikaMimeTypeResolver implements BundleJarsPrioritizingRunnable {

    public static final String PARAM_VALUE = "value";
    public static final String PARAM_RESULT = "result";

    private static final Logger logger = Logger.getLogger(TikaMimeTypeResolver.class);

    private final Tika tika = new Tika();

    @Override
    public Map<String, Object> run(Map<String, Object> map) {
        // Always return a map containing PARAM_RESULT. If detection fails we log the cause and return an empty Optional,
        // so callers never have to guard against a missing key (which previously caused NPE downstream).
        Optional<String> detectedMimeType = Optional.empty();
        try {
            Object value = map.get(PARAM_VALUE);
            String detected = value instanceof String stringValue ? tika.detect(stringValue) : tika.detect((byte[]) value);
            // Ignore 'application/octet-stream' fallback
            if (!MimeTypes.OCTET_STREAM.equals(detected)) {
                detectedMimeType = Optional.ofNullable(detected);
            }
        } catch (Exception e) {
            logger.warn("Tika failed to detect mime type", e);
        }
        return Map.of(PARAM_RESULT, detectedMimeType);
    }

}
