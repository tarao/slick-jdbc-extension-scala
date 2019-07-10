package com.github.tarao
package slickjdbc
package interpolation

import util.NonEmpty

trait ToPlaceholder[-T] {
  def apply(value: T): Placeholder
}
object ToPlaceholder {
  class FromProduct[-T](implicit product: T <:< Product)
      extends ToPlaceholder[T] {
    def apply(value: T): Placeholder = {
      def rec(v: Any): Placeholder = v match {
        case s: Tuple1[_] =>
          Placeholder.Nested(1)
        case ne: NonEmpty[_] =>
          Placeholder()
        case p: Product if p.productArity <= 0 =>
          throw new java.sql.SQLException("No value to bind for " + p)
        case p: Product if p.productArity == 1 =>
          p.productIterator.map(rec _).toSeq.head
        case p: Product =>
          Placeholder.Nested(p.productIterator.flatMap(rec(_).toSeq).toSeq: _*)
        case _ => Placeholder()
      }
      rec(value)
    }
  }

  class FromList[S, -T <: NonEmpty[S]](p: ToPlaceholder[S])
      extends ToPlaceholder[T] {
    def apply(value: T): Placeholder =
      Placeholder.Nested(value.map(p.apply _).toList: _*)
  }

  class FromTuple[-T <: Product](children: ToPlaceholder[_]*)
      extends ToPlaceholder[T] {
    def apply(value: T): Placeholder = Placeholder.Nested(
      children.iterator.zip(value.productIterator).map { pair =>
        pair._1.asInstanceOf[ToPlaceholder[Any]].apply(pair._2)
      }.toSeq: _*
    )
  }

  @inline implicit def anyToPlaceholder[T]: ToPlaceholder[T] =
    new ToPlaceholder[T] {
      def apply(value: T): Placeholder = Placeholder()
    }

  @inline implicit def tuple1ToPlaceholder[T1](implicit
    p1: ToPlaceholder[T1]
  ): ToPlaceholder[Tuple1[T1]] =
    new FromTuple[Tuple1[T1]](p1)

  @inline implicit def tuple2ToPlaceholder[T1, T2](implicit
    p1: ToPlaceholder[T1],
    p2: ToPlaceholder[T2]
  ): ToPlaceholder[(T1, T2)] =
    new FromTuple[(T1, T2)](p1, p2)

  @inline implicit def tuple3ToPlaceholder[T1, T2, T3, T5](implicit
    p1: ToPlaceholder[T1],
    p2: ToPlaceholder[T2],
    p3: ToPlaceholder[T3]
  ): ToPlaceholder[(T1, T2, T3)] =
    new FromTuple[(T1, T2, T3)](p1, p2, p3)

  @inline implicit def tuple4ToPlaceholder[T1, T2, T3, T4](implicit
    p1: ToPlaceholder[T1],
    p2: ToPlaceholder[T2],
    p3: ToPlaceholder[T3],
    p4: ToPlaceholder[T4]
  ): ToPlaceholder[(T1, T2, T3, T4)] =
    new FromTuple[(T1, T2, T3, T4)](p1, p2, p3, p4)

  @inline implicit def tuple5ToPlaceholder[T1, T2, T3, T4, T5](implicit
    p1: ToPlaceholder[T1],
    p2: ToPlaceholder[T2],
    p3: ToPlaceholder[T3],
    p4: ToPlaceholder[T4],
    p5: ToPlaceholder[T5]
  ): ToPlaceholder[(T1, T2, T3, T4, T5)] =
    new FromTuple[(T1, T2, T3, T4, T5)](p1, p2, p3, p4, p5)

  @inline implicit def tuple6ToPlaceholder[T1, T2, T3, T4, T5, T6](implicit
    p1: ToPlaceholder[T1],
    p2: ToPlaceholder[T2],
    p3: ToPlaceholder[T3],
    p4: ToPlaceholder[T4],
    p5: ToPlaceholder[T5],
    p6: ToPlaceholder[T6]
  ): ToPlaceholder[(T1, T2, T3, T4, T5, T6)] =
    new FromTuple[(T1, T2, T3, T4, T5, T6)](p1, p2, p3, p4, p5, p6)

