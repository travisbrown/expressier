package io.expressier

import io.expressier.utils.StringReplacements
import scala.language.experimental.macros
import shapeless._
import shapeless.ops.hlist.ZipWithKeys
import shapeless.ops.record.{ Keys, SelectAll, Values }

/**
 * A type class witnessing that `S` is a regular expression that can be used to
 * parse strings into values of type `Out`.
 */
trait Express[S <: String] {
  type Out
  def apply(input: String): Option[Out]
  def unapply(input: String): Option[Out] = apply(input)
}

object Express extends MidPriorityExpress {
  type To[Out0] = Express[_] { type Out = Out0 }

  implicit def materializeExpress[S <: String, T <: HList]: Aux[S, T] =
    macro WhiteboxMacros.materializeExpressImpl[S, T]
}

trait MidPriorityExpress extends LowPriorityExpress {
  implicit def labelledGenericExpress[S <: String, C, R <: HList](implicit
    gen: LabelledGeneric.Aux[C, R],
    exp: Aux[S, R]
  ): Aux[S, C] = new Express[S] {
    type Out = C
    def apply(input: String): Option[C] = exp(input).map(gen.from)
  }

  implicit def genericExpress[S <: String, C, R <: HList](implicit
    gen: Generic.Aux[C, R],
    exp: Aux[S, R]
  ): Aux[S, C] = new Express[S] {
    type Out = C
    def apply(input: String): Option[C] = exp(input).map(gen.from)
  }
}

/**
 * More complex instances.
 */
trait LowPriorityExpress {
  type Aux[S <: String, Out0] = Express[S] { type Out = Out0 }

  implicit def stringReplacedLabelledGenericExpress[
    S <: String,
    C,
    Repr <: HList,
    ReprK <: HList,
    ReprV <: HList,
    Rec <: HList,
    RecV <: HList
  ](implicit
    gen: LabelledGeneric.Aux[C, Repr],
    keys: Keys.Aux[Repr, ReprK],
    vals: Values.Aux[Repr, ReprV],
    exp: Aux[S, Rec],
    sel: SelectAll.Aux[Rec, ReprK, RecV],
    sr: StringReplacements[RecV, ReprV],
    zipper: ZipWithKeys.Aux[ReprK, ReprV, Repr]
  ): Aux[S, C] = new Express[S] {
    type Out = C
    def apply(input: String): Option[C] =
      exp(input).map(res => gen.from(zipper(sr(sel(res)))))
  }

  implicit def stringReplacedGenericExpress[S <: String, C, T <: HList, R <: HList](implicit
    gen: Generic.Aux[C, R],
    exp: Aux[S, T],
    sr: StringReplacements[T, R]
  ): Aux[S, C] = new Express[S] {
    type Out = C
    def apply(input: String): Option[C] = exp(input).map(res => gen.from(sr(res)))
  }
}
