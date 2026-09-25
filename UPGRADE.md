# Upgrade

Version-specific upgrade notes for the PDF Exporter extension. See the [README](README.md) for
installation and the [configuration reference](CONFIGURATION.md) for all settings.

## Upgrade from version 13.x.x to 13.9.0

The **User Guide** administration entry is replaced by a single **Documentation** entry. It opens a
documentation site - Quick Start, User Guide, Configuration, Limitations and Upgrade - with its own
navigation, search and cross-links between the articles, so none of these articles has a menu entry of
its own.

The admin node id changed with it, from `user-guide` to `documentation`. A bookmark or a link that points
at the old node, e.g. `#/administration/pdf-export/user-guide` or
`#/project/<id>/administration/pdf-export/user-guide`, no longer opens a page: replace `user-guide` with
`documentation` in it. The User Guide itself is one click away in the documentation sidebar.

## Upgrade from version 13.7.x to 13.8.0

### Custom values of CSS, Cover page, Header and Footer, and Filename template

A new setting of these sections starts with empty custom values. Before, it started with a copy of the default values.

A stored copy of the default values, of the current version or of a former one, reads as empty while it is not in use. For CSS, the default CSS is then no longer
applied twice. For the other sections, the page no longer shows a stale copy. No action is required.

A copy which was edited afterwards stays as it is. Use `Compare with default` to check it against the current default values.

Choosing the default values no longer erases the custom values.

## Upgrade to weasyprint-service 67.0.0

WeasyPrint 67.0 introduced breaking changes in PDF variant support:

**Removed variant:**
- `pdf/a-4b` - no longer supported by WeasyPrint 67.0

**New variants added:**
- `pdf/a-1a` - Accessible PDF/A-1 (tagged, Unicode)
- `pdf/a-2a` - Accessible PDF/A-2 (tagged, Unicode, modern features)
- `pdf/a-3a` - Accessible PDF/A-3 (tagged, Unicode, file attachments)
- `pdf/a-4e` - PDF/A-4 for engineering documents (allows 3D, RichMedia)
- `pdf/a-4f` - PDF/A-4 with embedded files (requires attachments in document)
- `pdf/ua-2` - Accessible PDF for assistive technologies (ISO 14289-2:2024) - **partial support, see Limitations**

**Post-processing applied automatically:**

The extension applies post-processing to ensure PDF/A and PDF/UA compliance:

| PDF Variant | Post-processing |
|-------------|-----------------|
| PDF/A-1a, PDF/A-1b | Removes transparency masks (SMask, Mask) from images; adds RoleMap for TBody, THead, TFoot structure types; conformance level passed to processor |
| PDF/A-4e, PDF/A-4f, PDF/A-4u | Sets PDF version to 2.0; fixes `pdfaid:rev` to "2020"; ensures OutputIntent; handles conformance level |
| PDF/UA-2 | Fixes `pdfuaid:rev` to "2024" |

**Migration steps:**
1. If you were using `pdf/a-4b` variant, switch to `pdf/a-4f` or `pdf/a-4e` instead
2. Update any style packages that reference `PDF_A_4B` to use `PDF_A_4F` or `PDF_A_4E`
3. Update weasyprint-service Docker image to version 67.0.0 or later
4. Note: `pdf/a-4f` requires documents to have attachments (embedded files) per ISO 19005-4:2020 clause 6.9

## Upgrade from version 11.x.x to 12.0.0

In version 12.0.0 Font Awesome has been upgraded from version 4.0.3 to version 6.2.0 to support Polarion 2512.

**What changed:**
- Default CSS now includes Font Awesome 6 font definitions instead of Font Awesome 4
- PDF template now loads `/polarion/ria/fontawesome-6.2.0/css/all.min.css` instead of `/polarion/ria/font-awesome-4.0.3/css/font-awesome.css`
- Cover page templates (English/German) have been updated with Font Awesome 6 font definitions

**Action required:**
If you have custom CSS settings that include Font Awesome 4 `@font-face` definitions, you need to update them to Font Awesome 6:

