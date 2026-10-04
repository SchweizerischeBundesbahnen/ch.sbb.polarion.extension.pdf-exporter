# Development Guide

This document provides comprehensive development guidelines for contributing to this Polarion extension project. It complements other documentation files and focuses specifically on technical setup and development workflows.

## Table of Contents

- [Development Environment Setup](#development-environment-setup)
- [Project Structure](#project-structure)
- [Building the Project](#building-the-project)
- [Testing](#testing)
- [Debugging](#debugging)
- [Development Workflow](#development-workflow)
- [Common Development Tasks](#common-development-tasks)

## Development Environment Setup

### Prerequisites

- Java JDK 21
- Maven 3.9
- IDE of your choice (IntelliJ IDEA, Eclipse, VS Code with Java extensions)
- Git
- Active Polarion license (as mentioned in [CONTRIBUTING.md](./CONTRIBUTING.md), all contributors must have an active Polarion license)

### Setting Up Your Environment

1. Clone the repository:

   ```bash
   git clone https://github.com/SchweizerischeBundesbahnen/<repository-name>.git
   cd <repository-name>
   ```

2. Set up Polarion dependencies:
   - Extract dependencies from your Polarion installer using [polarion-artifacts-deployer](https://github.com/SchweizerischeBundesbahnen/polarion-artifacts-deployer)
   - This step is required for building and testing the extension

3. Set environment variables:

   ```bash
   export POLARION_HOME=/path/to/your/polarion/installation
   ```

4. Import the project into your IDE:
   - For Eclipse: Import as "Existing Maven Project"
   - For IntelliJ IDEA: Import as Maven project
   - For VS Code: Open folder and ensure Java extensions are installed

## Project Structure

The project follows a standard Maven directory structure:

```
├── src/
│   ├── main/
│   │   ├── java/           # Java source files
│   │   │   └── ch/sbb/polarion/extension/
│   │   └── resources/      # Resources like configuration files
│   │       ├── META-INF/
│   │       └── webapp/     # Web resources
│   └── test/               # Test sources
├── docs/                   # Documentation
├── LICENSE                 # Apache License 2.0
├── NOTICE                  # Copyright attribution
├── DCO                     # Developer Certificate of Origin
├── pom.xml                 # Maven configuration
├── README.md               # Project overview
└── various .md files       # Additional documentation
```

## Building the Project

### Basic Build

```bash
mvn clean package
```

### Install to Local Polarion

```bash
mvn clean install -P local-install-into-polarion
```

Note: This requires `POLARION_HOME` environment variable to be set correctly.

### Build with Tests

```bash
mvn clean verify
```

## Testing

### Running Tests

The project uses JUnit for testing. Run tests with:

```bash
mvn test
```

### Test Coverage

Code coverage reports can be generated with:

```bash
mvn verify
```

The reports will be available in `target/site/jacoco`.

### UI Test Flags

The `ui/` Vitest suite runs in the `test` phase alongside the Java tests. By default it runs the full suite (behavior + visual regression) inside the pinned Playwright Docker image so the screenshots match the committed references. These `-D` flags adjust that:

| Flag | Effect |
| --- | --- |
| `-DskipJsTests` | Skip all UI tests (Java tests still run). Use on a host without Docker. |
| `-DjsTestsNoDocker` | Run the tests natively (`npm run test:coverage:full`) instead of the Docker image. Needs the Playwright browser on the host (see the install flags below). |
| `-DinstallPlaywright` | Install the Chromium browser plus its OS libraries (the with-deps variant) before the tests. Needs a Debian/Ubuntu host with `apt-get` and root/sudo. |
| `-DinstallPlaywrightNoDeps` | Install the Chromium browser binary only. For hosts without `apt-get`/root; the host must already provide Chromium's system libraries. |
| `-DskipVisualJsTests` | Exclude the pixel-based visual-regression tests (`*.visual.test.tsx`), which only reproduce inside the Docker image. Only effective together with `-DjsTestsNoDocker`. |

Docker-less run of the behavior tests:

```bash
mvn clean install -DjsTestsNoDocker -DinstallPlaywrightNoDeps -DskipVisualJsTests
```

### Performance Tests

The performance tests export documents of a known shape and fail when an export takes far longer than it does today.
They are tagged `performance` and run in a profile of their own, not in the regular build:

```bash
mvn verify -P performance-tests-with-weasyprint-docker
```

The profile builds the classes, the tests and the jar, and runs these tests alone. It takes every other step of the
build out: the documentation pages, the UI and its tests, the OpenAPI file, coverage, the source and javadoc jars and
the Polarion compatibility check. Pass `-Dweasyprint.service.url=http://localhost:9080` to use a running WeasyPrint
service instead of a container.

- `ExportPerformanceTest` exports one shape each: a small document, a large table, cells running across pages, many
  images, many work items, sections which page breaks turn landscape, tables whose words leave them no room, and
  hyphenated tables. The small document takes the exporter little but what every export costs, so a cost added to
  every export shows there as a multiple of its time.
- `FeatureDocumentTest` exports one document with every feature and every option of a style package, and compares its
  pages with reference images, so that one export shows whether any of it broke.

The exporter and WeasyPrint are timed apart, read from the generation log, so a failure names the slow side. Each
document is exported three times, and the time of each part is the average.

The reference time of each part is in `src/test/resources/performance/reference-times.properties`: its average over
runs of CI, together with the time CI takes for a fixed piece of JDK work, hashing, sorting and many small objects, which
runs no code of the exporter. A run expects each part to take its reference time scaled to the run:

- The small document is exported first. Every other export is scaled by how much longer or shorter than its reference
  the small document took in this run, the exporter and WeasyPrint apart. Whatever makes this machine or this moment
  faster or slower makes the small document so too, so a runner slower at WeasyPrint than at Java, or a busy neighbor,
  cancels out.
- A change which slows every export would slow the small document as well and cancel out that way. So the small document
  itself is scaled by the fixed work, timed before the first test: a slowdown of every export, as #1139, shows there.

The CSS of an export carries the fonts of the default CSS embedded, as in Polarion, so a cost which grows with the CSS
shows here too. Each part is judged against its expected time:

| Part | Warning above | Fails above |
|---|---|---|
| Exporter | 1.2 times | 1.5 times |
| WeasyPrint | 1.35 times | 2 times |

Across eleven runs of CI, no part came more than 16 % above its expected time. A part over its limit fails its test and
the build. A warning only marks the part in the report and writes a `::warning` line, which GitHub Actions shows as an
annotation of the run. On a machine unlike the CI runners, as an arm64 Mac, the parts come out up to a third below their
expected time, as WeasyPrint lays out large documents relatively faster there.

After the last test, the log shows the report: a table of the reference times, what each is scaled by and the time
expected here, and a table of the results, each part with its expected time, its time, how far it is from the expected
one, its warning level, its limit and its result. The report is also written to
`target/surefire-reports/performance-summary.md`, which CI adds to the summary of the run and uploads with the timing
report of each export.

Each run also writes its times to `target/surefire-reports/performance-reference-times.properties`, which CI uploads.
To take new reference times, average them over at least five runs of CI on `main`, and propose the result in a pull
request:

```bash
gh run list --workflow "Performance Tests" --branch main --status success --limit 10 --json databaseId -q '.[].databaseId' \
  | xargs -I{} gh run download {} -n performance-reports -D runs/{}
java src/test/java/ch/sbb/polarion/extension/pdf_exporter/weasyprint/performance/AverageReferenceTimes.java \
  runs/*/performance-reference-times.properties > src/test/resources/performance/reference-times.properties
```

A pull request, never the build itself, so that a regression merged into `main` cannot become its own reference.

## Debugging

### Remote Debugging

For debugging the extension in a running Polarion instance:

1. Add debug parameters to the `config.sh` file in your Polarion installation:

   ```bash
   # Add this line to config.sh
   JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
   ```

2. Start Polarion as a service:

   ```bash
   service polarion start
   ```

3. Connect your IDE to the remote JVM on port 5005

### Logging

- Use the Polarion logging system for extension logs
- Logs are available in `<polarion_home>/polarion/logs/main.log`

## Development Workflow

### Branching Strategy

- `main` branch is protected and represents the production-ready state
- Create feature branches from `main` for new work
- Follow the pattern: `feature/<feature-name>` or `fix/<bug-name>`

### Pull Request Process

1. Ensure your branch is up to date with `main`
2. Make sure all tests pass
3. Create a pull request targeting `main`
4. Follow the commit message guidelines in [CONTRIBUTING.md](./CONTRIBUTING.md)
5. Ensure the PR description clearly describes the changes
6. Wait for code review and approval

### Continuous Integration

This project uses GitHub Actions for CI/CD, and SonarCloud for code quality analysis.

## Common Development Tasks

### Creating a New Feature

1. Create a new branch from `main`
2. Implement your feature
3. Write tests
4. Update documentation
5. Submit a pull request

### Fixing a Bug

1. Create a new branch from `main`
2. Fix the bug
3. Add a test that verifies the fix
4. Submit a pull request

## Related Documentation

- [README.md](./README.md) - Project overview and installation instructions
- [CONTRIBUTING.md](./CONTRIBUTING.md) - Guidelines for contributing to the project
- [CODING_STANDARDS.md](./CODING_STANDARDS.md) - Detailed coding standards
- [RELEASE.md](./RELEASE.md) - Information about the release process