  @inline implicit def tuple7ToPlaceholder[T1, T2, T3, T4, T5, T6, T7](implicit
    p1: ToPlaceholder[T1],
    p2: ToPlaceholder[T2],
    p3: ToPlaceholder[T3],
    p4: ToPlaceholder[T4],
    p5: ToPlaceholder[T5],
    p6: ToPlaceholder[T6],
    p7: ToPlaceholder[T7]
  ): ToPlaceholder[(T1, T2, T3, T4, T5, T6, T7)] =
    new FromTuple[(T1, T2, T3, T4, T5, T6, T7)](p1, p2, p3, p4, p5, p6, p7)

  @inline implicit def tuple8ToPlaceholder[T1, T2, T3, T4, T5, T6, T7, T8](implicit
    p1: ToPlaceholder[T1],
    p2: ToPlaceholder[T2],
    p3: ToPlaceholder[T3],
    p4: ToPlaceholder[T4],
    p5: ToPlaceholder[T5],
    p6: ToPlaceholder[T6],
    p7: ToPlaceholder[T7],
    p8: ToPlaceholder[T8]
  ): ToPlaceholder[(T1, T2, T3, T4, T5, T6, T7, T8)] =
    new FromTuple[(T1, T2, T3, T4, T5, T6, T7, T8)](p1, p2, p3, p4, p5, p6, p7, p8)

