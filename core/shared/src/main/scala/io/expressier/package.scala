package io

import scala.language.experimental.macros

package object expressier {
  private[expressier] val patternParser: PatternParser =
    new io.expressier.internals.NosyPatternParser

  implicit class ExpressStringContext(sc: StringContext) {
    def x(): Any = macro WhiteboxMacros.stringContextImpl
  }

  implicit class ExpressString[S <: String](s: S) {
    def express[T]: Express.To[T] = macro BlackboxMacros.stringImpl[T]
  }
}
