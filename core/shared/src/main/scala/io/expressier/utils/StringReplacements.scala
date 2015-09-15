package io.expressier.utils

import shapeless._

/**
 * A type class witnessing that `B` corresponds to `A` with some (or all) of its
 * fields replaced with [[String]].
 */
trait StringReplacements[A <: HList, B <: HList] {
  def apply(a: A): B
}

object StringReplacements extends LowStringReplacements {
  implicit val hnilStringReplacements: StringReplacements[HNil, HNil] =
    new StringReplacements[HNil, HNil] {
      def apply(a: HNil): HNil = a
    }

  implicit def hconsStringReplacements1[H, T <: HList, O <: HList](implicit
    sr: StringReplacements[T, O]
  ): StringReplacements[H :: T, String :: O] = new StringReplacements[H :: T, String :: O] {
    def apply(a: H :: T): String :: O = a.head.toString :: sr(a.tail)
  }
}

trait LowStringReplacements {
  implicit def hconsStringReplacements0[H, T <: HList, O <: HList](implicit
    sr: StringReplacements[T, O]
  ): StringReplacements[H :: T, H :: O] = new StringReplacements[H :: T, H :: O] {
    def apply(a: H :: T): H :: O = a.head :: sr(a.tail)
  }
}
