package io.expressier.utils

import shapeless._, shapeless.labelled.FieldType

/**
 * A type class witnessing that `B` corresponds to `A` with any labels removed.
 */
trait Unlabelled[A <: HList] extends DepFn1[A] {
  type Out <: HList

  def apply(a: A): Out
}

object Unlabelled extends LowPriorityUnlabelled {
  implicit val hnilUnlabelled: Aux[HNil, HNil] =
    new Unlabelled[HNil] {
      type Out = HNil
      def apply(a: HNil): HNil = a
    }

  implicit def hconsUnlabelled1[K, V, T <: HList, O <: HList](implicit
    unlabelled: Aux[T, O]
  ): Aux[FieldType[K, V] :: T, V :: O] =
    new Unlabelled[FieldType[K, V] :: T] {
      type Out = V :: O
      def apply(a: FieldType[K, V] :: T): V :: O = a.head :: unlabelled(a.tail)
    }
}

trait LowPriorityUnlabelled {
  type Aux[A <: HList, Out0 <: HList] = Unlabelled[A] { type Out = Out0 }

  implicit def hconsUnlabelled0[H, T <: HList, O <: HList](implicit
    unlabelled: Aux[T, O]
  ): Aux[H :: T, H :: O] = new Unlabelled[H :: T] {
    type Out = H :: O
    def apply(a: H :: T): H :: O = a.head :: unlabelled(a.tail)
  }
}
