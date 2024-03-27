package org.virtuslab.migration.scala3

import scalafix.v1._
import scala.meta._

class CirceLiteralMigration extends SemanticRule("CirceLiteralMigration") {
  object symbols {
    val LiteralJson = Symbol("io/circe/literal/json#")
    val LiteralJsonStringContext = Symbol("io/circe/literal/package.JsonStringContext().")
  }

  override def isRewrite: Boolean = true
  override def description: String =
    s"Rule used to migrate usages of `io.circe.literal` json interpolators. Replaces imports of `literal.JsonStringContext` used in Scala 2 with `literal.json` available in Scala 3"
  override def fix(implicit doc: SemanticDocument): Patch = {
    doc.tree
      .collect {
        case t: Importee if t.symbol == symbols.LiteralJsonStringContext =>
          Patch.removeImportee(t) + Patch.addGlobalImport(symbols.LiteralJson)
      }
      .asPatch
      .atomic
  }
}