  @inline implicit def tuple9ToPlaceholder[T1, T2, T3, T4, T5, T6, T7, T8, T9](implicit
    p1: ToPlaceholder[T1],
    p2: ToPlaceholder[T2],
    p3: ToPlaceholder[T3],
    p4: ToPlaceholder[T4],
    p5: ToPlaceholder[T5],
    p6: ToPlaceholder[T6],
    p7: ToPlaceholder[T7],
    p8: ToPlaceholder[T8],
    p9: ToPlaceholder[T9]
  ): ToPlaceholder[(T1, T2, T3, T4, T5, T6, T7, T8, T9)] =
    new FromTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9)](p1, p2, p3, p4, p5, p6, p7, p8, p9)

  @inline implicit def tuple10ToPlaceholder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10](implicit
    p1: ToPlaceholder[T1],
    p2: ToPlaceholder[T2],
    p3: ToPlaceholder[T3],
    p4: ToPlaceholder[T4],
    p5: ToPlaceholder[T5],
    p6: ToPlaceholder[T6],
    p7: ToPlaceholder[T7],
    p8: ToPlaceholder[T8],
    p9: ToPlaceholder[T9],
    p10: ToPlaceholder[T10]
  ): ToPlaceholder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10)] =
    new FromTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10)](p1, p2, p3, p4, p5, p6, p7, p8, p9, p10)

  @inline implicit def tuple11ToPlaceholder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11](implicit
    p1: ToPlaceholder[T1],
    p2: ToPlaceholder[T2],
    p3: ToPlaceholder[T3],
    p4: ToPlaceholder[T4],
    p5: ToPlaceholder[T5],
    p6: ToPlaceholder[T6],
    p7: ToPlaceholder[T7],
    p8: ToPlaceholder[T8],
    p9: ToPlaceholder[T9],
    p10: ToPlaceholder[T10],
    p11: ToPlaceholder[T11]
  ): ToPlaceholder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11)] =
    new FromTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11)](p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11)

  @inline implicit def tuple12ToPlaceholder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12](implicit
    p1: ToPlaceholder[T1],
    p2: ToPlaceholder[T2],
    p3: ToPlaceholder[T3],
    p4: ToPlaceholder[T4],
    p5: ToPlaceholder[T5],
    p6: ToPlaceholder[T6],
    p7: ToPlaceholder[T7],
    p8: ToPlaceholder[T8],
    p9: ToPlaceholder[T9],
    p10: ToPlaceholder[T10],
    p11: ToPlaceholder[T11],
    p12: ToPlaceholder[T12]
  ): ToPlaceholder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12)] =
    new FromTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12)](p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12)

  @inline implicit def tuple13ToPlaceholder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13](implicit
    p1: ToPlaceholder[T1],
    p2: ToPlaceholder[T2],
    p3: ToPlaceholder[T3],
    p4: ToPlaceholder[T4],
    p5: ToPlaceholder[T5],
    p6: ToPlaceholder[T6],
    p7: ToPlaceholder[T7],
    p8: ToPlaceholder[T8],
    p9: ToPlaceholder[T9],
    p10: ToPlaceholder[T10],
    p11: ToPlaceholder[T11],
    p12: ToPlaceholder[T12],
    p13: ToPlaceholder[T13]
  ): ToPlaceholder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13)] =
    new FromTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13)](p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13)

  @inline implicit def tuple14ToPlaceholder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14](implicit
    p1: ToPlaceholder[T1],
    p2: ToPlaceholder[T2],
    p3: ToPlaceholder[T3],
    p4: ToPlaceholder[T4],
    p5: ToPlaceholder[T5],
    p6: ToPlaceholder[T6],
    p7: ToPlaceholder[T7],
    p8: ToPlaceholder[T8],
    p9: ToPlaceholder[T9],
    p10: ToPlaceholder[T10],
    p11: ToPlaceholder[T11],
    p12: ToPlaceholder[T12],
    p13: ToPlaceholder[T13],
    p14: ToPlaceholder[T14]
  ): ToPlaceholder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14)] =
    new FromTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14)](p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14)

  @inline implicit def tuple15ToPlaceholder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15](implicit
    p1: ToPlaceholder[T1],
    p2: ToPlaceholder[T2],
    p3: ToPlaceholder[T3],
    p4: ToPlaceholder[T4],
    p5: ToPlaceholder[T5],
    p6: ToPlaceholder[T6],
    p7: ToPlaceholder[T7],
    p8: ToPlaceholder[T8],
    p9: ToPlaceholder[T9],
    p10: ToPlaceholder[T10],
    p11: ToPlaceholder[T11],
    p12: ToPlaceholder[T12],
    p13: ToPlaceholder[T13],
    p14: ToPlaceholder[T14],
    p15: ToPlaceholder[T15]
  ): ToPlaceholder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15)] =
    new FromTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15)](p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15)

  @inline implicit def tuple16ToPlaceholder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16](implicit
    p1: ToPlaceholder[T1],
    p2: ToPlaceholder[T2],
    p3: ToPlaceholder[T3],
    p4: ToPlaceholder[T4],
    p5: ToPlaceholder[T5],
    p6: ToPlaceholder[T6],
    p7: ToPlaceholder[T7],
    p8: ToPlaceholder[T8],
    p9: ToPlaceholder[T9],
    p10: ToPlaceholder[T10],
    p11: ToPlaceholder[T11],
    p12: ToPlaceholder[T12],
    p13: ToPlaceholder[T13],
    p14: ToPlaceholder[T14],
    p15: ToPlaceholder[T15],
    p16: ToPlaceholder[T16]
  ): ToPlaceholder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16)] =
    new FromTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16)](p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16)

  @inline implicit def tuple17ToPlaceholder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17](implicit
    p1: ToPlaceholder[T1],
    p2: ToPlaceholder[T2],
    p3: ToPlaceholder[T3],
    p4: ToPlaceholder[T4],
    p5: ToPlaceholder[T5],
    p6: ToPlaceholder[T6],
    p7: ToPlaceholder[T7],
    p8: ToPlaceholder[T8],
    p9: ToPlaceholder[T9],
    p10: ToPlaceholder[T10],
    p11: ToPlaceholder[T11],
    p12: ToPlaceholder[T12],
    p13: ToPlaceholder[T13],
    p14: ToPlaceholder[T14],
    p15: ToPlaceholder[T15],
    p16: ToPlaceholder[T16],
    p17: ToPlaceholder[T17]
  ): ToPlaceholder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17)] =
    new FromTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17)](p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17)

  @inline implicit def tuple18ToPlaceholder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18](implicit
    p1: ToPlaceholder[T1],
    p2: ToPlaceholder[T2],
    p3: ToPlaceholder[T3],
    p4: ToPlaceholder[T4],
    p5: ToPlaceholder[T5],
    p6: ToPlaceholder[T6],
    p7: ToPlaceholder[T7],
    p8: ToPlaceholder[T8],
    p9: ToPlaceholder[T9],
    p10: ToPlaceholder[T10],
    p11: ToPlaceholder[T11],
    p12: ToPlaceholder[T12],
    p13: ToPlaceholder[T13],
    p14: ToPlaceholder[T14],
    p15: ToPlaceholder[T15],
    p16: ToPlaceholder[T16],
    p17: ToPlaceholder[T17],
    p18: ToPlaceholder[T18]
  ): ToPlaceholder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18)] =
    new FromTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18)](p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18)

  @inline implicit def tuple19ToPlaceholder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19](implicit
    p1: ToPlaceholder[T1],
    p2: ToPlaceholder[T2],
    p3: ToPlaceholder[T3],
    p4: ToPlaceholder[T4],
    p5: ToPlaceholder[T5],
    p6: ToPlaceholder[T6],
    p7: ToPlaceholder[T7],
    p8: ToPlaceholder[T8],
    p9: ToPlaceholder[T9],
    p10: ToPlaceholder[T10],
    p11: ToPlaceholder[T11],
    p12: ToPlaceholder[T12],
    p13: ToPlaceholder[T13],
    p14: ToPlaceholder[T14],
    p15: ToPlaceholder[T15],
    p16: ToPlaceholder[T16],
    p17: ToPlaceholder[T17],
    p18: ToPlaceholder[T18],
    p19: ToPlaceholder[T19]
  ): ToPlaceholder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19)] =
    new FromTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19)](p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19)

  @inline implicit def tuple20ToPlaceholder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20](implicit
    p1: ToPlaceholder[T1],
    p2: ToPlaceholder[T2],
    p3: ToPlaceholder[T3],
    p4: ToPlaceholder[T4],
    p5: ToPlaceholder[T5],
    p6: ToPlaceholder[T6],
    p7: ToPlaceholder[T7],
    p8: ToPlaceholder[T8],
    p9: ToPlaceholder[T9],
    p10: ToPlaceholder[T10],
    p11: ToPlaceholder[T11],
    p12: ToPlaceholder[T12],
    p13: ToPlaceholder[T13],
    p14: ToPlaceholder[T14],
    p15: ToPlaceholder[T15],
    p16: ToPlaceholder[T16],
    p17: ToPlaceholder[T17],
    p18: ToPlaceholder[T18],
    p19: ToPlaceholder[T19],
    p20: ToPlaceholder[T20]
  ): ToPlaceholder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20)] =
    new FromTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20)](p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20)

  @inline implicit def tuple21ToPlaceholder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21](implicit
    p1: ToPlaceholder[T1],
    p2: ToPlaceholder[T2],
    p3: ToPlaceholder[T3],
    p4: ToPlaceholder[T4],
    p5: ToPlaceholder[T5],
    p6: ToPlaceholder[T6],
    p7: ToPlaceholder[T7],
    p8: ToPlaceholder[T8],
    p9: ToPlaceholder[T9],
    p10: ToPlaceholder[T10],
    p11: ToPlaceholder[T11],
    p12: ToPlaceholder[T12],
    p13: ToPlaceholder[T13],
    p14: ToPlaceholder[T14],
    p15: ToPlaceholder[T15],
    p16: ToPlaceholder[T16],
    p17: ToPlaceholder[T17],
    p18: ToPlaceholder[T18],
    p19: ToPlaceholder[T19],
    p20: ToPlaceholder[T20],
    p21: ToPlaceholder[T21]
  ): ToPlaceholder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21)] =
    new FromTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21)](p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20, p21)

  @inline implicit def tuple22ToPlaceholder[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22](implicit
    p1: ToPlaceholder[T1],
    p2: ToPlaceholder[T2],
    p3: ToPlaceholder[T3],
    p4: ToPlaceholder[T4],
    p5: ToPlaceholder[T5],
    p6: ToPlaceholder[T6],
    p7: ToPlaceholder[T7],
    p8: ToPlaceholder[T8],
    p9: ToPlaceholder[T9],
    p10: ToPlaceholder[T10],
    p11: ToPlaceholder[T11],
    p12: ToPlaceholder[T12],
    p13: ToPlaceholder[T13],
    p14: ToPlaceholder[T14],
    p15: ToPlaceholder[T15],
    p16: ToPlaceholder[T16],
    p17: ToPlaceholder[T17],
    p18: ToPlaceholder[T18],
    p19: ToPlaceholder[T19],
    p20: ToPlaceholder[T20],
    p21: ToPlaceholder[T21],
    p22: ToPlaceholder[T22]
  ): ToPlaceholder[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22)] =
    new FromTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22)](p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p20, p21, p22)
}
