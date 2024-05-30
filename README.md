# Polarion ALM extension to <...>

This Polarion extension provides possibility to <...>
## Build

This extension can be produced using maven:
```
mvn clean package
```

## Installation to Polarion

To install the extension to Polarion `ch.sbb.polarion.extension.<extension_name>-<version>.jar`
should be copied to `<polarion_home>/polarion/extensions/ch.sbb.polarion.extension.<extension_name>/eclipse/plugins`
It can be done manually or automated using maven build:
```
mvn clean install -P polarion2304,install-to-local-polarion
```
For automated installation with maven env variable `POLARION_HOME` should be defined and point to folder where Polarion is installed.

Changes only take effect after restart of Polarion.

## Polarion configuration

<...>


## Extension Configuration

<...>


## Usage

<...>

