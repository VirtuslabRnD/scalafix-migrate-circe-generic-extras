package org.virtuslab.migration.scala3

import scalafix.v1._
import scala.meta._
import scala.collection.mutable

final class CirceGenericExtrasMigration extends SemanticRule("CirceGenericExtrasMigration") {
  implicit val dialect: Dialect = dialects.Scala3

  override def isRewrite: Boolean = true
  override def description: String =
    s"Rewrite rule for migration from macro-annotation based `circe-generic-extras` / `circe-derivation`(best-effort) libraries to dedicated Scala 3 derivation of codecs."

  def symbolOf(t: Tree with Stat.WithMods)(implicit doc: SemanticDocument) =
    Option(t.symbol)
      .filterNot(_.isNone)
      .orElse {
        //  Bug workaround:
        // If type has annotation then type symbol is stored in annotation symbol
        t.mods.filter(_.is[Mod.Annot]).map(_.symbol).headOption
      }

  case class Configuration(
      renamedInCtor: Map[String, String],
      renamedInStats: Map[String, String],
      noDefaults: Boolean,
      explicitConfig: Option[Term]
  ) {
    def isDefined = renamedInCtor.nonEmpty || renamedInStats.nonEmpty || noDefaults || explicitConfig.isDefined
  }
  abstract case class CodecKind(symbol: Symbol, tpe: Type, contextBounds: List[Type])
  object CodecKind {
    object Codec extends CodecKind(symbols.Codec, types.Codec, List(types.Encoder, types.Decoder))
    object ConfiguredCodec
        extends CodecKind(symbols.ConfiguredCodec, types.ConfiguredCodec, List(types.Encoder, types.Decoder))
    object ConfiguredEncoder extends CodecKind(symbols.ConfiguredEncoder, types.ConfiguredEncoder, List(types.Encoder))
    object ConfiguredDecoder extends CodecKind(symbols.ConfiguredDecoder, types.ConfiguredDecoder, List(types.Decoder))
  }

  object symbols {
    val Codec = Symbol("io/circe/Codec#")
    val Encoder = Symbol("io/circe/Encoder#")
    val Decoder = Symbol("io/circe/Decoder#")

    val ConfiguredCodec = Symbol("io/circe/derivation/ConfiguredCodec#")
    val ConfiguredEncoder = Symbol("io/circe/derivation/ConfiguredEncoder#")
    val ConfiguredDecoder = Symbol("io/circe/derivation/ConfiguredDecoder#")
    val Configuration = Symbol("io/circe/derivation/Configuration#")
    val DeriveCodec = Symbol("io/circe/generic/semiauto.deriveCodec().")
    val DeriveEncoder = Symbol("io/circe/generic/semiauto.deriveEncoder().")
    val DeriveDecoder = Symbol("io/circe/generic/semiauto.deriveDecoder().")
    val GenericSemiautoPackage = Symbol("io/circe/generic/extras/semiauto.")

