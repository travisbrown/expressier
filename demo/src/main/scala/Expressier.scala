package expressier

import expressier.internals.NosyPatternParser
import java.util.regex.Pattern
import scala.language.experimental.macros
import scala.reflect.macros.Context
import scala.reflect.api.Universe
import scala.util.Try

case class Expressier[T <: Product](parse: String => Option[T]) {
  type Out = T
  def unapply(s: String): Option[Out] = parse(s)
}

object Expressier extends NosyPatternParser {
  def regex(pattern: String) = macro regexSimpleImpl

  def regexStringContextImpl(c: Context)(): c.Expr[Any] = {
    import c.universe._

    val patternString = c.prefix.tree match {
      case q"${_}(${_}(${s: String}))" => s
    }

    create(c)(patternString)
  }

  def regexStringImpl(c: Context): c.Expr[Any] = {
    import c.universe._

    val patternString = c.prefix.tree match {
      case q"${_}(${s: String})" => s
    }

    create(c)(patternString)
  }

  def regexSimpleImpl(c: Context)(pattern: c.Expr[String]): c.Expr[Any] = {
    import c.universe._

    val patternString = pattern.tree match {
      case q"${s: String}" => s
    }

    create(c)(patternString)
  }

  private[this] def tupleTypeName(u: Universe)(n: Int) = {
    import u._
    tq"_root_.scala.${newTypeName("Tuple" + n)}"
  }

  private[this] def create(c: Context)(patternString: String): c.Expr[Any] = {
    import c.universe.{ Try => _, _ }

    Try(patternString.r.pattern).toOption.flatMap(pattern =>
      parsePattern(c.universe)(pattern)
    ).fold(
      c.abort(c.enclosingPosition, "Can't parse this regular expression!")
    ) { results =>
      val productClassDef = createProductClass(c)(results)
      c.Expr[Any](q"""
        $productClassDef
        _root_.expressier.Expressier(${createParseFunction(c)(patternString, results, productClassDef.name)})
      """)
    }
  }

  private[this] def createProductClass(c: Context)(
    results: List[ResultItem[c.universe.type]]
  ): c.universe.ClassDef = {
    import c.universe._

    val productClassName = newTypeName(c.fresh)
    val resultNames = List.fill(results.size)(newTermName(c.fresh()))

    val superClass = tupleTypeName(c.universe)(results.size)
    val ctorParams = resultNames.zip(results.map(_.tpe)).map { case (name, tpe) => q"val $name: $tpe" }
    val namedAliases = results.zipWithIndex.flatMap {
      case (ResultItem(possibleName, _, _), i) =>
        val alias = newTermName(f"_${i + 1}%d")
        possibleName.map(name => q"def ${newTermName(name)} = $productClassName.this.$alias")
    }

    q"""
      class $productClassName(..$ctorParams) extends $superClass(..$resultNames) {
        ..$namedAliases
      }
    """
  }

  private[this] def createParseFunction(c: Context)(
    pattern: String,
    results: List[ResultItem[c.universe.type]],
    productClassName: c.universe.TypeName
  ) = {
    import c.universe._

    val parameterName = newTermName(c.fresh())
    val converted = createConversion(c)(results, productClassName)

    q"(($parameterName: ${tq""}) => $pattern.r.unapplySeq($parameterName).map($converted))"
  }

  private[this] def createConversion(c: Context)(
    results: List[ResultItem[c.universe.type]],
    productClassName: c.universe.TypeName
  ) = {
    import c.universe._

    val parameterName = newTermName(c.fresh())
    val resultNames = List.fill(results.size)(newTermName(c.fresh()))
    val resultPatterns = resultNames.map(n => Bind(n, Ident(nme.WILDCARD)))
    val converted = results.zip(resultNames).map {
      case (ResultItem(_, _, converter), name) => converter(Ident(name))
    }

    q"""
      (($parameterName: ${tq""}) => {
        $parameterName match {
          case List(..$resultPatterns) => new $productClassName(..$converted)
        }
      })
    """
  }
}

