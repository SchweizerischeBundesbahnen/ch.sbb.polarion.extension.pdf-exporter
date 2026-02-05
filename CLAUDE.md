# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Polarion ALM extension that converts Polarion Documents to PDF files using WeasyPrint as a PDF engine. The project is a Maven-based Java application with JavaScript components, targeting Java 21 and supporting only the latest version of Polarion (currently 2512).

## Build Commands

### Basic Build
```bash
mvn clean package
```

### Install to Local Polarion
```bash
mvn clean install -P install-to-local-polarion
```
Requires `POLARION_HOME` environment variable set to your Polarion installation directory.

### Run Tests
```bash
# Java tests only
mvn test

# All tests including JavaScript
mvn verify

# Java tests with WeasyPrint Docker integration
mvn clean test -P tests-with-weasyprint-docker
```

### Run JavaScript Tests
```bash
npm test
```

### Generate Test Coverage
```bash
mvn verify
# Reports available in target/site/jacoco
```

### Pre-commit Hooks
```bash
pre-commit run -a
```
Always run this after implementation. The project uses extensive pre-commit hooks including yamlfix, gitleaks, commitizen, zizmor, and actionlint.

## Architecture

### Main Components

**Extension Entry Point:**
- `ExtensionBundleActivator.java` - OSGi bundle activator
- `PdfExporterFormExtension.java` - Main extension class integrating with Polarion's document UI

**Core Export Logic:**
- `converter/` - PDF conversion job management and execution
- `service/` - Business logic services
- `weasyprint/` - Integration with WeasyPrint REST service

**REST API:**
- `rest/controller/` - JAX-RS REST endpoints (API and Internal controllers)
- `rest/model/` - REST API models
- `PdfExporterRestApplication.java` - REST application configuration
- OpenAPI specification available at `docs/openapi.json`

**Settings Management:**
- `settings/` - Configuration handling for CSS, cover pages, headers/footers, localization, style packages, and filenames
- Settings are stored per-project in Polarion and accessed via REST API

