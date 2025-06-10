# Contributing
We appreciate all kinds of contributions. The following is a set of guidelines for contributing to this repository on GitHub.
These are mostly guidelines, not rules. Use your best judgment, and feel free to propose changes to this document in a pull request.

## Table Of Contents
[Prerequisites](#prerequisites)

[Asking Questions](#asking-questions)

[What should I know before I get started?](#what-should-i-know-before-i-get-started)
* [Tools and Packages](#tools-and-packages)
* [Design Decisions](#design-decisions)

[How Can I Contribute?](#how-can-i-contribute)
* [Reporting Bugs](#reporting-bugs)
* [Suggesting Enhancements](#suggesting-enhancements)
* [Submitting Changes](#submit-changes)
* [Commit Message Guidelines](#commit-message-guidelines)
* [Coding Rules](#coding-rules)

## <a id="prerequisites"></a>Prerequisites
This project and everyone participating in it are governed by our [Code of Conduct](https://github.com/SchweizerischeBundesbahnen/.github/blob/main/CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

All contributors must have an active Polarion license. An active Polarion license means that the contributor has access to a valid Polarion license issued by Siemens, including but not limited to customer, partner, academic, trial, or demo Polarion license.

## <a id="asking-questions"></a>Asking questions
Do not know how something in this project works? Curious if this project can achieve your desired functionality? Please ask questions in this project discussions [here](../../discussions)

## <a id="what-should-i-know-before-i-get-started"></a>What should I know before I get started?

### <a id="tools-and-packages"></a>Tools and Packages
All extensions provided by SBB Polarion Team can be built, tested and packaged using Maven.
It is only possible when the dependencies are extract from Polarion installer. The process must be performed by each contributor. Please consider to use https://github.com/SchweizerischeBundesbahnen/polarion-artifacts-deployer to extract the dependencies for your own Polarion installer version.

For detailed information about setting up your development environment, please refer to the [Development Guide](./DEVELOPMENT.md).

### <a id="design-decisions"></a>Design Decisions
The generic implementation for extensions provided by SBB Polarion Team is located in [ch.sbb.polarion.extension.generic](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic)

## <a id="how-can-i-contribute"></a>How Can I Contribute?

### <a id="reporting-bugs"></a>Reporting Bugs
To report a bug, [submit an issue](../../issues/new) with the label `bug`. Please ensure the bug has not already been reported. **If the bug is a potential security vulnerability, please report it using our [security policy](https://github.com/SchweizerischeBundesbahnen/.github/blob/main/SECURITY.md).**

Providing the following information will increase the chances of your issue being dealt with quickly:

* **Overview of the Issue** - if an error is being thrown a non-minified stack trace helps
* **Toolchain and Environment Details** - which versions of libraries, toolchain, platform etc
* **Motivation for or Use Case** - explain what are you trying to do and why the current behavior is a bug for you
* **Browsers and Operating System** - is this a problem with all browsers?
* **Reproduce the Error** - provide a live example or a unambiguous set of steps
* **Screenshots** - maybe screenshots can help the team to triage issues far more quickly than a text description
* **Related Issues** - has a similar issue been reported before?
* **Suggest a Fix** - if you can't fix the bug yourself, perhaps you can point to what might be causing the problem (line of code or commit)

You can help the team even more by [submitting changes](#submitting-changes) with a fix.

### <a id="suggesting-enhancements"></a>Suggesting Enhancements
To suggest a feature or enhancement, please [submit an issue](../../issues/new) with the label `enhancement`. Please ensure the feature or enhancement has not already been suggested.

Please consider what kind of change it is:

* For a **Major Feature**, first open an issue and outline your proposal so that it can be discussed. This will also allow us to better coordinate our efforts, prevent duplication of work, and help you to craft the change so that it is successfully accepted into the project.
* **Small Features** can be crafted and directly [submitted changes](#submitting-changes).

### <a id="submit-changes"></a>Submitting Changes
Before you submit your Pull Request (PR) consider the following guidelines:

* Make your changes in a new git branch:

     ```shell
     git checkout -b my-fix-branch main
     ```

* Create your patch, **including appropriate test cases**.
* Follow our [Coding Rules](#coding-rules).
* Test your changes with our supported browsers and screen readers.
* Run tests and ensure that all tests pass.
* Commit your changes using a descriptive commit message that follows our
  [commit message conventions](#commit-message-guidelines). Adherence to these conventions
  is necessary because release notes are automatically generated from these messages.

     ```shell
     git commit -a --gpg-sign
     ```
  Note: The optional commit `-a` command line option will automatically "add" and "rm" edited files.

  Note: The command line option `-S/--gpg-sign` generates a signed commit, which is required to make a contribution (See [Developer Certificate of Origin](./LICENSES/DCO.txt))

* Push your branch to GitHub:

    ```shell
    git push my-fork my-fix-branch
    ```

* In GitHub, send a pull request to `sbb-your-project:main`.
  The PR title and message should as well conform to the [commit message conventions](#commit-message-guidelines).

### <a id="commit-message-guidelines"></a>Commit Message Guidelines
This project uses [Conventional Commits](https://www.conventionalcommits.org/) to generate the Changelog using the [Release Please GitHub action](.github/workflows/release-please.yml).
For comprehensive information, please consult the [Release Please documentation](https://github.com/googleapis/release-please).

### <a id="coding-rules"></a>Coding Rules
To ensure consistency throughout the source code, keep these rules in mind as you are working:

* All features or bug fixes **must be tested** by one or more specs (unit-tests).
* All API methods **must be documented**.
* Also see [CODING_STANDARDS.md](./CODING_STANDARDS.md)
