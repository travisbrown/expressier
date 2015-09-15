package io.expressier

import scala.language.experimental.macros
import scala.reflect.macros.{ blackbox, whitebox }
import shapeless.HList

@macrocompat.bundle
class WhiteboxMacros(val c: whitebox.Context) {
  import patternParser.ResultItem
  import c.universe._
  import compat._

  private[this] val expressTC = c.typeOf[Express[_]].typeConstructor
  private[this] val symTpe = c.typeOf[scala.Symbol]
  private[this] val tagTC = c.typeOf[shapeless.tag.@@[_, _]].typeConstructor
  private[this] val taggedSym = typeOf[shapeless.tag.Tagged[_]].typeConstructor.typeSymbol

  private[this] def stringSingletonTpe(s: String): c.Type = ConstantType(Constant(s))
  private[this] def symbolSingletonTpe(s: String): c.Type =
    appliedType(tagTC, List(symTpe, stringSingletonTpe(s)))

  private[this] def mkSymbolSingleton(s: String): Tree =
    q"""_root_.scala.Symbol($s).asInstanceOf[${symbolSingletonTpe(s)}]"""

  def stringContextImpl(): c.Expr[Any] = {
    val patternString = c.prefix.tree match {
      case q"${_}(${_}(${s: _root_.java.lang.String}))" => s
    }

    val instance = c.inferImplicitValue(appliedType(expressTC, stringSingletonTpe(patternString)))

    c.Expr[Any](q"new _root_.io.expressier.ExpressRecord($instance)")
  }

  def materializeExpressImpl[S <: String, T <: HList](implicit
    S: c.WeakTypeTag[S]
  ): c.Expr[Express.Aux[S, T]] = {
    val patternString = S.tpe match {
      case ConstantType(Constant(s: String)) => s
      case _ => c.abort(c.enclosingPosition, "Not a string literal singleton")
    }

    scala.util.Try(patternString.r.pattern).toOption.flatMap(pattern =>
      patternParser.parsePattern(c.universe)(pattern)
    ).fold(
      c.abort(c.enclosingPosition, "Can't parse this regular expression")
    ) { results =>
      val hlistTpe = results.foldRight(tq"_root_.shapeless.HNil": c.Tree) {
        case (ResultItem(None, tpe, _), acc) => tq"_root_.shapeless.::[$tpe, $acc]"
        case (ResultItem(Some(name), tpe, _), acc) =>
          val nameTpe = mkSymbolSingleton(name)
          tq"""_root_.shapeless.::[_root_.shapeless.labelled.FieldType[$nameTpe, $tpe], $acc]"""
      }

      val resultNames = List.fill(results.size)(TermName(c.fresh()))
      val resultPatterns = resultNames.map(n => Bind(n, Ident(termNames.WILDCARD)))

      val converted = results.zip(resultNames).foldRight(q"_root_.shapeless.HNil": c.Tree) {
        case ((ResultItem(None, _, converter), resultName), acc) =>
          q"_root_.shapeless.::(${ converter(Ident(resultName)) }, $acc)"
        case ((ResultItem(Some(name), _, converter), resultName), acc) =>
          val nameTpe = mkSymbolSingleton(name)
          q"""
            _root_.shapeless.::(
              _root_.shapeless.labelled.field[$nameTpe](${ converter(Ident(resultName)) }),
              $acc
            )
          """
      }

      c.Expr[Express.Aux[S, T]](
        q"""
          new _root_.io.expressier.Express[${c.weakTypeOf[S]}] {
            type Out = $hlistTpe
            private[this] val pattern = $patternString.r
            def apply(input: _root_.java.lang.String): _root_.scala.Option[$hlistTpe] =
              pattern.unapplySeq(input).map {
                case _root_.scala.List(..$resultPatterns) => $converted
              }
          }
        """
      )
    }
  }
}

@macrocompat.bundle
class BlackboxMacros(val c: blackbox.Context) {
  import c.universe._
  import compat._

  private[this] def stringSingletonTpe(s: String): c.Type = ConstantType(Constant(s))

  def stringImpl[T](implicit T: c.WeakTypeTag[T]): c.Expr[Express.To[T]] = {
    val patternString = c.prefix.tree match {
      case q"${_}(${s: _root_.java.lang.String})" => s
    }

    c.Expr[Express.To[T]](q"""
      implicitly[
        _root_.io.expressier.Express.Aux[${stringSingletonTpe(patternString)}, $T]
      ]: _root_.io.expressier.Express.To[$T]
    """)
  }
}