    val GenericExtrasPackage = Symbol("io/circe/generic/extras/")
    val GenericExtrasConfiguration = Symbol("io/circe/generic/extras/Configuration#")
    val GenericExtrasConfiguredCodec = Symbol("io/circe/generic/extras/Configuration#")
    val GenericExtrasConfiguredEncoder = Symbol("io/circe/generic/extras/ConfiguredEncoder#")
    val GenericExtrasConfiguredDecoder = Symbol("io/circe/generic/extras/ConfiguredDecoder#")
    val GenericExtrasConfiguredJsonCodec = Symbol("io/circe/generic/extras/ConfiguredJsonCodec#")
    val GenericExtrasJsonCodec = Symbol("io/circe/generic/extras/JsonCodec#")
    val GenericExtrasJsonKey = Symbol("io/circe/generic/extras/JsonKey#")
    val GenericExtrasJsonNoDefault = Symbol("io/circe/generic/extras/JsonNoDefault#")
    val GenericExtrasSemiautoPackage = Symbol("io/circe/generic/extras/semiauto.")
    val GenericExtrasDeriveCodec = Symbol("io/circe/generic/extras/semiauto.deriveCodec().")
    val GenericExtrasDeriveEncoder = Symbol("io/circe/generic/extras/semiauto.deriveEncoder().")
    val GenericExtrasDeriveDecoder = Symbol("io/circe/generic/extras/semiauto.deriveDecoder().")
    val GenericExtrasDeriveConfiguredCodec = Symbol("io/circe/generic/extras/semiauto.deriveConfiguredCodec().")
    val GenericExtrasDeriveConfiguredEncoder = Symbol("io/circe/generic/extras/semiauto.deriveConfiguredEncoder().")
    val GenericExtrasDeriveConfiguredDecoder = Symbol("io/circe/generic/extras/semiauto.deriveConfiguredDecoder().")
    val GenericExtrasDeriveUnwrappedCodec = Symbol("io/circe/generic/extras/semiauto.deriveUnwrappedCodec().")
    val GenericExtrasDeriveUnwrappedEncoder = Symbol("io/circe/generic/extras/semiauto.deriveUnwrappedEncoder().")
    val GenericExtrasDeriveUnwrappedDecoder = Symbol("io/circe/generic/extras/semiauto.deriveUnwrappedDecoder().")
    val GenericExtrasSemiautoDeriveMethods = Seq(
      // format: off
      GenericExtrasDeriveCodec, GenericExtrasDeriveEncoder, GenericExtrasDeriveDecoder,
      GenericExtrasDeriveConfiguredCodec, GenericExtrasDeriveConfiguredEncoder, GenericExtrasDeriveConfiguredDecoder,
      // format: on
    )
    val GenericExtrasDeriveUnwrappedMethods =
      Seq(GenericExtrasDeriveUnwrappedCodec, GenericExtrasDeriveUnwrappedEncoder, GenericExtrasDeriveUnwrappedDecoder)

    val DerivationAnnotConfiguration = Symbol("io/circe/derivation/annotations/Configuration#")
    val DerivationAnnotJsonCodec = Symbol("io/circe/derivation/annotations/JsonCodec#")
    val DerivationAnnotJsonKey = Symbol("io/circe/derivation/annotations/JsonKey#")
    val DerivationAnnotJsonNoDefault = Symbol("io/circe/derivation/annotations/JsonNoDefault#")
    val DerivationAnnotPackage = Symbol("io/circe/derivation/annotations/")
  }
  object types {
    def fromSymbol(sym: Symbol): Type =
      Type.Name(sym.displayName)
    private def select(sym: Symbol, name: String) =
      Type.Select(Term.Name(sym.displayName), Type.Name(name))
    val Encoder = fromSymbol(symbols.Encoder)
    val Decoder = fromSymbol(symbols.Decoder)
    val ConfiguredCodec = fromSymbol(symbols.ConfiguredCodec)
    val ConfiguredEncoder = fromSymbol(symbols.ConfiguredEncoder)
    val ConfiguredDecoder = fromSymbol(symbols.ConfiguredDecoder)
    val Codec = fromSymbol(symbols.Codec)
  }