```css
/* Old Font Awesome 4 (remove this) */
@font-face {
    font-family: 'FontAwesome';
    src: url('/polarion/ria/font-awesome-4.0.3/fonts/fontawesome-webfont.woff') format('woff'),
    url('/polarion/ria/font-awesome-4.0.3/fonts/fontawesome-webfont.ttf') format('truetype');
}

/* New Font Awesome 6 (add this) */
@font-face {
    font-family: 'Font Awesome 6 Free';
    font-style: normal;
    font-weight: 900;
    src: url('/polarion/ria/fontawesome-6.2.0/webfonts/fa-solid-900.woff2') format('woff2'),
    url('/polarion/ria/fontawesome-6.2.0/webfonts/fa-solid-900.ttf') format('truetype');
}

@font-face {
    font-family: 'Font Awesome 6 Free';
    font-style: normal;
    font-weight: 400;
    src: url('/polarion/ria/fontawesome-6.2.0/webfonts/fa-regular-400.woff2') format('woff2'),
    url('/polarion/ria/fontawesome-6.2.0/webfonts/fa-regular-400.ttf') format('truetype');
}

@font-face {
    font-family: 'Font Awesome 6 Brands';
    font-style: normal;
    font-weight: 400;
    src: url('/polarion/ria/fontawesome-6.2.0/webfonts/fa-brands-400.woff2') format('woff2'),
    url('/polarion/ria/fontawesome-6.2.0/webfonts/fa-brands-400.ttf') format('truetype');
}
```

## Upgrade from version 9.0.0 to 9.x.x

Due to a conflict of HTML elements' IDs in default cover page templates, their HTML/CSS were modified
to make IDs more unique and to replace IDs by classes where relevant. If you ever created your custom cover page templates
based on default ones, you can be interested in revising these changes and replicate them in your custom templates. Otherwise,
if you always used either default template(-s) or/and custom templates not inherited from default ones, no additional actions required.

## Upgrade from version 8.x.x to 9.0.0

In version 9.0.0 support for Polarion 2410 and older has been dropped. This extension supports only Polarion 2506.

PDF variant configuration has been moved to style package instead of using configuration properties.

## Upgrade from version 8.1.3 to 8.2.0

Sections Cover page, CSS, Header and Footer, Filename template were modified to split custom values (provided by user) and default values (provided by application, which can't be modified by user).
For a smooth migration from previous version we consider all already stored settings as custom, even if they contained default values, not to erase all your already stored settings.
Be aware that you'll need to tidy this up, storing explicitly either your custom values or switching to use default values.

## Upgrade from version 7.x.x to 8.0.0

In version 8.0.0 support for Polarion 2404 and older has been dropped. This extension supports only Polarion 2410.
Recommended version of WeasyPrint Service is 63.1.0.

## Upgrade from version 6.x.x to 7.0.0

In version 7.0.0 `/export-filename` REST API endpoint changed. As a result, if the endpoint has been used, it's required to adjust the calls accordingly.

`DocumentType` enum in `ExportParams` has been changed. As a result, if enum values have been used, it's required to adjust the calls accordingly.

Main package has been renamed from `ch.sbb.polarion.extension.pdf.exporter` to `ch.sbb.polarion.extension.pdf_exporter`. As a result, if the extension has been used in another OSGi bundles, it's required to adjust the package imports accordingly.

There was also added a CSS fragment for better display of Test Run pages in PDF, please add this fragment to your CSS definitions if they differ from default one, or update your CSS definitions via UI clicking button "Default" and later saving it. Here is this fragment:
```css
#polarion-rp-widget-content > .polarion-TestRunOverviewWidget-table > tbody > tr > td:first-child {
   width: 46% !important;
}
.polarion-TestRunOverviewWidget-buttonName {
   padding-top: 20px;
}
```

In version 7.1.0 the property `ch.sbb.polarion.extension.pdf-exporter.internalizeExternalCss` has been removed. `polarion.properties` should be updated accordingly.

## Upgrade from version 5.x.x to 6.0.0

In version 6.0.0 WeasyPrint CLI support was removed. As a result, if WeasyPrint CLI has been using to generate PDFs, it's required to switch to [WeasyPrint Service](CONFIGURATION.md#weasyprint-configuration).

The configuration properties `ch.sbb.polarion.extension.pdf-exporter.weasyprint.connector` and `ch.sbb.polarion.extension.pdf-exporter.weasyprint.executable` have been removed due to the removal of WeasyPrint CLI support. `polarion.properties` should be updated accordingly.

## Upgrade from version 4.x.x to 5.0.0
In version 5.0.0 not only label of configuration parameter "Fit images and tables to page width" was modified to be "Fit images and tables to page",
but also underlying parameter was renamed to reflect this change. As a result if you had "Fit images and tables to page width" ticked in your configuration prior to version 5.0.0,
after installation of this version you will have to go to configuration again and re-tick property "Fit images and tables to page", both on global repository level and on level of projects.

Another change is default CSS which was modified to reflect different possible paper sizes as well as additional styling for images to jump into next page if they can't be fully displayed on current one.
Thus please either reset your saved CSS into last version if you didn't have your own CSS definitions or merge your saved version with new default version.
