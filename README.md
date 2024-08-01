[![scalafix-migrate-circe-generic-extras Scala version support](https://index.scala-lang.org/virtuslabrnd/scalafix-migrate-circe-generic-extras/scalafix-migrate-circe-generic-extras/latest-by-scala-version.svg?platform=jvm)](https://index.scala-lang.org/virtuslabrnd/scalafix-migrate-circe-generic-extras/scalafix-migrate-circe-generic-extras)

# Scalafix rules for circe-generic-extra migration

This project contains set of rules allowing to migrate Scala 2 macro-annotation based codecs into Scala 3 built-in derivation. 

## Available rules: 

### `CirceGenericExtrasMigration`
Main rule, detects usages of macro-annotations or derivation from `circe-generic-extras` and replaces them with the counterparts defined in `circe` core Scala 3 library.

**Compatible with Circe 0.14.7 or later.**

Can be used to migrate usages of both `circe-generic-extras` and `circe-derivation` (partially) to Scala 3.

Supported rewrites:

- [x] - `JsonCodec` macro-annotations - replaced with `Codec` derivation;
- [x] - `ConfiguredJsonCodec` macro-annotations - replaced with `ConfiguredCodec` derivation;
- [x] - `ConfiguredJsonCodec(encodeOnly = true)` / `ConfiguredJsonCodec(decodeOnly = true)` - rewritten into  `ConfiguredEncoder` or `ConfiguredDecoder` derived instances;
- [x] - `@JsonKey`  annotations - field names defined in primary constructor or as member fields are transformed into dedicated `Configuration` instance;
- [ ] - `generic.extras.semiauto`:
  - [x] - `deriveCodec[T]`, `deriveDecoder[T]`, `deriveEncoder[T]`, - replaces calls to methods defined in `io.circe.generic.extras.semiauto._` with their counterpart in  `io.circe.generic.semiauto._`, does not requrie implicit `Configuration`. Requires `io.circe::circe-generic` dependency
  - [x] - `deriveConfiguredCodec[T]`, `deriveConfiguredDecoder[T]`, `deriveConfiguredEncoder[T]` - rewritten into `Codec.derived` / `Decoder.derivedConfigured` / `Encoder.derived`
  - [x] - `deriveUnwrappedCodec[T]`, `deriveUnwrappedDecoder[T]`, `deriveUnwrappedEncoder[T]` - rewritten into `Codec`/`Decoder`/`Encoder` constructed from value class underlying type and `map`/`contramap` operations.
  - [ ] - `deriveEnumerationCodec[T]`, `deriveEnumerationDecoder[T]`, `deriveEnumerationEncoder[T]` - not yet supported
  - [ ] - `deriveFor` - no replacement in Scala 3
  - [ ] - `deriveExtrasCodec[T]`, `deriveExtrasDecoder[T]`, `deriveExtrasEncoder[T]`: no replecement in Scala 3
  


### `CirceLiteralMigration`
Simple rule allowing to adopt to `json` string interpolator changes done in Scala 3. Would rewrite imports `io.circe.literal.JsonStringContext` into `io.circe.literal.json`.

## Usage
For information on how to use this projects refer to [Scalafix user guide](https://scalacenter.github.io/scalafix/docs/users/installation.html)
