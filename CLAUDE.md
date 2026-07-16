# CLAUDE.md

## Gotchas

- **`ch.sbb.polarion.extension.generic`** is the parent project providing reusable infrastructure for all Polarion plugins in this org (settings framework, REST base classes, OSGi helpers, etc.). Before implementing anything cross-cutting, check if it already exists there.
- **Package naming**: Use `ch.sbb.polarion.extension.pdf_exporter` (underscore). Pre-v7.0.0 code used `pdf.exporter` (dot) — don't follow old patterns still present in the codebase.
- **Maven Settings**: Builds require `.mvn/settings.xml` (JFrog, GitHub Packages, Sonatype credentials via env vars). CI passes it with `-s .mvn/settings.xml`. `.mvn/maven.config` auto-activates the Polarion version profile.
- **Polarion Dependencies**: You must extract dependencies from the Polarion installer using [polarion-artifacts-deployer](https://github.com/SchweizerischeBundesbahnen/polarion-artifacts-deployer) before the Maven build will work.
- **Local Polarion Installation**: Requires `POLARION_HOME` environment variable. Use the `install-to-local-polarion` Maven profile: `mvn clean install -P install-to-local-polarion`
- **After any code change**: Delete `<POLARION_HOME>/data/workspace/.config` before restarting Polarion or changes won't be picked up.
- **Remote Debugging**: Add to Polarion's `config.sh`: `JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"`
- **Logging**: Polarion logs: `<POLARION_HOME>/polarion/logs/main/*.log`
- **Branch conventions**: Conventional commits enforced by commitizen (pre-commit hook). Feature branches: `feature/<name>`, bug fixes: `fix/<name>`, LTS branches: `release-v*` (e.g., `release-v6`).
- **Pre-commit hooks block internal patterns**: some org-specific identifiers are treated as secrets. Run `pre-commit run -a` after implementation.
