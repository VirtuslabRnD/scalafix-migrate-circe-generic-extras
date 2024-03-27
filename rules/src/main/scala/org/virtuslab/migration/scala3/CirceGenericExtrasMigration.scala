package org.virtuslab.migration.scala3

import scalafix.v1._
import scala.meta._
import scala.collection.mutable

class CirceGenericExtrasMigration extends SemanticRule("CirceGenericExtrasMigration") {
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
    object Codec extends CodecKind(symbols.Codec, types.CodecAsObject, List(types.Encoder, types.Decoder))
    object ConfiguredCodec
        extends CodecKind(symbols.ConfiguredCodec, types.ConfiguredCodec, List(types.Encoder, types.Decoder))
    object ConfiguredEncoder extends CodecKind(symbols.ConfiguredEncoder, types.ConfiguredEncoder, List(types.Encoder))
    object ConfiguredDecoder extends CodecKind(symbols.ConfiguredDecoder, types.ConfiguredDecoder, List(types.Decoder))
  }

  object symbols {
    val Codec = Symbol("io/circe/Codec#")
    val CodecAsObject = Symbol("io/circe/Codec/AsObject#")
    val Encoder = Symbol("io/circe/Encoder#")
    val Decoder = Symbol("io/circe/Decoder#")

    val ConfiguredCodec = Symbol("io/circe/derivation/ConfiguredCodec#")
    val ConfiguredEncoder = Symbol("io/circe/derivation/ConfiguredEncoder#")
    val ConfiguredDecoder = Symbol("io/circe/derivation/ConfiguredDecoder#")
    val Configuration = Symbol("io/circe/derivation/Configuration#")

    val GenericExtrasPackage = Symbol("io/circe/generic/extras/")
    val GenericExtrasConfiguration = Symbol("io/circe/generic/extras/Configuration#")
    val GenericExtrasConfiguredCodec = Symbol("io/circe/generic/extras/Configuration#")
    val GenericExtrasConfiguredEncoder = Symbol("io/circe/generic/extras/ConfiguredEncoder#")
    val GenericExtrasConfiguredDecoder = Symbol("io/circe/generic/extras/ConfiguredDecoder#")
    val GenericExtrasConfiguredJsonCodec = Symbol("io/circe/generic/extras/ConfiguredJsonCodec#")
    val GenericExtrasJsonCodec = Symbol("io/circe/generic/extras/JsonCodec#")
    val GenericExtrasJsonKey = Symbol("io/circe/generic/extras/JsonKey#")
    val GenericExtrasJsonNoDefault = Symbol("io/circe/generic/extras/JsonNoDefault#")
    val DeriveUnwrappedCodec = Symbol("io/circe/generic/extras/semiauto.deriveUnwrappedCodec().")

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
    val CodecAsObject = select(symbols.Codec, "AsObject")
  }

  override def fix(implicit doc: SemanticDocument): Patch = {
    object names {
      sealed abstract class NameExtractor(val name: String) {
        val aliases: mutable.Set[String] = mutable.Set.empty
        def unapply(t: Name): Boolean =
          t.value == name || aliases.contains(t.value)
      }
      object ReplacedName {
        val extractors = Seq(
          JsonCodec,
          ConfiguredJsonCodec,
          ConfiguredEncoder,
          ConfiguredDecoder,
          Configuration,
          JsonKey,
          JsonNoDefault
        )
        def unapply(t: Name): Option[NameExtractor] =
          extractors.iterator.collectFirst {
            case extractor if extractor.unapply(t) => extractor
          }
      }
      object JsonKey extends NameExtractor(symbols.GenericExtrasJsonKey.displayName)
      object JsonNoDefault extends NameExtractor(symbols.GenericExtrasJsonNoDefault.displayName)
      object JsonCodec extends NameExtractor(symbols.GenericExtrasJsonCodec.displayName)
      object ConfiguredJsonCodec extends NameExtractor(symbols.GenericExtrasConfiguredJsonCodec.displayName)
      object ConfiguredEncoder extends NameExtractor(symbols.GenericExtrasConfiguredEncoder.displayName)
      object ConfiguredDecoder extends NameExtractor(symbols.GenericExtrasConfiguredDecoder.displayName)
      object Configuration extends NameExtractor(symbols.GenericExtrasConfiguration.displayName)

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
        case t @ Importee.Name(names.ReplacedName(_)) =>
          patches += Patch.removeImportee(t)
        case t @ Importer(pkg, importees) =>
          pkg.symbol match {
            case symbols.DerivationAnnotPackage | symbols.GenericExtrasPackage =>
              patches += importees.map(Patch.removeImportee(_)).reduce(_ + _)
            case _ => ()
          }

        case t @ Importee.Rename(names.ReplacedName(extractor), alias) =>
          patches += Patch.removeImportee(t)
          extractor.aliases += alias.value

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

        val (configInstance, usingConfig) = Option
          .when(config.isDefined) {
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

            val ctorNameTransformer = Option.when(config.renamedInCtor.nonEmpty) { (baseConfig: Term) =>
              Term.Apply(
                fun = Term.Select(baseConfig, Term.Name("withTransformConstructorNames")),
                args = Term
                  .PartialFunction(
                    config.renamedInCtor.map { case (name, rename) =>
                      Case(pat = Lit.String(name), cond = None, body = Lit.String(rename))
                    }.toList :+ Case(
                      pat = Pat.Var(Term.Name("name")),
                      cond = None,
                      body = Term.Apply(
                        Term.Select(
                          configInstance,
                          Term.Name("transformConstructorNames")
                        ),
                        List(Term.Name("name"))
                      )
                    )
                  ) :: Nil
              )
            }
            val memberNameTransformer = Option.when(config.renamedInStats.nonEmpty) { (baseConfig: Term) =>
              Term.Apply(
                fun = Term.Select(baseConfig, Term.Name("withTransformMemberNames")),
                args = Term.PartialFunction(
                  config.renamedInStats.map { case (name, rename) =>
                    Case(pat = Lit.String(name), cond = None, body = Lit.String(rename))
                  }.toList :+ Case(
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
            val withoutDefaults = Option.when(config.noDefaults) { (baseConfig: Term) =>
              Term.Select(baseConfig, Term.Name("withoutDefaults"))
            }
            val composedConfig = List(ctorNameTransformer, memberNameTransformer, withoutDefaults).flatten
              .foldRight[Term](configInstance)(_.apply(_))
            configInstance -> Term.ArgClause(values = List(composedConfig), mod = Some(Mod.Using()))
          }
          .unzip

        val givenDerivedCodec = Defn.GivenAlias(
          mods = Nil,
          name = Name.Anonymous(),
          paramClauseGroup = Option.when(tree.tparamClause.nonEmpty)(
            Member.ParamClauseGroup(
              tree.tparamClause.values.map(tparam => tparam.copy(cbounds = codecContextBounds ::: tparam.cbounds)),
              Nil
            )
          ),
          decltpe = Type.Apply(
            tpe = codecType match {
              case types.CodecAsObject => types.Codec
              case tpe                 => tpe
            },
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
                Term.Select(Term.Name(symbols.Codec.displayName), Term.Name(symbols.CodecAsObject.displayName))
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
                t.ctor.paramClauses.iterator.flatten.flatMap(param => param.mods.map(param -> _)).foreach {
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

            val (treeCodecAnnotation, explicitConfig) = t.mods.collectFirst {
              case annot @ Mod.Annot(Init(names.JsonCodec(), _, args))        => (annot, args.flatten.headOption)
              case annot @ Mod.Annot(Init(names.ConfiguredJsonCodec(), _, _)) => (annot, None)
            }.unzip

            val config = Configuration(
              renamedInCtor = renamedInCtor.result(),
              renamedInStats = renamedInStats.result(),
              noDefaults = noDefaults,
              explicitConfig = explicitConfig.flatten
            )
            memberRenamesInSymbol += t.name.value -> config.renamedInStats

            symbolOf(t)
              .flatMap(codecForSymbol)
              // .orElse{println(s"No codec info for ${t.name}"); None}
              .foreach(transform(t, _, treeCodecAnnotation, config))
        }

      case defn: Defn with Tree.WithDeclTpeOpt with Tree.WithBody =>
        defn.body.symbol match {
          case symbols.DeriveUnwrappedCodec =>
            // implicit val codec: Codec[ThingId] = Codec.from(
            //   summon[Decoder[String]].map(ThingId(_)),
            //   summon[Encoder[String]].contramap(_.value)
            // )
            defn.decltpe
              .collect {
                case Type.Apply(Type.Name("Codec"), tpe :: Nil) if !tpe.symbol.isNone => tpe
              }
              .map { tpe =>
                val symbol = tpe.symbol
                val ctor = symbol.info.map(_.signature).collect { case cls: ClassSignature =>
                  cls.declarations.filter(_.isConstructor).map(_.signature).collect {
                    case MethodSignature(_, List(List(singleParam)), _) =>
                      singleParam.signature match {
                        case ValueSignature(tpe: TypeRef) =>
                          // Codec.from(decoder, encoder)
                          val newBody = Term.Apply(
                            Term.Select(Term.Name(symbols.Codec.displayName), Term.Name("from")),
                            List(
                              // summon[Decoder[String]].map(ThingId(_))
                              Term.Apply(
                                Term.Select(
                                  Term.ApplyType(
                                    Term.Name("summon"),
                                    List(
                                      Type.Apply(
                                        Type.Name(symbols.Decoder.displayName),
                                        List(Type.Name(tpe.symbol.displayName))
                                      )
                                    )
                                  ),
                                  Term.Name("map")
                                ),
                                List(Term.Apply(Term.Name(symbol.displayName), List(Term.Placeholder())))
                              ),
                              // summon[Encoder[String]].contramap(_.param)
                              Term.Apply(
                                Term.Select(
                                  Term.ApplyType(
                                    Term.Name("summon"),
                                    List(
                                      Type.Apply(
                                        Type.Name(symbols.Encoder.displayName),
                                        List(Type.Name(tpe.symbol.displayName))
                                      )
                                    )
                                  ),
                                  Term.Name("contramap")
                                ),
                                List(Term.Select(Term.Placeholder(), Term.Name(singleParam.displayName)))
                              )
                            )
                          )
                          patches += Seq(
                            Patch.addGlobalImport(symbols.Codec),
                            Patch.addGlobalImport(symbols.Encoder),
                            Patch.addGlobalImport(symbols.Decoder),
                            Patch.replaceTree(defn.body, newBody.syntax)
                          ).asPatch

                        case _ => 
                      }
                  }
                }
              }
          // patches += Patch.replaceTree(defn.body, )
          case _ => ()
        }
    }
    Patch.fromIterable(patches.result())
  }
}
