package ch.sbb.polarion.extension.pdf.exporter.weasyprint.cli;

import ch.sbb.polarion.extension.pdf.exporter.weasyprint.WeasyPrintOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.module.ModuleDescriptor;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class WeasyPrintExecutorTest {

    private final WeasyPrintExecutor weasyPrintExecutor = new WeasyPrintExecutor();

    @ParameterizedTest
    @MethodSource("paramsForGetCommand")
    void shouldGetCommand(String[] executable, WeasyPrintOptions weasyPrintOptions, String[] expectedCommand) {
        // Act
        String[] command = weasyPrintExecutor.getCommand(executable, weasyPrintOptions);

        // Assert
        assertThat(command[0]).contains(expectedCommand[0]);
        IntStream.range(1, command.length)
                .forEach(i -> assertThat(command[i]).isEqualTo(expectedCommand[i]));
    }

    private static Stream<Arguments> paramsForGetCommand() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("windows")) {
            return Stream.of(
                    Arguments.of(
                            new String[] {"powershell.exe", "echo", "'version 54.3'"},
                            new WeasyPrintOptions(true),
                            new String[] {"powershell", "echo", "'version 54.3'", "-p", "-e", "utf8", "-", "-"}
                    ),
                    Arguments.of(
                            new String[] {"powershell.exe", "echo", "'version 52.1'"},
                            new WeasyPrintOptions(false),
                            new String[] {"powershell", "echo", "'version 52.1'", "-e", "utf8", "-f", "pdf", "-", "-"}
                    ),
                    Arguments.of(
                            new String[] {"powershell.exe", "echo", "XXX"},
                            new WeasyPrintOptions(true),
                            new String[] {"powershell", "echo", "XXX", "-p", "-e", "utf8", "-f", "pdf", "-", "-"}
                    )
            );
        } else {
            return Stream.of(
                    Arguments.of(
                            new String[] {"echo", "version 54.3"},
                            new WeasyPrintOptions(true),
                            new String[] {"echo", "version 54.3", "-p", "-e", "utf8", "-", "-"}
                    ),
                    Arguments.of(
                            new String[] {"echo", "version 52.1"},
                            new WeasyPrintOptions(false),
                            new String[]{"echo", "version 52.1", "-e", "utf8", "-f", "pdf", "-", "-"}
                    ),
                    Arguments.of(
                            new String[] {"echo", "XXX"},
                            new WeasyPrintOptions(true),
                            new String[] {"echo", "XXX", "-p", "-e", "utf8", "-f", "pdf", "-", "-"}
                    )
            );
        }
    }

    @Test
    void shouldFindExecutable() {
        // Arrange
        String osName = System.getProperty("os.name").toLowerCase();
        String command = osName.contains("windows")? "powershell.exe" : "echo";

        // Act
        String executableFullPath = weasyPrintExecutor.findExecutable(command);

        // Assert
        assertThat(executableFullPath).contains(command);
        assertThat(executableFullPath.length()).isGreaterThan(command.length());
    }

    @Test
    void shouldGetFirstSuitableExecutablePathFromString() {
        String text1 = "C:\\Program Files\\Docker\\Docker\\bin\\docker" + System.lineSeparator() + "C:\\Program Files\\Docker\\Docker\\bin\\docker.exe";
        String path1 = weasyPrintExecutor.getFirstSuitableExecutablePathFromString("docker", true, text1);
        assertThat(path1).isEqualTo("C:\\Program Files\\Docker\\Docker\\bin\\docker.exe");

        String text2 = "/usr/local/bin/docker"  + System.lineSeparator() + "/usr/local/bin/docker-alternative";
        String path2 = weasyPrintExecutor.getFirstSuitableExecutablePathFromString("docker", false, text2);
        assertThat(path2).isEqualTo("/usr/local/bin/docker");

        String text3 = "";
        Assertions.assertThrows(IllegalStateException.class, () -> weasyPrintExecutor.getFirstSuitableExecutablePathFromString("docker", false, text3));
    }

    @Test
    void shouldExtractVersionFromExecutable() {
        // Arrange
        String osName = System.getProperty("os.name").toLowerCase();
        String[] executable = osName.contains("windows")? new String[]{"powershell.exe", "echo", "'version 1.2.3'"} : new String[]{"echo", "version 1.2.3"};

        // Act
        ModuleDescriptor.Version version = weasyPrintExecutor.getExecutableVersion(executable);

        // Assert
        assertThat(version.toString()).startsWith("1.2.3");
    }

    @ParameterizedTest
    @MethodSource("paramsForWeasyPrintVersion")
    void shouldParseWeasyPrintVersion(String input, ModuleDescriptor.Version expectedVersion) {
        // Act
        ModuleDescriptor.Version version = weasyPrintExecutor.parseVersion(input);

        // Assert
        assertThat(version).isEqualTo(expectedVersion);
    }

    private static Stream<Arguments> paramsForWeasyPrintVersion() {
        return Stream.of(
                Arguments.of("test version 2.3.4", ModuleDescriptor.Version.parse("2.3.4")),
                Arguments.of("test 2.3.4", null),
                Arguments.of("", null)
        );
    }
}