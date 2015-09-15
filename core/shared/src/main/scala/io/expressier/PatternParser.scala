package io.expressier

import java.util.regex.Pattern
import scala.reflect.api.Universe

/**
 * Pattern parsing functionality.
 */
trait PatternParser {
  case class ResultItem[U <: Universe](
    name: Option[String],
    tpe: U#Type,
    converter: U#Tree => U#Tree
  )

  def parsePattern(u: Universe)(p: Pattern): Option[List[ResultItem[u.type]]]

  def stringResult(u: Universe)(name: Option[String]): ResultItem[u.type] =
    ResultItem[u.type](name, u.typeOf[String], tree => tree)

  def integerResult(u: Universe)(name: Option[String]): ResultItem[u.type] = {
    import u._
    ResultItem[u.type](name, typeOf[Int], tree => q"$tree.toInt")
  }

  def characterResult(u: Universe)(name: Option[String]): ResultItem[u.type] = {
    import u._
    ResultItem[u.type](name, typeOf[Char], tree => q"$tree.head")
  }
}
