package ch.sbb.polarion.extension.pdf_exporter.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class ExportContext {
    private static final ThreadLocal<List<String>> workItemIDsWithMissingAttachment = ThreadLocal.withInitial(ArrayList::new);
    /**
     * Keyed by the url, so a resource named by several rules is reported once. The first reason wins: it is
     * the one the step which refused the resource gave, while the steps above it only see that nothing came back.
     */
    private static final ThreadLocal<Map<String, String>> blockedResources = ThreadLocal.withInitial(LinkedHashMap::new);

    public static void addWorkItemIDsWithMissingAttachment(String entry) {
        workItemIDsWithMissingAttachment.get().add(entry);
    }

    public static List<String> getWorkItemIDsWithMissingAttachment() {
        return new ArrayList<>(workItemIDsWithMissingAttachment.get());
    }

    /**
     * Records a resource the export did not embed, so that the result of the conversion can name it. The
     * exported document carries a placeholder or nothing at all where the resource was named.
     *
     * @param url    the address as the document names it
     * @param reason why it was not embedded, for the log and for the generation report
     */
    public static void addBlockedResource(@NotNull String url, @NotNull String reason) {
        blockedResources.get().putIfAbsent(url, reason);
    }

    /**
     * Takes back what an attempt to read a resource recorded, for a resource which was read after all. A
     * reference without a scheme is tried under both, and the first attempt may be refused while the second
     * one reads it: the document gets the resource then, and the result of the export may not say otherwise.
     */
    public static void unblockResource(@NotNull String url) {
        blockedResources.get().remove(url);
    }

    public static List<BlockedResource> getBlockedResources() {
        return blockedResources.get().entrySet().stream()
                .map(entry -> new BlockedResource(entry.getKey(), entry.getValue()))
                .toList();
    }

    public static void clear() {
        workItemIDsWithMissingAttachment.remove();
        blockedResources.remove();
    }

    /**
     * A resource the export did not embed, and why.
     */
    public record BlockedResource(@NotNull String url, @NotNull String reason) {
    }
}
