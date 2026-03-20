# CLAUDE.md

## Landmines

- **`ch.sbb.polarion.extension.generic`** is the parent project providing reusable infrastructure for all Polarion plugins in this org (settings framework, REST base classes, OSGi helpers, etc.). Before implementing anything cross-cutting, check if it already exists there.

- **Package naming**: Use `ch.sbb.polarion.extension.pdf_exporter` (underscore). Pre-v7.0.0 code used `pdf.exporter` (dot) — don't follow old patterns still present in the codebase.
- **After any code change**: Delete `<polarion_home>/data/workspace/.config` before restarting Polarion or changes won't be picked up.
- **Pre-commit hooks block SBB-specific patterns**: internal URLs, UE numbers, and DEV ticket numbers are treated as secrets. Run `pre-commit run -a` after implementation.

## GitHub PR Code Reviews

Reviews must be **terse, actionable, problem-focused**. No praise, no analysis of unchanged code. Only comment on changed lines.

**Skip** (automated tools already check these): code formatting, import order, SonarCloud findings, YAML/action linting, commit message format, secret scanning.

**Focus on:**
- Security: input validation in REST endpoints, XSS in HTML processing, path traversal in file ops, secrets in logs/errors
- Polarion integration: OSGi service lifecycle, transaction handling, resource cleanup
- PDF export: HTML transformation pipeline correctness, WeasyPrint error handling
- Async jobs: lifecycle management, timeout handling, concurrent execution safety
- Breaking changes: REST API, settings structure, OpenAPI spec, backward compatibility with existing configs