  override def fix(implicit doc: SemanticDocument): Patch = {
    object names {
      sealed abstract class NameExtractor(symbol: Symbol, val shouldBeRemoved: Boolean = false) {
        val name: String = symbol.displayName
        val aliases: mutable.Set[String] = mutable.Set.empty
        def aliasOrName: String = aliases.headOption.getOrElse(name)
        def unapply(t: Name): Boolean = {
          t.value == name || aliases.contains(t.value)
        }
      }
      object NameExtractor {
        private lazy val extractors = Seq(
          // format: off
          JsonKey, JsonNoDefault, 
          JsonCodec, ConfiguredJsonCodec, ConfiguredEncoder, ConfiguredDecoder,
          DeriveCodec, DeriveEncoder, DeriveDecoder,
          DeriveConfiguredCodec, DeriveConfiguredEncoder, DeriveConfiguredDecoder,
          DeriveUnwrappedCodec, DeriveUnwrappedEncoder, DeriveUnwrappedDecoder,
          CirceConfiguration, CirceCodec, CirceEncoder, CirceDecoder,
          // format: on
        )
        def unapply(t: Name): Option[NameExtractor] =
          extractors.iterator.collectFirst {
            case extractor if extractor.unapply(t) => extractor
          }
      }
      object JsonKey extends NameExtractor(symbols.GenericExtrasJsonKey, shouldBeRemoved = true)
      object JsonNoDefault extends NameExtractor(symbols.GenericExtrasJsonNoDefault, shouldBeRemoved = true)
      object JsonCodec extends NameExtractor(symbols.GenericExtrasJsonCodec, shouldBeRemoved = true)
      object ConfiguredJsonCodec extends NameExtractor(symbols.GenericExtrasConfiguredJsonCodec, shouldBeRemoved = true)
      object ConfiguredEncoder extends NameExtractor(symbols.GenericExtrasConfiguredEncoder, shouldBeRemoved = true)
      object ConfiguredDecoder extends NameExtractor(symbols.GenericExtrasConfiguredDecoder, shouldBeRemoved = true)
      object CirceConfiguration extends NameExtractor(symbols.GenericExtrasConfiguration, shouldBeRemoved = true)
      object CirceCodec extends NameExtractor(symbols.Codec)
      object CirceEncoder extends NameExtractor(symbols.Encoder)
      object CirceDecoder extends NameExtractor(symbols.Decoder)
      object DeriveUnwrappedCodec
          extends NameExtractor(symbols.GenericExtrasDeriveUnwrappedCodec, shouldBeRemoved = true)
      object DeriveUnwrappedEncoder
          extends NameExtractor(symbols.GenericExtrasDeriveUnwrappedEncoder, shouldBeRemoved = true)
      object DeriveUnwrappedDecoder
          extends NameExtractor(symbols.GenericExtrasDeriveUnwrappedDecoder, shouldBeRemoved = true)
      object DeriveCodec extends NameExtractor(symbols.GenericExtrasDeriveCodec, shouldBeRemoved = true)
      object DeriveEncoder extends NameExtractor(symbols.GenericExtrasDeriveEncoder, shouldBeRemoved = true)
      object DeriveDecoder extends NameExtractor(symbols.GenericExtrasDeriveDecoder, shouldBeRemoved = true)
      object DeriveConfiguredCodec
          extends NameExtractor(symbols.GenericExtrasDeriveConfiguredCodec, shouldBeRemoved = true)
      object DeriveConfiguredEncoder
          extends NameExtractor(symbols.GenericExtrasDeriveConfiguredEncoder, shouldBeRemoved = true)
      object DeriveConfiguredDecoder
          extends NameExtractor(symbols.GenericExtrasDeriveConfiguredDecoder, shouldBeRemoved = true)
    }
    object annots {
      object JsonKey {
        def unapply(mod: Mod): Option[String] = mod match {
          case Mod.Annot(Init(names.JsonKey(), _, List(List(Lit.String(name))))) => Some(name)
          case _                                                                 => None
        }
      }
      object JsonNoDefault {
        def unapply(mod: Mod): Boolean = mod match {
          case Mod.Annot(Init(names.JsonNoDefault(), _, _)) => true
          case _                                            => false
        }
      }
    }

    val patches = Seq.newBuilder[Patch]

    val (companionObjects, explicitCodecForSymbol) = {
      val companionObjects = Map.newBuilder[String, Defn.Object]
      val codecForSymbol = Map.newBuilder[String, CodecKind]
      doc.tree.traverse {
        case t @ Importee.Name(names.NameExtractor(name)) if name.shouldBeRemoved =>
          patches += Patch.removeImportee(t)
        case t @ Importer(pkg, importees) =>
          pkg.symbol match {
            case symbols.DerivationAnnotPackage | symbols.GenericExtrasPackage | symbols.GenericExtrasSemiautoPackage =>
              patches += importees.map(Patch.removeImportee(_)).reduce(_ + _)
            case _ => ()
          }

        case t @ Importee.Rename(names.NameExtractor(name), alias) =>
          if (name.shouldBeRemoved) patches += Patch.removeImportee(t)
          name.aliases += alias.value

        case t: Defn.Object => companionObjects += t.name.value -> t
        case t: Defn with Member.Type with Stat.WithMods =>
          val name = t.name.value
          t.mods.foreach {
            case annot @ Mod.Annot(Init(names.JsonCodec(), _, args)) =>
              symbolOf(t).foreach(codecForSymbol += _.value -> CodecKind.Codec)

            case annot @ Mod.Annot(Init(names.ConfiguredJsonCodec(), _, args)) =>
              val kind = args.flatten match {
                case Term.Assign(Term.Name("encodeOnly"), Lit.Boolean(true)) :: _ => CodecKind.ConfiguredEncoder
                case Term.Assign(Term.Name("decodeOnly"), Lit.Boolean(true)) :: _ => CodecKind.ConfiguredDecoder
                case _                                                            => CodecKind.ConfiguredCodec
              }
              symbolOf(t).foreach(codecForSymbol += _.value -> kind)
            case _ => ()
          }
        case t =>
          def replaceImport(to: Symbol) = patches += Patch.removeGlobalImport(t.symbol) + Patch.addGlobalImport(to)
          def removeImport() = patches += Patch.removeGlobalImport(t.symbol)
          t.symbol match {
            case sym if sym.isNone => ()

            case symbols.GenericExtrasConfiguration       => replaceImport(symbols.Configuration)
            case symbols.GenericExtrasConfiguredJsonCodec => replaceImport(symbols.ConfiguredCodec)
            case symbols.GenericExtrasConfiguredEncoder   => replaceImport(symbols.ConfiguredEncoder)
            case symbols.GenericExtrasConfiguredDecoder   => replaceImport(symbols.ConfiguredDecoder)
            case symbols.GenericExtrasJsonCodec           => removeImport()
            case symbols.GenericExtrasJsonKey             => removeImport()

            case symbols.GenericExtrasDeriveUnwrappedCodec   => removeImport()
            case symbols.GenericExtrasDeriveUnwrappedEncoder => removeImport()
            case symbols.GenericExtrasDeriveUnwrappedDecoder => removeImport()

            case symbols.DerivationAnnotConfiguration => replaceImport(symbols.Configuration)
            case symbols.DerivationAnnotJsonCodec     => replaceImport(symbols.Codec)
            case symbols.DerivationAnnotJsonKey       => removeImport()
            case symbols.DerivationAnnotJsonNoDefault => removeImport()

            case _ => ()
          }
      }
      (companionObjects.result(), codecForSymbol.result())
    }
    def codecForSymbol(symbol: Symbol): Option[CodecKind] = explicitCodecForSymbol
      .get(symbol.value)
      .orElse {
        util
          .Try(symbol.info)
          .getOrElse(None)
          .flatMap(_.signature match {
            case cls: ClassSignature =>
              cls.parents.view
                .collect { case ref: TypeRef =>
                  codecForSymbol(ref.symbol)
                }
                .flatten
                .headOption
            case _ => None
          })
      }
    val memberRenamesInSymbol = mutable.Map.empty[String, Map[String, String]]

    def transform(
        tree: Defn with Member.Type with Stat.WithMods with Stat.WithTemplate with Tree.WithTParamClause,
        codecKind: CodecKind,
        annotation: Option[Mod.Annot],
        config: Configuration
    ): Unit = {
      val CodecKind(codecSymbol, codecType, codecContextBounds) = codecKind
      patches += Patch.addGlobalImport(codecSymbol)
      val filteredModifiers = tree.mods.diff(annotation.toList)
      if (tree.tparamClause.isEmpty && !config.isDefined) {
        // If there is no explicit annotation, is means parent is already implementing derivation
        if (annotation.isEmpty) () // no-op
        else {
          // Quick patch, we only need to add derivation clouses
          val usingDerivation = tree.templ
            .copy(derives = codecType :: tree.templ.derives)
          val newDefn = tree match {
            case t: Defn.Class => t.copy(templ = usingDerivation, mods = filteredModifiers)
            case t: Defn.Trait => t.copy(templ = usingDerivation, mods = filteredModifiers)
          }
          patches += Patch.replaceTree(tree, newDefn.syntax)
        }
      } else {
        // Workaround for poor circe derivation design: https://github.com/circe/circe/issues/1965
        // We need to create `given [T]: ConfiguredCodec[Type[T]] = ConfiguredCodec.derived` as workaround
        val newDefn = tree match {
          case t: Defn.Class =>
            t.copy(
              mods = filteredModifiers,
              ctor = t.ctor.copy(
                mods = t.ctor.mods,
                name = t.ctor.name,
                paramClauses = t.ctor.paramClauses.map { params =>
                  params.copy(values =
                    params.values.map(param =>
                      param.copy(mods = param.mods.filter {
                        case annots.JsonKey(_) => false
                        case _                 => true
                      })
                    )
                  )
                }
              )
            )
          case t: Defn.Trait => t.copy(mods = filteredModifiers)
        }
        patches += Patch.replaceTree(tree, newDefn.syntax)
        for {
          param <- newDefn.ctor.paramClauses.flatten
          mod <- param.mods
        } {
          mod match {
            case annot @ annots.JsonKey(name) => patches += Patch.removeTokens(annot.tokens)
            case _                            => ()
          }
        }

        val (configInstance, usingConfig) =
          if (!config.isDefined) (None, None)
          else {
            val configInstance = config.explicitConfig.getOrElse {
              patches += Patch.addGlobalImport(symbols.Configuration)
              // When using circe.derivation.annotation JsonCodec and JsonKey we probably don't have access to implicit Configuration
              if (codecSymbol == symbols.Codec && config.isDefined)
                Term.Select(
                  qual = Term.Name(symbols.Configuration.displayName),
                  name = Term.Name("default")
                )
              else
                Term.ApplyType(
                  fun = Term.Name("summon"),
                  targClause = Type.ArgClause(List(Type.Name(symbols.Configuration.displayName)))
                )
            }

            // It seems like derivation of ConfiguredJsonCodecs ignores `withTransformConstructorNames` arguments
            // Instead we emmit single `withTransformMemberNames
            val memberNameTransformer =
              if (config.renamedInStats.isEmpty && config.renamedInCtor.isEmpty) None
              else
                Some { (baseConfig: Term) =>
                  val allRenames = config.renamedInStats ++ config.renamedInCtor
                  Term.Apply(
                    fun = Term.Select(baseConfig, Term.Name("withTransformMemberNames")),
                    args = Term.PartialFunction(
                      allRenames.toList.map { case (name, rename) =>
                        Case(pat = Lit.String(name), cond = None, body = Lit.String(rename))
                      } :+ Case(
                        pat = Pat.Var(Term.Name("name")),
                        cond = None,
                        body = Term.Apply(
                          Term.Select(
                            configInstance,
                            Term.Name("transformMemberNames")
                          ),
                          List(Term.Name("name"))
                        )
                      )
                    ) :: Nil
                  )
                }
            val withoutDefaults =
              if (!config.noDefaults) None
              else
                Some { (baseConfig: Term) =>
                  Term.Select(baseConfig, Term.Name("withoutDefaults"))
                }
            val composedConfig = List(memberNameTransformer, withoutDefaults).flatten
              .foldRight[Term](configInstance)(_.apply(_))
            Some(configInstance) -> Some(Term.ArgClause(values = List(composedConfig), mod = Some(Mod.Using())))
          }

        val givenDerivedCodec = Defn.GivenAlias(
          mods = Nil,
          name = Name.Anonymous(),
          paramClauseGroup =
            if (tree.tparamClause.isEmpty) None
            else
              Some(
                Member.ParamClauseGroup(
                  tree.tparamClause.values.map(tparam => tparam.copy(cbounds = codecContextBounds ::: tparam.cbounds)),
                  Nil
                )
              ),
          decltpe = Type.Apply(
            tpe = codecType,
            args = {
              if (tree.tparamClause.isEmpty) tree.name
              else
                Type.Apply(
                  tree.name,
                  Type.ArgClause(
                    tree.tparamClause.map(v => Type.Name(v.name.value))
                  )
                )
            } :: Nil
          ),
          body = usingConfig.foldLeft[Term] {
            val derivedCodecName =
              if (codecSymbol != symbols.Codec) {
                patches += Patch.addGlobalImport(codecSymbol)
                Term.Name(codecSymbol.displayName)
              } else if (configInstance.isDefined) {
                patches += Patch.addGlobalImport(symbols.ConfiguredCodec)
                Term.Name(symbols.ConfiguredCodec.displayName)
              } else {
                patches += Patch.addGlobalImport(symbols.Codec)
                Term.Name(symbols.Codec.displayName)
              }
            Term.Select(derivedCodecName, Term.Name("derived"))
          }(Term.Apply(_, _))
        )

        val defaultIdent = "  "
        companionObjects.get(tree.name.value) match {
          case Some(companionTree) =>
            val Defn.Object(_, _, templ) = companionTree
            templ.stats.lastOption match {
              case Some(stat) =>
                val indent = " " * stat.pos.startColumn
                val newLine = System.lineSeparator() + indent
                patches += Patch.addRight(
                  stat,
                  List(givenDerivedCodec.syntax)
                    .mkString(newLine, newLine, "")
                )
              case None =>
                patches += Patch.replaceTree(
                  companionTree,
                  companionTree
                    .copy(templ = templ.copy(stats = List(givenDerivedCodec)))
                    .syntax
                ) + Patch.addLeft(givenDerivedCodec, defaultIdent)
            }
          case None =>
            val ident = " " * tree.pos.startColumn
            patches += Patch.addRight(
              tree,
              System.lineSeparator() + ident + Defn
                .Object(
                  mods = Nil,
                  name = Term.Name(tree.name.value),
                  templ = Template(
                    early = Nil,
                    inits = Nil,
                    self = Self(Name.Anonymous(), None),
                    stats = List(givenDerivedCodec)
                  )
                )
                .syntax
            )
        }
      }
    }

    doc.tree.traverse {
      case defnTree @ (_: Defn.Class | _: Defn.Trait) =>
        defnTree match {
          case t: Defn with Member.Type with Stat.WithMods with Stat.WithTemplate with Tree.WithTParamClause =>
            val renamedInCtor = Map.newBuilder[String, String]
            val renamedInStats = Map.newBuilder[String, String]
            var noDefaults = false
            val stats = defnTree match {
              case t: Defn.Class =>
                t.ctor.paramClauses.iterator.flatMap(_.values).flatMap(param => param.mods.map(param -> _)).foreach {
                  case (param, annot @ annots.JsonKey(name)) =>
                    renamedInCtor += param.name.value -> name
                  // case (param, annots.JsonNoDefault()) => noDefault += param.name.value
                  case _ => ()
                }
                t.templ.stats

              case t: Defn.Trait => t.templ.stats
            }

            renamedInStats ++= util
              .Try(t.symbol.info)
              .getOrElse(None)
              .map(_.signature)
              .toList
              .collect { case cls: ClassSignature =>
                cls.parents.collect { case TypeRef(_, symbol, _) =>
                  memberRenamesInSymbol.get(symbol.displayName).toList.flatten
                }.flatten
              }
              .flatten
              .distinct
            stats.iterator
              .collect {
                case member: Decl.Def => member
                case member: Decl.Val => member
                case member: Decl.Var => member
                case member: Defn.Val => member
                case member: Defn.Var => member
              }
              .flatMap(member => member.mods.map(member.symbol.displayName -> _))
              .foreach {
                case (member, annot @ annots.JsonKey(name)) =>
                  patches += Patch.removeTokens(annot.tokens)
                  renamedInStats += member -> name
                // case (member, annots.JsonNoDefault()) => noDefault += member
                case _ => ()
              }

            val (treeCodecAnnotation, explicitConfig) = t.mods
              .collectFirst {
                case annot @ Mod.Annot(Init(names.JsonCodec(), _, args))        => (annot, args.flatten.headOption)
                case annot @ Mod.Annot(Init(names.ConfiguredJsonCodec(), _, _)) => (annot, None)
              } match {
              case Some((annot, maybeConfig)) => Some(annot) -> maybeConfig
              case _                          => (None, None)
            }

            val config = Configuration(
              renamedInCtor = renamedInCtor.result(),
              renamedInStats = renamedInStats.result(),
              noDefaults = noDefaults,
              explicitConfig = explicitConfig
            )
            memberRenamesInSymbol += t.name.value -> config.renamedInStats

            symbolOf(t)
              .flatMap(codecForSymbol)
              // .orElse{println(s"No codec info for ${t.name}"); None}
              .foreach(transform(t, _, treeCodecAnnotation, config))
        }

      case defn: Defn with Tree.WithDeclTpeOpt with Tree.WithBody =>
        def valueClassDecoder(runtimeType: TypeRef, valueClassType: Type) = {
          patches += Patch.addGlobalImport(symbols.Decoder)
          // summon[Decoder[String]].map(ThingId(_))
          Term.Apply(
            Term.Select(
              Term.ApplyType(
                Term.Name("summon"),
                List(
                  Type.Apply(
                    Type.Name(names.CirceDecoder.aliasOrName),
                    List(Type.Name(runtimeType.symbol.displayName))
                  )
                )
              ),
              Term.Name("map")
            ),
            List(Term.Apply(Term.Name(valueClassType.symbol.displayName), List(Term.Placeholder())))
          )
        }
        def valueClassEncoder(tpe: TypeRef, valueName: String) = {
          patches += Patch.addGlobalImport(symbols.Encoder)
          // summon[Encoder[String]].contramap(_.param)
          Term.Apply(
            Term.Select(
              Term.ApplyType(
                Term.Name("summon"),
                List(
                  Type.Apply(
                    Type.Name(names.CirceEncoder.aliasOrName),
                    List(Type.Name(tpe.symbol.displayName))
                  )
                )
              ),
              Term.Name("contramap")
            ),
            List(Term.Select(Term.Placeholder(), Term.Name(valueName)))
          )
        }

        val rhsSymbol = defn.body.symbol
        if (symbols.GenericExtrasDeriveUnwrappedMethods.contains(rhsSymbol)) {
          patches ++= defn.decltpe
            .collect {
              case Type.Apply(_, valueClassType :: Nil) if !valueClassType.symbol.isNone =>
                val symbol = valueClassType.symbol
                symbol.info
                  .map(_.signature)
                  .collectFirst { case cls: ClassSignature =>
                    cls.declarations.filter(_.isConstructor).map(_.signature).collectFirst {
                      case MethodSignature(_, List(List(singleParam)), _) =>
                        singleParam.signature match {
                          case ValueSignature(runtimeType: TypeRef) => Some((runtimeType, singleParam))
                          case _                                    => None
                        }
                    }
                  }
                  .flatten
                  .flatten
                  .map { case (runtimeType, param) =>
                    rhsSymbol match {
                      case symbols.GenericExtrasDeriveUnwrappedEncoder =>
                        Patch.replaceTree(defn.body, valueClassEncoder(runtimeType, param.symbol.displayName).syntax)
                      case symbols.GenericExtrasDeriveUnwrappedDecoder =>
                        Patch.replaceTree(defn.body, valueClassDecoder(runtimeType, valueClassType).syntax)
                      case symbols.GenericExtrasDeriveUnwrappedCodec =>
                        // Codec.from(decoder, encoder)
                        val newBody = Term.Apply(
                          Term.Select(Term.Name(names.CirceCodec.aliasOrName), Term.Name("from")),
                          List(
                            valueClassDecoder(runtimeType, valueClassType),
                            valueClassEncoder(runtimeType, param.symbol.displayName)
                          )
                        )
                        Seq(
                          Patch.addGlobalImport(symbols.Codec),
                          Patch.replaceTree(defn.body, newBody.syntax)
                        ).asPatch
                    }
                  }
            }
            .flatten
            .toList
        } else if (symbols.GenericExtrasSemiautoDeriveMethods.contains(rhsSymbol)) {
          val bodyRewrite = rhsSymbol match {

            // New syntax
            case symbols.GenericExtrasDeriveConfiguredCodec   => Patch.replaceTree(defn.body, "Codec.derived")
            case symbols.GenericExtrasDeriveConfiguredEncoder => Patch.replaceTree(defn.body, "Encoder.derived")
            case symbols.GenericExtrasDeriveConfiguredDecoder =>
              Patch.replaceTree(defn.body, "Decoder.derivedConfigured")
            // Depreacted syntax, not requiring configuration
            case symbols.GenericExtrasDeriveCodec | symbols.GenericExtrasDeriveEncoder |
                symbols.GenericExtrasDeriveDecoder =>
              val semiautoObjRef = Term.Select(
                Term.Select(Term.Select(Term.Name("io"), Term.Name("circe")), Term.Name("generic")),
                Term.Name("semiauto")
              )
              Patch.addGlobalImport(Importer(semiautoObjRef, List(Importee.Wildcard())))

          }
          patches += bodyRewrite + Patch.removeGlobalImport(rhsSymbol)

        }
    }
    Patch.fromIterable(patches.result())
  }
}
