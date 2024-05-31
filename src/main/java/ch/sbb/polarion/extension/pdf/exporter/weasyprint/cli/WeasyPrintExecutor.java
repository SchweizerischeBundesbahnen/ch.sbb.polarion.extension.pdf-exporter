package ch.sbb.polarion.extension.pdf.exporter.weasyprint.cli;

import ch.sbb.polarion.extension.pdf.exporter.PdfExportException;
import ch.sbb.polarion.extension.pdf.exporter.properties.PdfExporterExtensionConfiguration;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintConverter;
import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintOptions;
import com.polarion.core.util.logging.Logger;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.module.ModuleDescriptor;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WeasyPrintExecutor implements WeasyPrintConverter {
    private static final String STDINOUT = "-";
    private static final int TIMEOUT = 10;
    private static final ModuleDescriptor.Version FORMAT_PARAM_REMOVED_VERSION = ModuleDescriptor.Version.parse("53.0");
    private static final Pattern WEASYPRINT_VERSION_PATTERN = Pattern.compile("version (?<version>.+)", Pattern.CASE_INSENSITIVE);

    private final List<Integer> successValues = new ArrayList<>(Collections.singletonList(0));
    private final Logger logger = Logger.getLogger(WeasyPrintExecutor.class);

    @Override
    @SneakyThrows
    public byte[] convertToPdf(String htmlPage, WeasyPrintOptions weasyPrintOptions) {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            String weasyPrintExecutable = PdfExporterExtensionConfiguration.getInstance().getWeasyprintExecutable();

            String[] executable = weasyPrintExecutable.split(" ");
            String[] command = getCommand(executable, weasyPrintOptions);

            Process process = Runtime.getRuntime().exec(command);

            Future<byte[]> inputStreamToByteArray = executor.submit(streamToByteArrayTask(process.getInputStream()));
            Future<byte[]> errorStreamToByteArray = executor.submit(streamToByteArrayTask(process.getErrorStream()));

            try (
                    OutputStream stdin = process.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin))
            ) {
                writer.write(htmlPage);
                writer.flush();
            }

            process.waitFor();

            if (!successValues.contains(process.exitValue())) {
                byte[] errorStream = getFuture(errorStreamToByteArray);
                String error = new String(errorStream);
                logger.error(String.format("Error while generating pdf: %s", error));
                throw new PdfExportException(String.join(" ", command), process.exitValue(), errorStream, getFuture(inputStreamToByteArray));
            } else {
                String error = new String(getFuture(errorStreamToByteArray));
                logger.debug(String.format("WeasyPrint output:%s%s", System.lineSeparator(), error));
            }

            logger.info("PDF successfully generated with");
            return getFuture(inputStreamToByteArray);
        } finally {
            logger.debug("Shutting down executor for WeasyPrint.");
            executor.shutdownNow();
        }
    }

    @Override
    public ModuleDescriptor.Version getWeasyPrintVersion() {
        String weasyPrintExecutable = PdfExporterExtensionConfiguration.getInstance().getWeasyprintExecutable();
        String[] executable = weasyPrintExecutable.split(" ");
        return getExecutableVersion(executable);
    }

    @VisibleForTesting
    String[] getCommand(String[] weasyPrintExecutable, WeasyPrintOptions weasyPrintOptions) {
        String fullPathExecutable = findExecutable(weasyPrintExecutable[0]);

        List<String> commandLine = new ArrayList<>(Arrays.asList(weasyPrintExecutable));
        commandLine.set(0, fullPathExecutable);

        String weasyprintPdfVariant = PdfExporterExtensionConfiguration.getInstance().getWeasyprintPdfVariant();
        if (weasyprintPdfVariant != null) {
            commandLine.add("--pdf-variant");
            commandLine.add(weasyprintPdfVariant);
        }

        if (weasyPrintOptions.followHTMLPresentationalHints()) {
            commandLine.add("-p"); // Follow HTML presentational hints (eg. honor direct width/height attributes of HTML elements, if appropriate CSS in style attribute is missing)
        }
        commandLine.add("-e");
        commandLine.add("utf8");

        //WeasyPrint lower than v.53 requires explicit format param, but newer versions fail when they meet it
        ModuleDescriptor.Version executableVersion = getExecutableVersion(weasyPrintExecutable);
        if (executableVersion == null || executableVersion.compareTo(FORMAT_PARAM_REMOVED_VERSION) < 0) {
            commandLine.add("-f");
            commandLine.add("pdf");
        }

        commandLine.add(STDINOUT);
        commandLine.add(STDINOUT);
        logger.debug(String.format("Command generated: %s", commandLine));
        return commandLine.toArray(new String[]{});
    }

    @VisibleForTesting
    String findExecutable(String executable) {
        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("windows");
            String command = isWindows ? "where.exe " + executable : "which " + executable;
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

            String text = IOUtils.toString(process.getInputStream(), Charset.defaultCharset()).trim();
            String path = getFirstSuitableExecutablePathFromString(executable, isWindows, text);
            logger.debug(String.format("'%s' command found: %s", executable, path));
            return path;
        } catch (InterruptedException | IOException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed while getting '" + executable + "' executable.", e);
        }
    }

    @VisibleForTesting
    ModuleDescriptor.Version getExecutableVersion(String[] weasyPrintExecutable) {
        List<String> command = new ArrayList<>(Arrays.asList(weasyPrintExecutable));
        command.add("--version");

        try {
            Process process = Runtime.getRuntime().exec(command.toArray(String[]::new));

            try (BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = stdInput.readLine()) != null) {
                    return parseVersion(line);
                }
            }
        } catch (Exception e) {
            logger.warn("Cannot get weasyprint version", e);
        }
        return null;
    }

    @Nullable
    @VisibleForTesting
    ModuleDescriptor.Version parseVersion(String line) {
        Matcher matcher = WEASYPRINT_VERSION_PATTERN.matcher(line);
        if (matcher.find()) {
            String version = matcher.group("version");
            logger.debug("weasyprint executable version: " + version);
            return ModuleDescriptor.Version.parse(version);
        }
        return null;
    }

    @NotNull
    @VisibleForTesting
    String getFirstSuitableExecutablePathFromString(@NotNull String executable, boolean isWindows, @NotNull String text) {
        String[] lines = text.split(System.lineSeparator());

        String result = lines[0].trim();

        if (isWindows && lines.length > 1) {
            result = Arrays.stream(lines)
                    .map(String::trim)
                    .filter(line -> line.endsWith(".exe"))
                    .findFirst()
                    .orElse("");
        }

        if (result.isBlank()) {
            throw new IllegalStateException("'" + executable + "' command was not found.");
        }
        return result;
    }

    private Callable<byte[]> streamToByteArrayTask(final InputStream input) {
        return () -> IOUtils.toByteArray(input);
    }

    private byte[] getFuture(Future<byte[]> future) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return future.get(TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Thread.currentThread().interrupt();
            throw e;
        }
    }
}
