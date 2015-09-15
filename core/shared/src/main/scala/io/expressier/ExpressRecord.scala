package io.expressier

import io.expressier.utils.Unlabelled
import shapeless.HList, shapeless.ops.hlist.Tupler, shapeless.syntax.DynamicRecordOps

class ExpressRecord[S <: String, Out <: HList, V <: HList, P](exp: Express.Aux[S, Out])(implicit
  unlabelled: Unlabelled.Aux[Out, V],
  tupler: Tupler.Aux[V, P]
) {
  def apply(input: String): Option[DynamicRecordOps[Out]] = exp(input).map(DynamicRecordOps(_))
  def unapply(input: String): Option[P] = exp(input).map(res => tupler(unlabelled(res)))
}
