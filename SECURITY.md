# Security Policy

## Reporting a vulnerability

Report a suspected vulnerability through GitHub private vulnerability reporting:
[Report a vulnerability](../../security/advisories/new).

The report stays private between you and the maintainers.

**Do not open a public issue, a pull request or a discussion for a security problem.** A public
report exposes every installation until a fix is released.

Please include:

1. The affected extension version and the Polarion version.
2. A description of the vulnerability and its impact.
3. Step by step instructions to reproduce it.
4. Any proof of concept, log excerpt or screenshot you have.

Attach files directly to the advisory. Report attachments to other channels can get lost.

## What happens next

1. We acknowledge the report within 5 working days.
2. We assess it and tell you our conclusion, as a rule within 10 working days.
3. We agree a disclosure date with you.
4. We prepare a fix and release it.
5. We publish a GitHub Security Advisory and request a CVE.

We credit the reporter in the advisory. Tell us the name you want, or tell us that you prefer to
stay anonymous.

Please keep the report confidential until the advisory is published.

## Supported versions

Fixes go into the latest released version only. This extension supports the latest Polarion
version, currently Polarion 2606, so older lines are not maintained.

## Scope

This policy covers the code in this repository.

Report a problem in one of these components to its own repository:

- [ch.sbb.polarion.extension.generic](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.generic),
  the shared infrastructure of the SBB Polarion extensions.
- [weasyprint-service](https://github.com/SchweizerischeBundesbahnen/weasyprint-service), the
  conversion service this extension calls.

Report a problem in Polarion itself to Siemens.
