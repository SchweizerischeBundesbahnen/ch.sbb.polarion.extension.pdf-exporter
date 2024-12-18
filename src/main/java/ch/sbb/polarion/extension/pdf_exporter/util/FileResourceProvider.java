package ch.sbb.polarion.extension.pdf_exporter.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface FileResourceProvider {

    @Nullable
    String getResourceAsBase64String(@NotNull String resource, List<String> unavailableWorkItemAttachments);

    byte[] getResourceAsBytes(@NotNull String resource, List<String> unavailableWorkItemAttachments);

}
