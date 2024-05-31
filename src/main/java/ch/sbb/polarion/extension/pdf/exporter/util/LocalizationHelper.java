package ch.sbb.polarion.extension.pdf.exporter.util;

import ch.sbb.polarion.extension.pdf.exporter.model.TranslationEntry;
import net.sf.okapi.lib.xliff2.core.Segment;
import net.sf.okapi.lib.xliff2.core.StartFileData;
import net.sf.okapi.lib.xliff2.core.Unit;
import net.sf.okapi.lib.xliff2.reader.Event;
import net.sf.okapi.lib.xliff2.reader.EventType;
import net.sf.okapi.lib.xliff2.reader.XLIFFReader;
import net.sf.okapi.lib.xliff2.writer.XLIFFWriter;

import java.io.StringWriter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class LocalizationHelper {

    private LocalizationHelper() {
    }

    public static Map<String, String> getTranslationsMapForLanguage(String source) {
        if (source == null) {
            return new TreeMap<>();
        }
        try (XLIFFReader reader = new XLIFFReader(XLIFFReader.VALIDATION_INCLUDE_SCHEMAS)) {
            Map<String, String> translations = new TreeMap<>();
            reader.open(source);
            while (reader.hasNext()) {
                Event nextEvent = reader.next();
                if (nextEvent.getType() == EventType.TEXT_UNIT) {
                    final Segment segment = nextEvent.getUnit().getSegment(0);
                    String sourceText = segment.getSource().getPlainText();
                    String targetText = segment.getTarget().getPlainText();
                    translations.put(sourceText, targetText);
                }
            }
            return translations;
        }
    }

    public static String xmlFromMap(String targetLanguage, Map<String, String> translations) {
        StringWriter stringWriter = new StringWriter();
        try (XLIFFWriter writer = createXliffWriter(stringWriter, targetLanguage)) {
            AtomicInteger id = new AtomicInteger(0);
            translations.forEach((key, value) -> {
                Unit unit = createUnit(id.getAndIncrement(), key, value);
                writer.writeUnit(unit);
            });
        }
        return stringWriter.toString();
    }

    public static String xmlForLanguage(String targetLanguage, Map<String, List<TranslationEntry>> translations) {
        StringWriter stringWriter = new StringWriter();

        try (XLIFFWriter writer = createXliffWriter(stringWriter, targetLanguage)) {
            AtomicInteger id = new AtomicInteger(0);
            translations.forEach((key, value) -> {
                TranslationEntry translationEntry = value.stream()
                        .filter(entry -> entry.getLanguage().equals(targetLanguage))
                        .findFirst()
                        .get();
                Unit unit = createUnit(id.getAndIncrement(), key, translationEntry.getValue());
                writer.writeUnit(unit);
            });
        }

        return stringWriter.toString();
    }

    private static XLIFFWriter createXliffWriter(StringWriter stringWriter, String targetLanguage) {
        XLIFFWriter writer = new XLIFFWriter();
        writer.setUseIndentation(true);
        writer.create(stringWriter,
                Locale.US.getLanguage(),
                Locale.forLanguageTag(targetLanguage).getLanguage());

        StartFileData fileElementAttribute = new StartFileData(null);
        String originalFile = targetLanguage + ".xlf";
        fileElementAttribute.setId("1");
        fileElementAttribute.setOriginal(originalFile);
        writer.writeStartFile(fileElementAttribute);
        return writer;
    }

    private static Unit createUnit(int id, String source, String target) {
        String strId = String.valueOf(id);
        Unit unit = new Unit(strId);
        Segment segment = unit.appendSegment();
        segment.setSource(source);
        segment.setTarget(target);
        return unit;
    }

}
