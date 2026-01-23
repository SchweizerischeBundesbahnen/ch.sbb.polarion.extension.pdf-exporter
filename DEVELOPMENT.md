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
├── LICENSES/               # License files
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
mvn clean install -P install-to-local-polarion
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
