# Changelog

## [11.1.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v11.0.0...v11.1.0) (2025-12-19)


### Features

* add debug data and detailed timing reports ([#663](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/663)) ([0599e23](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/0599e234e56c22587cf366e4d2b48a54eefdf2a6)), closes [#660](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/660)
* add support for pageFormat attribute of pd4ml:page.break tag ([#667](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/667)) ([5dbba39](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/5dbba39c98b4411606a7b0708fd76860fc372371)), closes [#635](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/635)
* generic v12.1.0 ([#661](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/661)) ([5995fd7](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/5995fd7be799f09dc1673919d6ab53dce28feee5))
* warn user that comments rendered as sticky notes are not compliant with PDF/A variants ([#666](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/666)) ([1eb4c59](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/1eb4c590037ca04283316d7489ff54156f595292))


### Bug Fixes

* 'Specific Workitem roles' checkbox is always checked on the side ([#669](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/669)) ([3f0e6ad](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/3f0e6addc77d58cf6c459daa6b8117aeaa092bb0)), closes [#668](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/668)
* **deps:** update dependency com.helger:ph-css to v8.1.1 ([86d3be0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/86d3be04b4045c2250af10ff421192c5846b4cea))
* **deps:** update dependency org.testcontainers:testcontainers-bom to v2.0.3 ([1d1c95f](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/1d1c95f3f35bb214e5c0876ffbb00bc877e0287c))
* fix cross-reference links in body of WorkItems ([#662](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/662)) ([a178aee](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/a178aee9b2fe6a96e9fbe850aafb2ceed5eac2d7)), closes [#656](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/656)


### Documentation

* clarify PDF/A incompatibility with sticky notes and suggest workaround ([#657](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/657)) ([c386012](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/c386012d10713dd8d707c00673b14e357b9d7491)), closes [#650](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/650)

## [11.0.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v10.1.2...v11.0.0) (2025-12-10)


### ⚠ BREAKING CHANGES

* add support for additional PDF/A and PDF/UA variants and update to WeasyPrint 67.0 ([#652](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/652))

### Features

* add support for additional PDF/A and PDF/UA variants and update to WeasyPrint 67.0 ([#652](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/652)) ([5aa6727](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/5aa6727154be09e328df864d499857448293fb58)), closes [#650](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/650)


### Documentation

* add workaround for icon fonts issue in PDF/A accessible variants and update related documentation ([#654](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/654)) ([8a74e77](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/8a74e771ab42c376e8899b0529f85a9824299d17)), closes [#650](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/650)

## [10.1.2](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v10.1.1...v10.1.2) (2025-12-08)


### Bug Fixes

* **ci:** replace flaky Swagger Editor Validator with Redocly CLI ([#641](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/641)) ([9c184c8](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/9c184c80b44b0186e5af9f046a1698545d8b53a0)), closes [#640](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/640)
* **deps:** update dependency net.bytebuddy:byte-buddy to v1.18.2 ([4e0d20f](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/4e0d20f8dbcaca24b3e9ae95706dbf4dd99e88bd))
* **deps:** update dependency org.apache.commons:commons-text to v1.15.0 ([4dd15c5](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/4dd15c546e39260a7e1a7c52ebf8e9df2f3e4537))
* **deps:** update dependency org.apache.tika:tika-core to v3.2.3 ([b839890](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/b83989020e7fde6b871dbc111130b56b745e228b))
* generation of table of figures and tables ([#631](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/631)) ([05e4bcd](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/05e4bcdbfa147e9fe7400df3929b4794e5c3986f)), closes [#630](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/630)
* resolved LiveDoc comments being displayed inside workitems ([#642](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/642)) ([a773bd7](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/a773bd761ebe7c5aee0d35c8693f905caf787120)), closes [#638](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/638)
* use actual Apache Tika library version ([#646](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/646)) ([87de7a6](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/87de7a6b0487426940a55456935d2996ed3edba0)), closes [#645](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/645)

## [10.1.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v10.1.0...v10.1.1) (2025-11-26)


### Bug Fixes

* **deps:** update dependency net.bytebuddy:byte-buddy to v1.18.0 ([6bfb1a6](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/6bfb1a67e31eedf628ff81e98006530b81be50cd))
* **deps:** update dependency net.bytebuddy:byte-buddy to v1.18.1 ([8da2949](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/8da29491b5b524ae51e876aea96a404409b9f34e))
* **deps:** update dependency org.testcontainers:testcontainers-bom to v2.0.2 ([10373ec](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/10373ecbacad82150f7a83b4d8c2b869045b8c54))
* resolved media type mismatch check with login-page redirect dete… ([#622](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/622)) ([70b301c](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/70b301cff23bc49289ee77922f6ef900409f14a9)), closes [#621](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/621)

## [10.1.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v10.0.1...v10.1.0) (2025-11-07)


### Features

* ability to export comments as sticky notes ([#587](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/587)) ([adcc532](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/adcc532fc73b2405ca9e57be5ae26b1c26194f7b)), closes [#576](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/576)
* export comments within a workitem description ([#577](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/577)) ([0e3e571](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/0e3e5717e406ff161aedcc3059ca5a06bb216e12)), closes [#551](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/551)
* PDF/A-1b compliance ([#608](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/608)) ([231197a](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/231197af0214276e1091cec82367333f8910b1fe))
* validate resulting pdfs using verapdf ([#604](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/604)) ([48b1a4d](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/48b1a4d175587f08e0e86c216d99b3d3ad4f032e)), closes [#603](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/603)


### Bug Fixes

* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v11.3.0 ([acc64d4](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/acc64d42c3a1bbc05b2dcc964a1c370d3b87fa0d))
* PDF/A-4 metadata compliance limitation ([#606](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/606)) ([bad341e](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/bad341eeedfdf5e38cd0f619e500fa36e3d3c7b7)), closes [#605](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/605)

## [10.0.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v10.0.0...v10.0.1) (2025-10-17)


### Bug Fixes

* **deps:** update dependency org.apache.pdfbox:pdfbox to v3.0.6 ([e11731c](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/e11731c72fc858d5fdb7b43e4c0171fbdd9af06b))
* **deps:** update dependency org.testcontainers:testcontainers-bom to v2.0.1 ([62be70f](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/62be70f743e8b66ab8f71f21d36becbf9842ff7b))
* Revert "feat: export comments within a workitem description ([#559](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/559))" ([#574](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/574)) ([dccaed7](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/dccaed741ed1f6e70265e44578bfc1bc2847d702))

## [10.0.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v9.4.4...v10.0.0) (2025-10-15)


### ⚠ BREAKING CHANGES

* Add disclaimer of the extension usage ([#569](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/569))

### Features

* ability to export pages during batch collections processing ([#555](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/555)) ([fcbc89d](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/fcbc89d4f7fdae5466998c84a7a7465df8822329)), closes [#553](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/553)
* Add disclaimer of the extension usage ([#569](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/569)) ([22bfcaf](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/22bfcafd16120ab5c73135ea7be54ad3903a1cb2)), closes [#566](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/566)
* convert wi links to internal document references ([#535](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/535)) ([01add64](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/01add6427444ae9567a10945f1276aabd6b6263f))
* export comments within a workitem description ([#559](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/559)) ([5005356](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/50053565105e9eea82a79c5d0aaea2a2576a77a6)), closes [#551](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/551)
* fix merged cells in table header that span multiple rows ([#565](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/565)) ([919fd79](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/919fd798ed8973059f792223ba9ed0904429fdb7)), closes [#556](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/556)
* Improve scaling of images inside tables when "fit to page" option is selected. Scaled images will be closer to their original size proportionally. ([#557](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/557)) ([59b97cf](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/59b97cf3c6435a4015bda5a6f034635c44c48221))


### Bug Fixes

* **deps:** update dependency net.bytebuddy:byte-buddy to v1.17.8 ([61a383c](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/61a383c160400b6c52a8dead308b6a16673e767f))
* enable PR comment posting in Claude Code Review workflow ([1fa2a3d](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/1fa2a3d1e8cb421f5303c507e75dc565653dc818))
* TOC generation with escaped angle brackets ([#564](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/564)) ([8466fd3](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/8466fd389cdade5ba408cd4a0285644a33ea7ca0)), closes [#563](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/563)


### Documentation

* Add GitHub PR code review guidelines to CLAUDE.md ([#561](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/561)) ([57486d4](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/57486d441eb41d9491df08e87428fedb0f7cc05e))

## [9.4.4](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v9.4.3...v9.4.4) (2025-10-02)


### Bug Fixes

* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v11.2.0 ([#542](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/542)) ([bbfa265](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/bbfa2655a79598df206c63e6409a942e26ee60a1))
* Fixes max height of images ([#549](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/549)) ([8ec9d66](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/8ec9d66b942aac5aba9555ed75f512045a5d01bc)), closes [#547](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/547)
* Improve fit images in tables ([#545](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/545)) ([b76e9e4](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/b76e9e4b5784e2b0ef9156777cae26e2016b8e63))
* SVG images embedded into tables are not adjusted to page width ([#548](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/548)) ([eb19f0e](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/eb19f0e40e22ebfde1aa69ca46507b8b496fa2a0)), closes [#547](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/547)

## [9.4.3](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v9.4.2...v9.4.3) (2025-09-30)


### Bug Fixes

* correct projectId assignment in document file name generation ([#538](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/538)) ([6cf623b](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/6cf623b0cb55c44ecc4687c78d99a87be424e2c9)), closes [#537](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/537)

## [9.4.2](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v9.4.1...v9.4.2) (2025-09-26)


### Bug Fixes

* Autoselect most suitable style package during bulk export ([#533](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/533)) ([8ee6e91](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/8ee6e91607622b3070d3ff9d48df90848b55e10b)), closes [#532](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/532)
* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v11.1.0 ([8b785c7](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/8b785c7bdf0716a63920ec3be67664ff32865ac5))

## [9.4.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v9.4.0...v9.4.1) (2025-09-24)


### Bug Fixes

* corrected logic for generating document file name ([#525](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/525)) ([d8a7bec](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/d8a7bec63620fbcf497df7bac31e040a8e5aa763)), closes [#524](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/524)

## [9.4.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v9.3.1...v9.4.0) (2025-09-16)


### Features

* added ability to set scale factor ([#514](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/514)) ([5a23545](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/5a235454924ffbc6454d1e471519b99994d19283)), closes [#509](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/509)
* Include attachments into PDF ([#507](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/507)) ([b33ea4b](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/b33ea4bf9e711b4eae20cb664f47082c82c51135)), closes [#497](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/497)
* include meta data in generated PDF ([#508](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/508)) ([75a7e3d](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/75a7e3daf07fa1f3f5855e712aa3a357131d7352)), closes [#498](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/498)


### Bug Fixes

* fixed page width validation ([#513](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/513)) ([697ce06](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/697ce06cfa4564e2d8045019e9e47d4df6f55026)), closes [#512](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/512)

## [9.3.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v9.3.0...v9.3.1) (2025-09-04)


### Bug Fixes

* all export jobs acessible for any user via /jobs endpoints ([#505](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/505)) ([6df031e](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/6df031edce9b46e30285f9107f1c498ecd3238b5)), closes [#504](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/504)
* bump weasyprint-service to 66.0.1 to support chinese characters ([#499](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/499)) ([d4cd65b](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/d4cd65b2ffa82634485ec72147eb6432a6f60d95)), closes [#491](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/491)

## [9.3.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v9.2.0...v9.3.0) (2025-08-22)


### Features

* fix NPE during bulk export ([#489](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/489)) ([7cbd681](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/7cbd6817535821839d41dc33e00336365503c83b)), closes [#488](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/488)


### Bug Fixes

* fixed loading of test runs ([#494](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/494)) ([52bf223](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/52bf22301b5fbb25b67302071fc933fe4bc934b7)), closes [#493](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/493)

## [9.2.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v9.1.1...v9.2.0) (2025-08-19)


### Features

* Additional flag for downloading attachments on individual test-… ([#486](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/486)) ([5dc80d4](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/5dc80d4c4414cc2d524d001a80809ef2ac4236cc))
* Additional flag for downloading attachments on individual test-case level ([5dc80d4](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/5dc80d4c4414cc2d524d001a80809ef2ac4236cc)), closes [#483](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/483)
* Updated logic to aggregate modules from both the main and upstr… ([#485](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/485)) ([ae90b8c](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/ae90b8c0c9c91c62d33f06659d13a3bdbbb14d7b)), closes [#477](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/477)


### Bug Fixes

* **deps:** update dependency net.bytebuddy:byte-buddy to v1.17.7 ([8a57bb1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/8a57bb18d5359055db4a167551201f1ffbd71e9b))

## [9.1.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v9.1.0...v9.1.1) (2025-08-10)


### Bug Fixes

* test run attachments always get downloaded even if checkbox is n… ([#479](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/479)) ([2baa38d](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/2baa38dcfa7d817417cb0ec297b688538a540ae0))
* test run attachments always get downloaded even if checkbox is not selected ([2baa38d](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/2baa38dcfa7d817417cb0ec297b688538a540ae0)), closes [#462](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/462)

## [9.1.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v9.0.0...v9.1.0) (2025-07-31)


### Features

* Configurable watermark ([#468](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/468)) ([3a3f348](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/3a3f3487e58f715d9468dd55c5c69866c1f62df5)), closes [#461](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/461)


### Bug Fixes

* add WeasyPrint Docker profile to Maven build commands ([#475](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/475)) ([bc8b838](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/bc8b838afd9cca73525ecaeab2b8c91b22481210))
* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v11.0.2 ([c5fd3b4](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/c5fd3b44e58b51dea902d3eb6745d19a95a08de2))
* missing test run attachments ([#463](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/463)) ([b50da5f](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/b50da5f1c5becc3134a92b6e13747dc4b50cee29)), closes [#462](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/462)

## [9.0.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v8.4.0...v9.0.0) (2025-07-09)


### ⚠ BREAKING CHANGES

* Polarion 2506 support ([#458](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/458))
* remove deprecated enableCommentsRendering param ([#455](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/455))
* PDF Variant configurable on style-package level ([#451](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/451))

### Features

* PDF Variant configurable on style-package level ([#451](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/451)) ([270cc8d](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/270cc8d836f33f6d7ea3023c0f90aaecc2079979)), closes [#450](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/450)


### Bug Fixes

* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v10.1.0 ([33be69d](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/33be69dc0b54eea54734698cdb63fb87718c4780))
* **deps:** update dependency net.bytebuddy:byte-buddy to v1.17.6 ([ca61cb3](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/ca61cb3ebf13d91f25af06a333a1cf1a72d9b774))
* migrate to generic v10 ([#445](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/445)) ([a07cd75](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/a07cd7572da47f7a00277ce65e57c95477f45ec5))


### Miscellaneous Chores

* Polarion 2506 support ([#458](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/458)) ([3c07f39](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/3c07f396591fb421931183f69a9beb87b4100bf3))
* remove deprecated enableCommentsRendering param ([#455](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/455)) ([deb0baa](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/deb0baa667868b04e50c30c8affad17a94dc4fe3))

## [8.4.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v8.3.1...v8.4.0) (2025-06-02)


### Features

* selective test run attachments export ([#426](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/426)) ([ddc5338](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/ddc53384ce452d04c3d72be29905ba2731840a49)), closes [#414](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/414)


### Bug Fixes

* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v8.1.1 ([#420](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/420)) ([b485970](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/b48597080950070791a1766628b4d91f9a13bad3))
* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v9.1.1 ([#441](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/441)) ([c4ba266](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/c4ba266ca946ce2a4730e06dd6e8b4275ccae82e))
* **deps:** update dependency org.apache.pdfbox:pdfbox to v3.0.5 ([96ab21e](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/96ab21e32645ad5310b509b1f0686146240b9163))
* extension registration using bundle activator ([#432](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/432)) ([00ef6bb](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/00ef6bb0863896caea7e864461ca1c3a2bc1ed1b))
* make pageParameters available in velocity context ([#440](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/440)) ([aa86063](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/aa8606348f6193ea65c6967a8287f06caec60853)), closes [#439](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/439)
* make tests stable ([#431](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/431)) ([461407b](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/461407b7df7e32be77c690dff2dc62672edf507e))


### Documentation

* address editorial issues in USER_GUIDE.md ([#427](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/427)) ([546ffd9](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/546ffd92b4439294f1d97a1524f8c8fb00d48079))
* use Headings instead of Headers ([#435](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/435)) ([260bf71](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/260bf71f29a6575e50f8d7d7f14fb244924e8b41))

## [8.3.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v8.3.0...v8.3.1) (2025-04-09)


### Bug Fixes

* wiki content toc support ([#416](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/416)) ([02f014c](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/02f014c3797168c91b51499405e26897c7510a8b)), closes [#415](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/415)

## [8.3.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v8.2.1...v8.3.0) (2025-03-31)


### Features

* weasypring-service v65.0.0 support ([#410](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/410)) ([35c9b4c](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/35c9b4c63208fd95893e7d894cc3849a2ddba13a)), closes [#409](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/409)


### Bug Fixes

* **deps:** update dependency net.bytebuddy:byte-buddy to v1.17.4 ([f3d2ce1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/f3d2ce1a511c44201efaedd4e5d063090c4b88d3))
* **deps:** update dependency net.bytebuddy:byte-buddy to v1.17.5 ([2c29859](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/2c298599efa344a0b3766537697765cb40856b50))

## [8.2.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v8.2.0...v8.2.1) (2025-03-25)


### Bug Fixes

* Fix storing custom CSS ([#404](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/404)) ([6cfd704](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/6cfd70447f5e739a4dd469631f04c0dbcd30268e))

## [8.2.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v8.1.3...v8.2.0) (2025-03-13)


### Features

* ability to filter out resolved comments ([#401](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/401)) ([9ce8a51](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/9ce8a511fe02cb01802033b6dd520b1e2151a27e)), closes [#400](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/400)
* fit html content when converted to pdf using the api ([#392](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/392)) ([5fc8fd7](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/5fc8fd7bd218ebf6b61706e4ea966bd9895ff2a3))
* Splitting setting (some) files into generic and custom one ([#398](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/398)) ([8f36e90](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/8f36e909830b5cb719faecf622f4efeb24afda3c))


### Bug Fixes

* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v8.1.0 ([#394](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/394)) ([1303725](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/13037256fe6cd0afc0720163f2ed7b668f63a1c1))
* Fixed export of LiveDoc containing SVG pictures. ([#396](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/396)) ([1ae6bbb](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/1ae6bbbb65e4ecc86707dbbfb0455ebfac222447)), closes [#395](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/395)

## [8.1.3](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v8.1.2...v8.1.3) (2025-02-20)


### Bug Fixes

* invalid "undefined" added to filename during bulk export of test runs ([#387](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/387)) ([4a6c3b5](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/4a6c3b50ea12a3f22c3337d2c60e4fcb2560812c)), closes [#386](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/386)
* support {{ PAGE_NUMBER }} and {{ PAGES_TOTAL_COUNT }} on cover page ([#383](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/383)) ([eae60e1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/eae60e171919382c56daa82da5fac78ad7ec679f)), closes [#382](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/382)
* Table of Content for LiveReports/TestRuns ([#380](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/380)) ([f1ae421](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/f1ae421e7d4094f55fec9639fb9c453db1291baa)), closes [#378](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/378)

## [8.1.2](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v8.1.1...v8.1.2) (2025-02-12)


### Bug Fixes

* export of testruns ([#372](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/372)) ([0b021e5](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/0b021e5a7ffb02ccc7ba00ce1f847042a305ac16)), closes [#369](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/369)
* show Polarion version in About page ([#374](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/374)) ([30a3840](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/30a3840e9668f9351b8b85bec147e9e6578210ba)), closes [#369](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/369)

## [8.1.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v8.1.0...v8.1.1) (2025-01-30)


### Bug Fixes

* **deps:** update dependency net.bytebuddy:byte-buddy to v1.16.1 ([#347](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/347)) ([2d4617c](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/2d4617c8e3d4d4afefd058b89e6414aa5c8b72f7))
* **deps:** update dependency net.bytebuddy:byte-buddy to v1.17.0 ([#365](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/365)) ([472cc9f](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/472cc9fb27f30ff369d9cfaede8f2b842e45fc19))
* **deps:** update dependency org.apache.pdfbox:pdfbox to v3.0.4 ([#351](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/351)) ([fc3cb02](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/fc3cb0256542e747968ab5a491855e06e478a0cf))
* Embed images as base64 data into cover page ([#359](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/359)) ([e30d6b7](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/e30d6b713eba1d694b134cda7e1511dd3c34beb9)), closes [#356](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/356)
* table of content indentation fix ([#357](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/357)) ([2a8c739](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/2a8c73983480388a4a306c2305da9f370c64ae79)), closes [#354](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/354)

## [8.1.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v8.0.0...v8.1.0) (2025-01-24)


### Features

* Added logic for handling unavailable images ([#316](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/316)) ([b81c438](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/b81c438a9a9746b3654c87b435c0b59fbb85dba8)), closes [#307](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/307)
* advanced images scaling in tables ([#343](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/343)) ([728bf2b](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/728bf2bf3ef3fca60646d9bdcef20e3875f703ab)), closes [#293](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/293)
* evaluate velocity using RichPageScriptRenderer ([#345](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/345)) ([f351c65](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/f351c656897710ea4637952b065ca774da1d93c6)), closes [#344](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/344)
* export of LiveDoc as Workflow Function ([#336](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/336)) ([2d5f47c](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/2d5f47ceb4c08af224ef601bcd71e2d23651f6c8)), closes [#291](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/291)
* user guide page ([#341](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/341)) ([34f5e27](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/34f5e2761282f101aff09ab12099294e5c6a5106))


### Bug Fixes

* improve getters implementation in widget renderer ([#334](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/334)) ([badae65](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/badae65d4b7fd70aeb8587e80816604cbe49e77c))
* Make it possible to alternate content of header/footer for even/odd pages ([#340](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/340)) ([394e831](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/394e8316ebe7029b42523c734819590037fc477b))
* test run attachments downloaded despite the checkbox 'Download a… ([#338](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/338)) ([99890e7](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/99890e76730580a21c1deb53431da9d9f397d0c8)), closes [#337](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/337)
* test run attachments downloaded despite the checkbox 'Download attachments' is unchecked ([99890e7](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/99890e76730580a21c1deb53431da9d9f397d0c8))

## [8.0.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v7.5.0...v8.0.0) (2025-01-08)


### ⚠ BREAKING CHANGES

* style package weights don't work for bulk export widget ([#324](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/324))
* support only Polarion 2410 ([#315](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/315))

### Features

* refactor the object model and cover with unit tests ([#313](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/313)) ([dd955b0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/dd955b0fb0d8577434a98d207921d4ef401762c5))
* support only Polarion 2410 ([#315](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/315)) ([3725e4e](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/3725e4eb6b67adb36a803164dbec2c5091c59f46)), closes [#299](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/299)
* support WeasyPrint Service v63.1.0 ([#319](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/319)) ([a2ebcfa](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/a2ebcfac5e4ef9b325b8a85d5600d6903368f9cf)), closes [#300](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/300)


### Bug Fixes

* **deps:** update dependency net.bytebuddy:byte-buddy to v1.15.11 ([#322](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/322)) ([77309b0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/77309b01d2915b2f963d3fb9487a29e3309429e8))
* style package weights don't work for bulk export widget ([#324](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/324)) ([3e7abb9](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/3e7abb92ef5379dbed30b57f114e19d918a346fc)), closes [#317](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/317)

## [7.5.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v7.4.0...v7.5.0) (2024-12-10)


### Features

* Baseline support ([#310](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/310)) ([da6c9c0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/da6c9c03b86ad85fda6388f0a65fafbc22e599d2)), closes [#305](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/305)
* support baseline view ([#308](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/308)) ([6c7ccf4](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/6c7ccf4efd5e6966e389c437a2fd1e1d02d6e495)), closes [#305](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/305)

## [7.4.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v7.3.1...v7.4.0) (2024-12-05)


### Features

* Added the ability to select a BaselineCollection for bulk export ([#301](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/301)) ([be22a6e](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/be22a6efd462e6d1c52106172a5bbc1983a5fc85)), closes [#285](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/285)
* export pdf attachments in addition ([#297](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/297)) ([d313974](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/d3139745de2d4daa4298e7b526c9cdbd1b22edfd)), closes [#289](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/289)
* Polarion 2410 support ([#277](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/277)) ([5a63874](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/5a638747215a3eccecb7b2983d6bd4f2949460b8)), closes [#276](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/276)
* Polarion 2410 support ([#286](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/286)) ([af5b4bf](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/af5b4bfb2ea220f8395b242da1108b4e00d306b3)), closes [#276](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/276)


### Bug Fixes

* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v7.7.0 ([963f6c6](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/963f6c6c6070315ff6b4aaaa4902f4c6dec29fd3))
* **deps:** update dependency net.bytebuddy:byte-buddy to v1.15.10 ([8c90fff](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/8c90fff9b5d21b9c761c8f969da65ad3e9712dfe))
* **deps:** update dependency net.bytebuddy:byte-buddy to v1.15.7 ([b5f08f9](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/b5f08f9f7955cc1dfbce08f57086635eefdf1d45))
* **deps:** update dependency net.bytebuddy:byte-buddy to v1.15.8 ([630ad15](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/630ad15b08bcaf41de327aa8de90b027484f4b44))
* **deps:** update dependency net.bytebuddy:byte-buddy to v1.15.9 ([b7c2d4f](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/b7c2d4f80c4fe488e16c89b32eb3e70c3b0c8ff1))
* error loading of document export popup dialog when document is opened in collection ([#304](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/304)) ([79a2930](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/79a293004a384bc6437707be9393347243120eb8)), closes [#303](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/303)
* fit images and tables to page for Test Runs ([#295](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/295)) ([9d77b06](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/9d77b06b5118c52872948e9092e42118e21aab76)), closes [#293](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/293)
* Fixed OpenAPI specification ([#282](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/282)) ([0d1233d](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/0d1233d04fe87f02ed43ce4517fc38be61c99857)), closes [#281](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/281)
* SonarCloud badges removed from about page ([#284](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/284)) ([bdbdec0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/bdbdec0bd6895f89058575c308b837ebd9017c13)), closes [#272](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/272)

## [7.3.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v7.3.0...v7.3.1) (2024-10-23)


### Bug Fixes

* Fix JS - null pointer errors and absent parameter with later decomposing ([#273](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/273)) ([61a1d3e](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/61a1d3e0cc5f74703bdcb906f46441e557d224a6)), closes [#271](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/271)

## [7.3.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v7.2.0...v7.3.0) (2024-10-22)


### Features

* "default" and "description" columns added to extension configuration properties ([#265](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/265)) ([1b8468d](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/1b8468d2d0f663ea7cabe033190865b5a2661a5b)), closes [#256](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/256)
* Bulk export ([#241](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/241)) ([6f6e8ba](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/6f6e8ba659819934377da1a0c5d8de573a928e90))
* detect whether upgrade of WeasyPrint Service is required and display it in About page ([#263](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/263)) ([429a2db](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/429a2db3eb47d06eb01e355b4d1f8d15e16734da)), closes [#258](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/258)
* display non-valid/obsolete configuration properties ([#264](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/264)) ([1dbf304](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/1dbf304c3cf26f4cb4ca78954ce2c4384c4ded30)), closes [#257](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/257)
* style packages weights page ([#246](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/246)) ([089d066](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/089d06682e8c9bf8422f6ae7364f8d8b50360e29))


### Bug Fixes

* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v7.3.1 ([#239](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/239)) ([a6f15ed](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/a6f15edc05512193c2423a3c648fcb0ae5cb4604))
* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v7.4.1 ([e7aeaa8](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/e7aeaa83ec58d96e4b66ff3273024ec5a950ce0d))
* **deps:** update dependency net.bytebuddy:byte-buddy to v1.15.4 ([737038f](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/737038f2e2dab2ac42ab1f07c413c9d09307012c))
* **deps:** update dependency net.bytebuddy:byte-buddy to v1.15.5 ([e06ffe4](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/e06ffe482fa6895cc584c103764b06656a21aeba))
* **deps:** update dependency net.sf.okapi.lib:okapi-lib-xliff2 to v1.47.0 ([34fe527](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/34fe5270ca61c78c75c72f218af07f58a3e643d9))
* fields data ignored on new style package creation ([#251](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/251)) ([3bca629](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/3bca62918902a182a62194a644d3cafe7b521671)), closes [#247](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/247)
* MANIFEST.MF contains org.apache.tika.patched for 2404 and org.apache.tika for 2310 ([#262](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/262)) ([283f562](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/283f562b149fb83273cf4fc7d697f23f6d55a987)), closes [#261](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/261)
* Prevent setting matching query for default style packages on server side level ([#242](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/242)) ([db1dce6](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/db1dce6048e5e051c813ae2cda80afc7740066fb))
* REST endpoints for getting/updating weights aren't available ([#255](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/255)) ([5037517](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/5037517717bd006bff72d75f9a001c7441f80f57)), closes [#254](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/254)
* SonarQube code smells ([#268](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/268)) ([fe0fbe3](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/fe0fbe33f081b9ddf6e9943d2e92ca336bf0f593))
* use "pdf/a-2b" as default value for "weasyprint.pdf.variant" config property ([#253](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/253)) ([1923004](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/1923004924563cd5d1ee22c53a095cb59506becb)), closes [#249](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/249)


### Documentation

* Add missing step for extension installation when restarting Polarion and clearify sections element a bit. ([#233](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/233)) ([55f2359](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/55f2359b3576f1e532e171864f67670bc0500915))
* README.md updated with info about removed/obsolete configuration properties ([#260](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/260)) ([51fe1f2](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/51fe1f23ac5529b882b35ca4b3c8030ae2a0a5f9)), closes [#259](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/259)

## [7.2.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v7.1.2...v7.2.0) (2024-10-02)


### Features

* style packages weights ([#234](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/234)) ([0cc515b](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/0cc515b5345bb37ed570c9b64d5271b001f7c4ef))


### Bug Fixes

* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v7.2.0 ([07674e1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/07674e1d30772d607ff7fe3a7d1feab09a16353e))
* **deps:** update dependency net.bytebuddy:byte-buddy to v1.15.3 ([c10e378](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/c10e37885c04a209e48f5da68c52d6277e8df1c7))
* set configuration properties prefix ([#236](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/236)) ([b037f03](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/b037f0312a3aad584254d9b2c1f2f3f736d7da0d)), closes [#235](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/235)

## [7.1.2](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v7.1.1...v7.1.2) (2024-09-25)


### Bug Fixes

* **deps:** update dependency net.bytebuddy:byte-buddy to v1.15.2 ([fcbaaac](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/fcbaaac9006d3a90f8f9f6a12a15c8b22fde1308))
* do not remove GenericUrlResolver from PolarionUrlResolver singleton ([#229](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/229)) ([a409e33](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/a409e33b1a1309f6c1805faefa4c7ebec1553426)), closes [#228](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/228)

## [7.1.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v7.1.0...v7.1.1) (2024-09-24)


### Bug Fixes

* Fix loading JS module ([#225](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/225)) ([c09cac1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/c09cac1f9cd09148b937e46180930213d8ab8a70)), closes [#222](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/222)
* fix renamed properties in schema ([#224](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/224)) ([e4ef40d](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/e4ef40defba8414ed33b58ffac0caeb37195e9bd)), closes [#222](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/222)

## [7.1.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v7.0.1...v7.1.0) (2024-09-24)


### Features

* display weasyprint server build timestamp and chromium version in extension info table ([#219](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/219)) ([fe7c7ea](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/fe7c7ea41735f32ae9c08cbc028e08388e517034)), closes [#218](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/218)
* unused configuration property removal ([#221](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/221)) ([d7508fa](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/d7508fa131ba9c513280f354f81a9894a609e853))
* urls in css now replaced by the base64 content ([#215](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/215)) ([8bc75d6](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/8bc75d6133c483b3d7288edb77d13fb7c44c0124))

## [7.0.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v7.0.0...v7.0.1) (2024-09-20)


### Bug Fixes

* trim export file name ([#213](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/213)) ([930bb97](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/930bb976485d87369290b9313ca8ba6e57088d50))

## [7.0.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v6.2.1...v7.0.0) (2024-09-17)


### ⚠ BREAKING CHANGES

* refactoring ([#200](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/200))
* refactoring ([#199](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/199))
* add support of test runs ([#196](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/196))

### Features

* add support of test runs ([#196](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/196)) ([b92cdae](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/b92cdae91108c138742a33b9f2ff8f2d54c49167)), closes [#192](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/192)
* javascript refactoring with ability to run JS tests in build process ([#203](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/203)) ([a84ec78](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/a84ec78cd778add63d2a9ab58a40c17072b78a60))


### Bug Fixes

* export of TestRun not working from "list view" ([#208](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/208)) ([fea02d6](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/fea02d6c1f1adf0c45a8ea71bc83f56e5cceac72))
* fix CSS for testruns ([#207](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/207)) ([724aa1a](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/724aa1a441ba915120a0076320f7f8ef02937ee6)), closes [#192](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/192)
* Fix css for testruns ([#210](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/210)) ([fada1ad](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/fada1ad815bdd909cfb8c43f0fd95fb87e456679)), closes [#192](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/192)
* Fix style of button in toolbar ([#211](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/211)) ([38e17ef](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/38e17efaecc120e4f57293d3faf4e927f5319b22))
* internalized relative links fix ([#202](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/202)) ([3b36210](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/3b3621030e345ce557e3a77d61afa3e9d4a5c5ec))
* JavaScript refactoring and fixes for test runs support ([#204](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/204)) ([cc5b954](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/cc5b9541c2bba83199d27046ff47bc47f8ed2606))
* package-lock.json under VCS, JSON sorter only for OpenAPI Spec ([#206](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/206)) ([d84e1b2](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/d84e1b22ffb11696b17469678a30d2d079f3d1fd))
* skipping JavaScript tests if -DskipTests is used ([#205](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/205)) ([357a872](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/357a87253e413e62b66f38afdaee807512a8d519))


### Miscellaneous Chores

* refactoring ([#199](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/199)) ([101af70](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/101af709292fd4c797dc9a7f15f17b7e2548586d))
* refactoring ([#200](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/200)) ([ec21fcd](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/ec21fcd8207bdc8af2c918b413da4ced272d1a70))

## [6.2.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v6.2.0...v6.2.1) (2024-09-10)


### Bug Fixes

* Add quick start ([#188](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/188)) ([7a26b34](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/7a26b34672aecd68cd5f6d131e2859043ba566ca)), closes [#183](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/183)
* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v7.1.0 ([34cfcf7](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/34cfcf78141898cfdf688064fb30c1805c401126))
* style packages can be saved without webhooks ([#194](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/194)) ([c9941a8](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/c9941a808f091beabc3dc95dedc22fac224d23c3)), closes [#193](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/193)

## [6.2.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v6.1.0...v6.2.0) (2024-09-03)


### Features

* authorization for webhooks ([#177](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/177)) ([4da2ee2](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/4da2ee277f080aff9012192f82a8e70ee751ad28)), closes [#169](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/169)
* Auto assign style packages ([#178](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/178)) ([77a76d4](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/77a76d413b1c176e1d820c80612adfc05648de41)), closes [#64](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/64)
* Cleanup successfully finished jobs by timeout like failed jobs ([#176](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/176)) ([a26d5d6](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/a26d5d6d821406fdb391e1274c62922d2d3e4277)), closes [#175](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/175)
* Extended REST API annotation ([#182](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/182)) ([9a89ff6](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/9a89ff6e9c5403045bf3926696965abd39272364)), closes [#180](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/180)


### Bug Fixes

* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v6.6.3 ([484aeea](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/484aeead23a202ceb53381e89868ea79642ad785))
* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v6.7.0 ([8bd8e2a](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/8bd8e2abaaac49d0b1c4ea0ac7a00310571923b0))
* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v7.0.1 ([928d126](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/928d126abe5c048d93e99c8264fef9275ac09d1c))
* **deps:** update dependency net.bytebuddy:byte-buddy to v1.14.19 ([#163](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/163)) ([457dd05](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/457dd058af1afdf73f3680ab53843688017a8435))
* **deps:** update dependency net.bytebuddy:byte-buddy to v1.15.0 ([55f995a](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/55f995a095a6279e9e703db866884beb152356a5))
* **deps:** update dependency net.bytebuddy:byte-buddy to v1.15.1 ([d759830](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/d759830e33c87bc032ce4ea7561588e474ed841f))
* do not logout the user if XSRF token is used in async operation ([#181](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/181)) ([97ce5f4](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/97ce5f4a806d69cd55380e7c7e37d1c57674e2ad)), closes [#170](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/170)
* Feature/fix cover page ([#166](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/166)) ([5fb657c](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/5fb657c3d17e126d863de33d9f41af5aba623196))
* Fixed getting project baseline ([#161](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/161)) ([d3163c5](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/d3163c5c6dc7e00e89f3e8a0987eb4d736b877c0))
* link on About page work as expected ([#185](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/185)) ([225b964](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/225b96483458d1ee21bf6f4f21743566744bd278)), closes [#171](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/171)
* Possibility for global admin to hide webhook functionality at all via polarion.properties ([#168](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/168)) ([437b1a5](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/437b1a51791be697eff150294ad84cdb44a27adb)), closes [#91](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/91)

## [6.1.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v6.0.1...v6.1.0) (2024-08-13)


### Features

* Webhooks framework added ([#155](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/155)) ([c151b10](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/c151b106b3dbcc776934abd0d5523f4cbf9b69d3))


### Bug Fixes

* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v6.6.2 ([#154](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/154)) ([a04dc58](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/a04dc58d1bdb0f69be7452c63386af92793bdffd))
* **deps:** update dependency org.apache.pdfbox:pdfbox to v3.0.3 ([#152](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/152)) ([07162f8](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/07162f88848ebeb819cfc298b5658fdf92e2a225))


### Documentation

* Webhooks documentation extended ([#156](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/156)) ([28f4299](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/28f4299a88b965dcf9b68c986937a3100fd6c7fa)), closes [#91](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/91)

## [6.0.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v6.0.0...v6.0.1) (2024-08-09)


### Bug Fixes

* fix CSS ([#148](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/148)) ([87e8e12](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/87e8e12bdc3fe5113064c8a1befe39eaba0dc207)), closes [#145](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/145)
* LiveReport Page now respects URL Parameters ([#150](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/150)) ([31fd642](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/31fd642549d18ff8170e33e2398719f9147ffb17)), closes [#147](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/147)

## [6.0.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v5.7.2...v6.0.0) (2024-08-08)


### ⚠ BREAKING CHANGES

* remove WeasyPrint CLI support  ([#134](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/134))

### Features

* remove WeasyPrint CLI support  ([#134](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/134)) ([c94ade3](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/c94ade319408285a9042e033086dfb7723382834)), closes [#129](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/129)


### Bug Fixes

* Export to PDF button on LiveReport page works as expected again ([#143](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/143)) ([d162c80](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/d162c80bfefe2083ec1d5294a06a66d447c860e4)), closes [#142](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/142)

## [5.7.2](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v5.7.1...v5.7.2) (2024-08-07)


### Bug Fixes

* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v6.6.1 ([#136](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/136)) ([e1f3ecb](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/e1f3ecb7aaa059b994c42b252ebf6b70a8720f07))
* update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v6.6.1 ([#135](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/135)) ([4a65890](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/4a65890d2a61db1312295e0be5be34d660a3c649))

## [5.7.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v5.7.0...v5.7.1) (2024-08-06)


### Bug Fixes

* CSS fixes for tables which split over multiple pages ([#131](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/131)) ([d77444e](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/d77444ec0c99d7478a18ea90eb3a7e3889df5f09)), closes [#130](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/130)
* filtering linked WI for Polarion 2404 ([#126](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/126)) ([d3aa6f1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/d3aa6f14567dc92f4472ec717df222e48483a5d2)), closes [#125](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/125)
* link converting table columns width to 'auto' with 'Fit to page' property ([#123](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/123)) ([b728ec9](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/b728ec94afd2d071e09134efb87bd864354bc0d3)), closes [#122](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/122)

## [5.7.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v5.6.0...v5.7.0) (2024-08-01)


### Features

* info about the used weasyprint docker-image version ([#115](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/115)) ([0669e8b](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/0669e8be5d702cab3e277444ff0fc8213dfbca67)), closes [#104](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/104)
* REST API for getting prepared HTML content for WeasyPrint ([#119](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/119)) ([41aea17](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/41aea179b5e2e145ff9cd62c582dfa4b63375411)), closes [#116](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/116)


### Bug Fixes

* update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v6.5.2 ([#120](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/120)) ([bf9003e](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/bf9003e2150228034739312b711d05e0090a7bf7))

## [5.6.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v5.5.4...v5.6.0) (2024-07-30)


### Features

* embed all the images in header/footer as base64 data ([#113](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/113)) ([1eb930c](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/1eb930c2c75f8a5feba33504055f40ca0ba0460e)), closes [#112](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/112)


### Bug Fixes

* plugin documentation link doesn't work ([#110](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/110)) ([685d548](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/685d5488c302aa5bd0c37ae2848e26d1d89adc3c)), closes [#97](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/97)

## [5.5.4](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v5.5.3...v5.5.4) (2024-07-25)


### Bug Fixes

* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v6.4.0 ([bdcd4dc](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/bdcd4dcc26189fc27093b0c15b87cbdea61efb9a))

## [5.5.3](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v5.5.2...v5.5.3) (2024-07-19)


### Bug Fixes

* configuration status is available again ([#98](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/98)) ([026a354](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/026a354821549979e07d7c230e609af47be6594d))

## [5.5.2](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v5.5.1...v5.5.2) (2024-07-16)


### Bug Fixes

* migrate to generic v6.3.0 to use the markdown2html-maven-plugin v1.1.0 ([#92](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/92)) ([ba80029](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/ba80029b5f51c265762151a0bed559a90caacfc5)), closes [#76](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/76)

## [5.5.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v5.5.0...v5.5.1) (2024-07-11)


### Bug Fixes

* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v6.2.0 ([c40db4b](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/c40db4bdeca3558b0187e81744241ee4a8a0f277))
* Fixed export of a live document from a collection ([#87](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/87)) ([f5b0f0d](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/f5b0f0d39e95f236d9cbce980408b8a946d4607a)), closes [#77](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/77)

## [5.5.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v5.4.1...v5.5.0) (2024-07-09)


### Features

* portal language for LiveDoc is supported for PDF export ([#69](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/69)) ([d3fd678](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/d3fd6789448749902247a02e671c553bcb1448f7)), closes [#58](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/58)


### Bug Fixes

* **deps:** update dependency net.bytebuddy:byte-buddy to v1.14.18 ([80a5272](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/80a5272a5b09fd3758e8daf7ce358b225c4bd8be))
* HTTP Status 500 in About page by migration to generic v6.1.0 ([#73](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/73)) ([45cf6fe](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/45cf6fef038f7065051a2b3a40232676c3d45688)), closes [#71](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/71)
* misaligned TOC numbers fix ([#72](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/72)) ([1271359](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/1271359a84de9c605ca78556538332a011d24839))

## [5.4.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v5.4.0...v5.4.1) (2024-07-08)


### Bug Fixes

* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v6.0.3 ([babf995](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/babf995141bb5819a3b5b1387f4fbc1f892b6e1a))
* Fixed check nested lists. ([#66](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/66)) ([757b86a](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/757b86a585b07d70dffae7d4e6673c1c5da45379)), closes [#65](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/65)

## [5.4.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v5.3.0...v5.4.0) (2024-07-04)


### Features

* migrate to generic v6.0.1 ([#55](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/55)) ([266f527](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/266f52703dbc5aefe7e337ea5a832402eedfefc6))


### Bug Fixes

* check if required commands are available ([#49](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/49)) ([b7d702a](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/b7d702afa526b36c2cfc6f00ac97d02c765b40e0))
* **deps:** update dependency ch.sbb.polarion.extensions:ch.sbb.polarion.extension.generic to v6.0.2 ([797ae72](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/797ae7263a9424a74f069ff0cf6dc28a9f993e8b))
* Fixed exception handling. ([#60](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/60)) ([5192e8a](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/5192e8a94fac2f3eec1ca19c246eeee49fedef7b))
* Fixed unstable JobsService test ([#52](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/52)) ([3fb7fdc](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/3fb7fdc391aea8aebc3571e22dca23fe6e30b86f)), closes [#51](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/51)
* missing styles for some reports components ([#54](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/54)) ([9b54da4](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/9b54da4bee44d9befe93514a28fda2a1b53e589b)), closes [#35](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/35)

## [5.3.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v5.2.0...v5.3.0) (2024-06-27)


### Features

* content of about page now generated based on information from README.md ([#36](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/36)) ([46628ef](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/46628ef1372a6dd20a73016edf63924f7ed1605e))


### Bug Fixes

* repeated export leads to 'SyntaxError: Identifier as already been declared' ([#41](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/41)) ([2dd641d](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/2dd641dd21e0251031e1e8a7c502ac190324e31b)), closes [#34](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/34)
* Replace dollar character by appropriate HTML entity ([#46](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/46)) ([28a8a89](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/28a8a89e46879241f24c66b440504f99ac5290f0)), closes [#42](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/42)

## [5.2.0](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v5.1.2...v5.2.0) (2024-06-18)


### Features

* Added links internalization ([#10](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/10)) ([8b75f41](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/8b75f41168a11c8f7f50d6660f83fc8dbf0e8f68))

## [5.1.2](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v5.1.1...v5.1.2) (2024-06-11)


### Bug Fixes

* deployment to packages only from main branch possible [#19](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/19) ([#20](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/20)) ([4a8fcb6](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/4a8fcb65c9264ca56c31800a68e5600334b82628))
* remove some specific parts ([#15](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/15)) ([e6aa41a](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/e6aa41a4555934fa909542f39d435d7f4643b229))


### Documentation

* add link to weasyprint-service in readme ([#21](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/21)) ([a4c8e27](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/a4c8e274cfd15fac71d8b012211cb20a780d375a))

## [5.1.1](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/compare/v5.1.0...v5.1.1) (2024-06-05)


### Documentation

* changelog history moved from README.md to CHANGELOG.md ([#9](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/9)) ([84c4234](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/84c4234245bc3c08ea4d00bf1db3ef352ecf0308))

## 5.1.0 (2024-06-05)


### Features

* migration to generic v4.10.0 ([#5](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/5)) ([f124394](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/f124394da566511324b197340f150094f763c745))


### Miscellaneous Chores

* release 5.1.0 ([#7](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/issues/7)) ([0b0260a](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.extension.pdf-exporter/commit/0b0260a0dc1014073e81067e12eaf57c7e827bca))


# Changelog before migration to conventional commits

| Version | Changes                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
|---------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| v5.0.2  | Added CSS configuration to hide non-printing elements                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| v5.0.1  | Added defaults for size and page parameters: A4 and Portrait                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| v5.0.0  | * Attribute "Fit images and tables to page width" is modified to be "Fit images and tables to page" and at the same time logic is modified to try and fit images not only to page width but also to page height (it's a breaking change, see "Upgrading instructions" section for further instructions).<br>* Exclude report parameters area from being exporting to pdf for live reports                                                                                                                                                                                                                                                                                                                                            |
| v4.5.2  | Help colorized                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| v4.5.1  | Help updated                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| v4.5.0  | PDF/A variants now supported both for weasyprint CLI and weasyprint service                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| v4.4.0  | Polarion 2404 is supported                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| v4.3.1  | Fixed bugs in about page                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| v4.3.0  | * Support of WeasyPrint as a Service has been added<br/>* Fixed export to PDF from default space                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| v4.2.0  | * Added BASELINE_NAME and REVISION_AND_BASELINE_NAME placeholders<br/>* StringIndexOutOfBoundsException on LiveDoc rendering fix<br/>* Hide hint for unsupported SVG 1.1 features                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| v4.1.1  | * HtmlUtils class moved to generic application.<br/>* Styling fix.<br/>* Fixed base URL prefix.<br/>* Debug information for raw HTMl export                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| v4.1.0  | * Current configuration status to About page added.<br/>* Expanded fields for document filename template<br/>* Icon for LiveReport button changed                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| v4.0.0  | * Using enums for DocumentType<br/>* Added endpoint for raw html to pdf export                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| v3.12.0 | User-friendly message about deactivated configuration properties on the Swagger UI page.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| v3.11.1 | Temporary fix for starting of clearing job                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| v3.11.0 | About page supports configuration help and icon                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| v3.10.1 | delivery-external maven profile does not include EuroSpec cover page                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| v3.10.0 | * Fixed wiki page rendering<br/>* Updated GUI to use asynchronous PDF export API<br/>* Added cleanup async jobs                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| v3.9.0  | Default font for PDF export is Arial<br/> Wiki pages now rendered using internal renderer directly                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| v3.8.1  | Fix for saving of default settings in read only transaction                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| v3.8.0  | * Added support Velocity variables and placeholders for document filename<br/>* Enabled PDF generation for live reports on global level<br/>* Added Asynchronous PDF generation API                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| v3.7.0  | * Fix for export of Wiki Page<br/>* removed wikiFolder and wikiName from ExportParams                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| v3.6.0  | Evaluation of Velocity expression is extended with $page variable for LiveReports                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| v3.5.0  | Paper size is added as style package option.<br/>* Fixed the logic for searching executor in windows with WSL                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| v3.4.0  | Support exporting Live Reports to PDF                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| v3.3.0  | * Evaluate Velocity expressions in Cover Page and Header/Footer templates<br/>* DOCUMENT_ID placeholder has been added                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| v3.2.3  | Fix images which width is bigger than the page width but their width is not explicitly specified in any of attributes and factual value is used                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| v3.2.1  | Page break styling fix                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| v3.2.0  | * Added support of HTML Presentational Hints.<br/>* Added possibility to re-define resulted file name.<br/>* Marking referenced WorkItems extracted as a configurable property                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| v3.1.1  | Configuration properties in about page                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| v3.1.0  | Added possibility to open PDF exporter view in popup (via toolbar button) implemented with help of JS-injection approach provided by Polarion                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| v3.0.0  | * Cover page endpoint renamed to correspond menu item name.<br/>* Fix named configurations issue when extension is installed from the scratch                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| v2.4.0  | New feature: on fly converting of cover page images located in SVN and referenced in CSS into base64 representation and embedding them into CSS directly                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| v2.3.0  | * Multiple predefined templates for cover page.<br/>* Extended description in quick help sections of cover page and header/footer configurations to clarify format of special variables and document's custom fields.<br/>* If no value was obtained/calculated for special variable or document's custom fields then blank string will be inserted instead of special variable or custom field ID.<br/>* All '&amp;nbsp;' occurrences are now removed from source HTML before converting into PDF. This is done to exclude situations when they are a reason of certain strings be too wide going outside of page boundaries. As a side effect in some cases WorkItems ID and title can be rendered on different lines of document. |
| v2.2.0  | * Cover page generation added together with cover page configuration.<br/>* Custom styling of referenced Work Items.<br/>* Cut local URLs + filtering Workitem roles working together fix.<br/>* Added variables injection into CSS of document and cover page.<br/>* Named settings moved to generic application                                                                                                                                                                                                                                                                                                                                                                                                                    |
| v2.1.1  | * Custom dropdowns are used to visually mark project's scope configurations inherited from global scope.<br/>* Adjust REST endpoints and use Polarion services via PolarionService aggregator/proxy.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| v2.1.0  | * New feature: added DOCUMENT_STATUS variable to header/footer settings.<br/>* "Fit to page width" checkbox is renamed to "Fit images and tables to page width" to reduce confusion with real implementation.<br/>* Fixed an issue when exporter form is initialized in case when no style packages ever configured on admin site.<br/>* Added tests running real WeasyPrint instance to test if document elements didn't go out of page boundaries.                                                                                                                                                                                                                                                                                 |
| v2.0.0  | Added named settings and ability to combine them into style packages. Breaking change modifying the way settings are stored!                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| v1.2.14 | Fix too wide lines generated in PDF caused by unnecessary &nbsp; characters in source HTML                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| v1.2.13 | Document's last revision is displayed instead of "HEAD" literal in case if no revision was specified                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| v1.2.12 | Added processing IDs of document's custom fields as placeholders in header/footer. Added an option to run WeasyPrint from docker image.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| v1.2.11 | Displaying Polarion's HEAD revision logic fixed                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| v1.2.10 | Getting document's title logic fixed                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| v1.2.9  | Fix for Table of Figures - arbitrary caption's label                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| v1.2.8  | HTML logging added                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| v1.2.7  | Items of ToF & ToT as hyperlinks, fix for comments inside captions to figures and tables                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| v1.2.6  | Fix for "page-break-inside: avoid"                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| v1.2.5  | Table of content issues fixed                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| v1.2.4  | Non-default icons rendering fix                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| v1.2.3  | Table of figures/tables fix                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| v1.2.2  | Table of figures/tables generation added                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| v1.2.1  | Numbered lists processing fix                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| v1.2.0  | Fixing API to call from python                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| v1.1.0  | Different refactorings                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| v1.0.0  | Initial release                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
