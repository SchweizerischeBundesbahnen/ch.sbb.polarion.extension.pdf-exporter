package ch.sbb.polarion.extension.pdf_exporter.util;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ExportContext {
    private static final ThreadLocal<List<String>> workItemIDsWithMissingAttachment = ThreadLocal.withInitial(ArrayList::new);

    public static void addWorkItemIDsWithMissingAttachment(String entry) {
        workItemIDsWithMissingAttachment.get().add(entry);
    }

    public static List<String> getWorkItemIDsWithMissingAttachment() {
        return new ArrayList<>(workItemIDsWithMissingAttachment.get());
    }

    public static void clear() {
        workItemIDsWithMissingAttachment.remove();
    }
}
