# Repository Coding Standards

The purpose of the Coding Standards is to create a baseline for collaboration and review within various aspects of our open source project and community, from core code to themes to plugins.

Coding standards help avoid common coding errors, improve the readability of code, and simplify modification. They ensure that files within the project appear as if they were created by a single common unit.

Following the standards means anyone will be able to understand a section of code and modify it, if needed, without regard to when it was written or by whom.

If you are planning to contribute, you need to familiarize yourself with these standards, as any code you submit will need to comply with them.

## Java Coding Standards and Best Practices

This document outlines the coding standards and best practices to be followed when writing Java code for this project. Consistency in coding style and adherence to best practices not only improve code readability but also facilitate collaboration and maintenance.

### Coding Standards Guidelines

Please refer to the following coding standards guidelines for detailed recommendations:

1. [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) - Google's Java style guide offers comprehensive guidelines on coding style, naming conventions, documentation, and more.

2. [Oracle's Java Code Conventions](https://www.oracle.com/java/technologies/javase/codeconventions-contents.html) - Oracle provides a set of conventions for writing Java code, covering formatting, naming conventions, and other aspects of coding style.

### Linters

We recommend using the following linters to enforce coding standards and best practices in your Java code:

1. [Checkstyle](https://checkstyle.org/) - Checkstyle is a static code analysis tool that checks Java code against a set of coding standards. It can detect violations of coding conventions, potential bugs, and other code quality issues.

2. [FindBugs](http://findbugs.sourceforge.net/) - FindBugs is a static analysis tool that detects potential bugs in Java code. It can identify common programming errors, performance issues, and security vulnerabilities.

3. [PMD](https://pmd.github.io/) - PMD is a source code analyzer that finds common programming flaws like unused variables, empty catch blocks, and unnecessary object creation. It provides actionable feedback to improve code quality.

4. [SpotBugs](https://spotbugs.github.io/) - SpotBugs is the successor of FindBugs, offering more features and improved bug detection capabilities. It performs static analysis to identify bugs and other issues in Java bytecode.

## Python Coding Standards and Best Practices

This document outlines the coding standards and best practices to be followed when writing Python code for this project. Consistency in coding style and adherence to best practices not only improve code readability but also facilitate collaboration and maintenance.

### Coding Standards Guidelines

Please refer to the following coding standards guidelines for detailed recommendations:

1. [PEP 8](https://pep8.org/) - Python Enhancement Proposal 8 provides the de facto style guide for Python code, covering formatting, naming conventions, and more.

2. [Google Python Style Guide](https://google.github.io/styleguide/pyguide.html) - Google's Python style guide offers comprehensive guidelines on coding style, naming conventions, documentation, and more.

### Linters

We recommend using the following linters to enforce coding standards and best practices in your Python code:

1. [flake8](https://flake8.pycqa.org/en/latest/) - Flake8 combines multiple linters including pycodestyle, pyflakes, and McCabe complexity checker to analyze your code against the PEP 8 style guide and detect various errors and inconsistencies.

2. [pylint](https://pylint.pycqa.org/) - Pylint analyzes Python code for errors, potential bugs, and code smells, providing detailed reports with suggestions for improvement.

3. [black](https://black.readthedocs.io/en/stable/) - Black is an opinionated code formatter for Python that automatically reformats your code to ensure consistent style adherence.

4. [mypy](http://mypy-lang.org/) - Mypy is a static type checker for Python that helps detect and prevent type-related errors using optional static typing.


## Docker Best Practices and Guidelines

This document outlines the best practices and guidelines to be followed when working with Docker for this project. Adhering to these practices ensures consistency, security, and efficiency in Dockerfile and container management.

### Best Practices Guidelines

Please refer to the following best practices guidelines for detailed recommendations:

1. [Docker Official Best Practices](https://docs.docker.com/develop/develop-images/dockerfile_best-practices/) - Docker's official documentation provides a comprehensive guide on best practices for writing Dockerfiles. It covers various aspects including image size optimization, layer caching, and security considerations.

2. [Google Container Best Practices](https://cloud.google.com/solutions/best-practices-for-building-containers) - Google Cloud offers best practices for building and deploying containers, covering topics like image security, resource optimization, and container orchestration.

3. [Red Hat Container Best Practices](https://www.redhat.com/en/topics/containers/container-best-practices) - Red Hat provides best practices for building and managing containers, focusing on security, performance, and scalability aspects.

4. [Microsoft Dockerfile best practices](https://docs.microsoft.com/en-us/dotnet/architecture/microservices/docker-application-development-process/docker-app-development-workflow) - Microsoft offers best practices for Dockerfile development, including multi-stage builds, image tagging, and Docker Compose usage.

### Linters and Static Analysis Tools

While Dockerfiles are not typically linted like source code, you can use static analysis tools to ensure adherence to best practices and detect potential issues:

1. [Hadolint](https://github.com/hadolint/hadolint) - Hadolint is a Dockerfile linter that checks Dockerfiles for common mistakes and best practices violations. It provides suggestions for improving Dockerfile quality and security.

2. [Docker Bench for Security](https://github.com/docker/docker-bench-security) - Docker Bench for Security is a script that checks for dozens of common best-practices around deploying Docker containers in production.

3. [Trivy](https://github.com/aquasecurity/trivy) - Trivy is a vulnerability scanner for containers and other artifacts, providing detailed reports on security vulnerabilities in Docker images.

---

This setup provides both the high-level guidelines for coding standards and best practices, as well as practical tools (linters) that can be used to enforce these standards in your project.
