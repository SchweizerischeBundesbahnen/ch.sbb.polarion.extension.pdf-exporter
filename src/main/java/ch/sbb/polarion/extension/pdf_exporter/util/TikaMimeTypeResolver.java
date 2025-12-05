package ch.sbb.polarion.extension.pdf_exporter.util;

import ch.sbb.polarion.extension.generic.util.BundleJarsPrioritizingRunnable;
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

    private final Tika tika = new Tika();

    @Override
    public Map<String, Object> run(Map<String, Object> map) {
        Object value = map.get(PARAM_VALUE);

        // Currently we can handle String (file names) or byte[] (file content)
        String detected = value instanceof String stringValue ? tika.detect(stringValue) : tika.detect((byte[]) value);

        // Ignore 'application/octet-stream' fallback
        return Map.of(PARAM_RESULT, Optional.ofNullable(MimeTypes.OCTET_STREAM.equals(detected) ? null : detected));
    }

}
