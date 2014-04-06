package expressier

import expressier.internals.NosyPatternParser
import expressier.utils.ScalaReflectionUtils
import java.util.regex.Pattern
import scala.language.experimental.macros
import scala.reflect.macros.Context
import scala.reflect.api.Universe
import scala.util.Try

case class Expressier[T <: Product](parse: String => Option[T]) {
  type Out = T
  def unapply(s: String): Option[Out] = parse(s)
}

object Expressier extends NosyPatternParser with ScalaReflectionUtils {
  def regex(pattern: String) = macro regexSimpleImpl

  def regexStringContextImpl(c: Context)(): c.Expr[Any] = {
    import c.universe._

    val patternString = c.prefix.tree match {
      case Apply(_, List(Apply(_, List(Literal(Constant(s: String)))))) => s
    }

    create(c)(patternString)
  }

  def regexStringImpl(c: Context): c.Expr[Any] = {
    import c.universe._

    val patternString = c.prefix.tree match {
      case Apply(_, List(Literal(Constant(s: String)))) => s
    }

    create(c)(patternString)
  }

  def regexSimpleImpl(c: Context)(pattern: c.Expr[String]): c.Expr[Any] = {
    import c.universe._

    val patternString = pattern.tree match {
      case Literal(Constant(s: String)) => s
    }

    create(c)(patternString)
  }

  private[this] def tupleTypeName(u: Universe)(n: Int) =
    u.Select(u.Ident(u.newTermName("scala")), u.newTypeName(f"Tuple${n}%d"))

  private[this] def create(c: Context)(patternString: String): c.Expr[Any] = {
    import c.universe.{ Try => _, _ }

    Try(patternString.r.pattern).toOption.flatMap(pattern =>
      parsePattern(c.universe)(pattern)
    ).fold(
      c.abort(c.enclosingPosition, "Can't parse this regular expression!")
    ) { results =>
      val (productClassName, productClassDef) = createProductClass(c)(results)

      c.Expr[Any](
        Block(
          productClassDef :: Nil,
          Apply(
            Select(
              Select(Ident(newTermName("expressier")), newTermName("Expressier")),
              newTermName("apply")
            ),
            createParseFunction(c)(patternString, results, productClassName) :: Nil
          )
        )
      )
    }
  }

  private[this] def createProductClass(c: Context)(
    results: List[ResultItem[c.universe.type]]
  ): (c.universe.TypeName, c.universe.Tree) = {
    import c.universe._

    val productClassName = newTypeName(c.fresh)
    val resultNames = List.fill(results.size)(newTermName(c.fresh())) 

    val namedAliases = results.zipWithIndex.flatMap {
      case (ResultItem(possibleName, _, _), i) => println(possibleName);possibleName.map(name =>
        DefDef(
          Modifiers(),
          newTermName(name),
          Nil,
          Nil,
          TypeTree(),
          Select(This(productClassName), newTermName(f"_${i + 1}%d"))
        )
      )
    }

    (
      productClassName,
      ClassDef(
        Modifiers(),
        productClassName,
        Nil,
        Template(
          tupleTypeName(c.universe)(results.size) :: Nil,
          emptyValDef,
          constructorWithSameParameters(c.universe)(
            resultNames.zip(results.map(_.tpe))
          ) :: namedAliases
        )
      )
    )
  }

  private[this] def createParseFunction(c: Context)(
    pattern: String,
    results: List[ResultItem[c.universe.type]],
    productClassName: c.universe.TypeName
  ) = {
    import c.universe._

    val parameterName = newTermName(c.fresh())
    val resultNames = List.fill(results.size)(newTermName(c.fresh())) 

    Function(
      ValDef(
        Modifiers(Flag.PARAM),
        parameterName,
        TypeTree(),
        EmptyTree
      ) :: Nil,
      Apply(
        Select(
          Apply(
            Select(
              Select(Literal(Constant(pattern)), newTermName("r")),
              newTermName("unapplySeq")
            ),
            Ident(parameterName) :: Nil
          ),
          newTermName("map")
        ),
        createConversion(c)(results, productClassName) :: Nil
      )
    )
  }

  private[this] def createConversion(c: Context)(
    results: List[ResultItem[c.universe.type]],
    productClassName: c.universe.TypeName
  ) = {
    import c.universe._

    val parameterName = newTermName(c.fresh())
    val resultNames = List.fill(results.size)(newTermName(c.fresh())) 

    Function(
      ValDef(
        Modifiers(Flag.PARAM),
        parameterName,
        TypeTree(),
        EmptyTree
      ) :: Nil,
      Match(
        Ident(parameterName),
        CaseDef(
          Apply(
            reify(List).tree,
            resultNames.map(name => Bind(name, Ident(nme.WILDCARD)))
          ),
          EmptyTree,
          Apply(
            Select(New(Ident(productClassName)), nme.CONSTRUCTOR),
            results.zip(resultNames).map {
              case (ResultItem(_, _, converter), name) => converter(Ident(name))
            }
          )
        ) :: Nil
      )
    )
  }
}

