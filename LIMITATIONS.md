# Limitations and Workarounds

This page collects content-specific behaviour that is by design and is therefore **not** changed by
default, together with a workaround you can apply yourself. Depending on the entry the workaround is a
CSS snippet added to your style package's **CSS** setting (Administration → PDF Exporter → CSS), an
export option, or another style-package setting — each entry states which one to use.

These are kept out of the defaults on purpose: the right behaviour depends on the concrete document
(its columns, content and language), and changing a global default would affect every export and could
regress other documents.

> For PDF/A and PDF/UA compliance limitations, see the [Limitations](README.md#limitations) section of the README.

## Table column sizing in wide tables

### Behavior (by design)

In a wide table, a column that holds a lot of text (a long URL or description) takes most of the
width, and the remaining columns are squeezed so narrow that their headers, IDs and dates are broken
**character by character** — for example `Disappro/ved`, `Gültig/ab`, `TMSPRG-1/3164`, `2022-01-/05`.

This is because the default export CSS lets any table cell break a word at any character so that long
tokens (such as URLs) never overflow the page:

```css
table tr th, table tr td {
    overflow-wrap: anywhere;
    white-space: normal;
}
```

As a side effect, `overflow-wrap: anywhere` also lets a cell shrink to about one character wide, so the
table auto-layout can collapse the narrow columns and break their short text arbitrarily.

### Workaround

Keep the table **headers** from being split character by character. This also gives each column at
least the width of its header, so the narrow columns stop collapsing:

```css
/* Use "table tr th" (not just "th"): a plain "th { ... }" will NOT override the default
   "table tr th, table tr td { overflow-wrap: anywhere }" because it has lower specificity. */
table tr th {
    overflow-wrap: normal;   /* never break a header word at an arbitrary character */
    word-break: normal;
    hyphens: auto;           /* a long single-word header breaks at syllables, with a real hyphen */
}
```

Notes:

- **Specificity matters.** The selector must be `table tr th`, otherwise it does not win against the
  default rule and nothing changes.
- `hyphens: auto` only takes effect when the **document language** is set, and that is opt-in: enter the ID of the
  LiveDoc custom field holding the language in the style package's **Document Language custom field** setting, and
  set that field on the document — see
  [Document language custom field (hyphenation)](USER_GUIDE.md#document-language-custom-field-hyphenation).
  Without a language the header is simply not hyphenated (the column may then be as wide as the longest header
  word), but the text is still never broken mid-character. A long single-word header (e.g.
  `Verantwortlichkeitsbereich`) needs the language to break at syllables so that a "long header / short content"
  column does not stay wide.
- If a **body** cell with a long identifier (not a link) still breaks, protect that column too by giving
  its cells `white-space: nowrap`.

Verified against the WeasyPrint engine used by the extension: with the workaround, headers, IDs and
dates stay intact, long single-word headers hyphenate so their column adapts to the content, and long
link URLs still wrap.

## Fit-to-page does not preserve Polarion-defined table cell widths

### Behavior (by design)

With **Fit images and tables to page width** enabled, the export replaces absolute table cell widths
defined in Polarion (e.g. `width: 269px`) with `width: auto` (`HtmlProcessor.adjustCellWidth`) so that
wide tables stay within the page. As a result the exact column widths set in the document are not
preserved.

### Workaround

- Export **without** *Fit images and tables to page width* to keep the Polarion-defined widths — note
  that very wide tables may then overflow the page.
- Or restore the widths with a **webhook** (available since v6.1.0) that adjusts the HTML before it is
  sent to the WeasyPrint service; see the
  [webhook samples repository](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter.webhook-samples)
  for an example. Note that webhooks are disabled by default and must first be enabled with
  `ch.sbb.polarion.extension.pdf-exporter.webhooks.enabled=true` in `polarion.properties` (see the README's
  [Enabling webhooks](README.md#enabling-webhooks) section).

See discussion [#30](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/discussions/30).

## Square brackets in a work item title break titles rendered via the Wiki rendering API

### Behavior (Polarion limitation)

When a wiki page renders work items with the rendering API, e.g.
`$transaction.workItems.getBy.ids("PROJECT", "ID").render()`, and a rendered work item's **title
contains square brackets** (`[` or `]`), the title is corrupted in the exported PDF — part of it is
shown in front of the work item icon.

The cause is Polarion's XWiki rendering engine: it renders the work item to HTML and then runs that HTML
through `DefaultXWikiRenderingEngine.renderDocument()`, which interprets `[ ]` as wiki link markup
(`[text>url]`) and corrupts the title before the PDF exporter ever receives it. Polarion's own standard
PDF export has the same problem, so the extension cannot fix it.

### Workaround

Avoid square brackets `[ ]` in work item titles that are rendered through the Wiki rendering API — the
reporter confirmed that removing the brackets renders the title correctly. As the root cause is in the
XWiki engine, it is worth reporting to Siemens as a Polarion platform bug.

See issue [#739](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/739).
