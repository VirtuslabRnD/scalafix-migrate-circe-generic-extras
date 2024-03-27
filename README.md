# Scalafix rules for circe-generic-extra migration

This project contains set of rules allowing to migrate Scala 2 macro-annotation based codecs into Scala 3 built-in derivation. 

## Available rules: 

### `CirceGenericExtrasMigration`
Main rule, detects usages of `JsonCodec`, `ConfiguredJsonCodec` macro-annotations and replaces them with derived `Codec.AsObject` or `ConfiguredCodec` instances. 
Supports `@JsonKey` annotation used to renaem fields defined either in primary constructor or as member fields (renames or member fields are working using best effort principles).
Supoprts `ConfiguredJsonCodec(encodeOnly = true)` and `ConfiguredJsonCodec(decodeOnly = true)` variants, these would be rewritten into `ConfiguredEncoder` or `ConfiguredDecoder` derived instances.
Can be used to migrate usages of both `circe-generic-extras` and `circe-derivation` to Scala 3.

### `CirceLiteralMigration`
Simple rule allowing to adopt to `json` string interpolator changes done in Scala 3. Would rewrite imports `io.circe.literal.JsonStringContext` into `io.circe.literal.json`.

## Usage
For information on how to use this projects refer to [Scalafix user guide](https://scalacenter.github.io/scalafix/docs/users/installation.html)