**HTML Processing:**
- `util/html/` - HTML manipulation (CSS internalizer, link handling)
- `util/adjuster/` - Content adjusters (image sizing, table sizing, page width)
- `util/exporter/` - Document rendering customization (bypasses Polarion's native rendering)

**UI Integration:**
- `widgets/` - Polarion widget integration
- `src/main/resources/webapp/pdf-exporter/` - JavaScript UI components
- `src/main/resources/webapp/pdf-exporter-admin/` - Admin UI components

### Key Architectural Patterns

1. **Settings Inheritance**: Extension uses generic settings framework from parent project `ch.sbb.polarion.extension.generic`

2. **Document Type Support**: Handles multiple document types through `DocumentType` enum:
   - Baseline Collections
   - Live Documents
   - Live Reports
   - Test Runs
   - Wiki Pages

3. **Async Job Processing**: Export jobs can run asynchronously with configurable timeouts (managed via `converter/PdfConverterJobsCleaner.java`)

4. **Workflow Integration**: Supports Polarion workflow functions via `PdfExportFunction.java` to automatically generate and attach PDFs to work items

5. **HTML Transformation Pipeline**:
   - Document content → Polarion rendering → HTML processing (link internalization, CSS handling, size adjustments) → WeasyPrint → PDF

## Dependencies

### Java Dependencies
- **Polarion Version**: 2512 (configured via profile in pom.xml)
- **Parent POM**: `ch.sbb.polarion.extension.generic` (see pom.xml for current version)
- **Key Libraries** (see pom.xml for current versions):
  - Apache PDFBox (PDF manipulation)
  - Apache Velocity (templating)
  - JSoup (HTML parsing, provided by Polarion)
  - Okapi XLIFF (localization)
  - Byte Buddy (bytecode manipulation)
  - Testcontainers (testing)

### JavaScript Dependencies
- Mocha (testing framework)
- Chai (assertions)
- JSDOM (DOM simulation)

### External Services
- **WeasyPrint Service**: Required Docker service for PDF generation (default: http://localhost:9080)
  - Configured via property: `ch.sbb.polarion.extension.pdf-exporter.weasyprint.service`

## Configuration Properties

Key properties in `polarion.properties`:
- `ch.sbb.polarion.extension.pdf-exporter.weasyprint.service` - WeasyPrint service URL
- `ch.sbb.polarion.extension.pdf-exporter.webhooks.enabled` - Enable webhook functionality
- `ch.sbb.polarion.extension.pdf-exporter.debug` - Enable HTML logging for debugging

## Important Development Notes

1. **Package Naming**: Main package changed from `ch.sbb.polarion.extension.pdf.exporter` to `ch.sbb.polarion.extension.pdf_exporter` in version 7.0.0

2. **Context in pom.xml**:
   - `maven-jar-plugin.Extension-Context: pdf-exporter`
   - This defines the webapp context path

3. **Polarion Artifacts**: Requires Polarion dependencies extracted using [polarion-artifacts-deployer](https://github.com/SchweizerischeBundesbahnen/polarion-artifacts-deployer)

4. **SonarCloud Integration**: Project uses SonarCloud for code quality analysis

5. **Pre-commit Hooks**: Includes sensitive data leak detection (URLs, UE numbers, DEV ticket numbers) specific to SBB

6. **License**: SBB License v1.0 (see LICENSES/SBB.txt)

## Testing Strategy

- **Unit Tests**: Standard JUnit tests in `src/test/java`
- **JavaScript Tests**: Mocha tests in `src/test/js`
- **Integration Tests**: Use Testcontainers for WeasyPrint service testing
- **Test Execution**: Maven surefire plugin runs Java tests; frontend-maven-plugin runs JavaScript tests during `verify` phase

## Common Pitfalls

1. **After Changes**: Always delete `<polarion_home>/data/workspace/.config` before restarting Polarion
2. **REST API**: Requires `com.siemens.polarion.rest.enabled=true` in polarion.properties
3. **CORS**: Must be explicitly configured if needed (see README.md)
4. **Debugging**: Use remote debugging on port 5005 (see DEVELOPMENT.md)

## GitHub PR Code Reviews (Automated Workflow)

**Philosophy**: Reviews should be **terse, actionable, and problem-focused**. No praise, no analysis of unchanged code.

**When reviewing PRs via the automated workflow:**
- ONLY review lines changed in the PR diff
- ONLY report actual problems (bugs, security issues, breaking changes, missing tests)
- Use terse format: `[file:line] Problem - Fix: Solution`
- If no issues found, say "No issues found." and stop
- Do NOT: praise code quality, review unchanged code, suggest optional improvements, analyze performance if not changed

**Review categories:**
- 🔴 **Critical**: Bugs, security vulnerabilities, breaking changes
- 🟡 **Important**: Missing tests for new functionality, significant issues

### Skip Reviews For (Automated Tools Handle These)

**The following are already checked by automated tools - DO NOT comment on them:**

**Code Formatting & Style** (handled by parent POM plugins + pre-commit hooks):
- Java code formatting
- Import organization
- Indentation and whitespace
- Line length
- Code style consistency

**Static Analysis** (handled by parent POM plugins):
- SonarCloud integration for code quality
- Static analysis tools configured in parent POM

**Testing** (handled by Maven + Surefire/Failsafe):
- Test execution and coverage
- JUnit test configuration
- JavaScript test execution (Mocha)
- Integration test setup

**Security & Compliance** (handled by pre-commit hooks):
- Sensitive data leak detection (gitleaks)
- Internal URL/email exposure
- UE numbers and DEV ticket numbers
- Secret scanning

**YAML & Documentation** (handled by pre-commit hooks):
- YAML formatting (yamlfix)
- GitHub Actions validation (actionlint, zizmor)
- Commit message format (commitizen)

**Don't suggest these common patterns (already established in codebase):**
- Using Lombok annotations
- JAX-RS patterns for REST endpoints
- OSGi bundle structure
- Polarion extension patterns from parent POM
- Maven plugin configurations already in use
- JavaScript testing with Mocha/Chai
- Testcontainers for integration tests

### Project-Specific Review Focus

**DO focus on:**

1. **Security**:
   - Proper input validation in REST endpoints
   - SQL injection in queries (especially in settings/repository access)
   - XSS vulnerabilities in HTML processing
   - Path traversal in file operations
   - Secrets exposure in logs or error messages
   - CORS configuration if changed

2. **Polarion Integration**:
   - Correct OSGi service registration/unregistration
   - Proper transaction handling with Polarion API
   - Resource cleanup (sessions, connections)
   - Settings inheritance and overrides
   - Widget integration patterns

3. **PDF Export Logic**:
   - HTML transformation pipeline correctness
   - WeasyPrint service error handling
   - Image and table sizing calculations
   - CSS handling and internalization
   - Link resolution (internal vs external)

4. **Async Job Processing**:
   - Proper job lifecycle management
   - Timeout handling
   - Job cleanup and resource management
   - Concurrent job execution safety

5. **Breaking Changes**:
   - REST API endpoint changes
   - Settings structure changes
   - JavaScript API changes
   - OpenAPI specification updates
   - Backward compatibility with existing configurations

6. **Resource Management**:
   - Temporary file cleanup
   - Memory leaks in long-running jobs
   - Database connection handling
   - OSGi service lifecycle

7. **Multi-language Support**:
   - Proper localization handling (XLIFF)
   - Character encoding in exports
   - Velocity template rendering

8. **JavaScript Components**:
   - DOM manipulation safety
   - Event handler cleanup
   - API request error handling
   - Browser compatibility (if UI changes)
