# CLAUDE.md

## Gotchas

- **`ch.sbb.polarion.extension.generic`** is the parent project providing reusable infrastructure for all Polarion plugins in this org (settings framework, REST base classes, OSGi helpers, etc.). Before implementing anything cross-cutting, check if it already exists there.
- **Two administration UIs at once.** The administration pages are being converted to React on
  [react-sbb-polarion](https://github.com/grigoriev/react-sbb-polarion) one at a time, so two webapps
  serve them side by side: `pdf-exporter-app` (the Vite bundle in `ui/`, see [`ui/README.md`](ui/README.md))
  and the legacy `pdf-exporter-admin` (the remaining JSP pages). `hivemodule.xml` carries a `pageUrl`
  per menu entry, which is what makes the split possible; the ids there must match `ui/src/features.tsx`.
  Converted so far: About, Usage Disclaimer, User Guide, Authorization, Style Package Weights,
  CSS, Cover Page, Header and Footer, Filename template, Localization, Webhooks. Left on JSP: style
  packages. `pdf-exporter-admin` is deleted once the last JSP page is gone.
- **The UI build comes from the generic parent**, activated by the presence of `ui/package.json` (its
  `vite-ui` profile): `npm ci` + `npm run build`, the bundle copied into `webapp/pdf-exporter-app/`, and
  the JS suite in the Maven `test` phase. This pom adds nothing for it. Note it also redirects
  markdown2html's output (`about.html`, `user-guide.html`, `disclaimer.html`) into
  `webapp/pdf-exporter-app/html/`. The separate `frontend-maven-plugin` block in this pom is unrelated -
  it builds and tests the **product** JS (the export dialog and the DLE toolbar).
- **Package naming**: Use `ch.sbb.polarion.extension.pdf_exporter` (underscore). Pre-v7.0.0 code used `pdf.exporter` (dot) — don't follow old patterns still present in the codebase.
- **Maven Settings**: Builds require `.mvn/settings.xml` (JFrog, GitHub Packages, Sonatype credentials via env vars). CI passes it with `-s .mvn/settings.xml`. `.mvn/maven.config` auto-activates the Polarion version profile.
- **Polarion Dependencies**: You must extract dependencies from the Polarion installer using [polarion-artifacts-deployer](https://github.com/SchweizerischeBundesbahnen/polarion-artifacts-deployer) before the Maven build will work.
- **Local Polarion Installation**: Requires `POLARION_HOME` environment variable. Use the `install-to-local-polarion` Maven profile: `mvn clean install -P install-to-local-polarion`
- **After any code change**: Delete `<POLARION_HOME>/data/workspace/.config` before restarting Polarion or changes won't be picked up.
- **Remote Debugging**: Add to Polarion's `config.sh`: `JAVA_OPTS="$JAVA_OPTS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"`
- **Logging**: Polarion logs: `<POLARION_HOME>/polarion/logs/main/*.log`
- **Branch conventions**: Conventional commits enforced by commitizen (pre-commit hook). Feature branches: `feature/<name>`, bug fixes: `fix/<name>`, LTS branches: `release-v*` (e.g., `release-v6`).
- **Pre-commit hooks block internal patterns**: some org-specific identifiers are treated as secrets. Run `pre-commit run -a` after implementation.
