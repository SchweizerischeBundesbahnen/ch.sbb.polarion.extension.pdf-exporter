# Changelog

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
