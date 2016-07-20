package com.github.tarao
package slickjdbc
package interpolation

import util.NonEmpty
import scala.annotation.implicitNotFound
import slick.jdbc.{SetParameter => SP, PositionedParameters}

trait ListParameter {
  @inline implicit def createSetList[T](implicit
    c: SP[T]
  ): SP[NonEmpty[T]] = new SetList[T, NonEmpty[T]](c)

  @inline implicit def listToPlaceholder[T](implicit
    p: ToPlaceholder[T]
  ): ToPlaceholder[NonEmpty[T]] = new ToPlaceholder.FromList[T, NonEmpty[T]](p)
}
object ListParameter extends ListParameter

trait ProductParameter {
  @inline implicit def createSetProduct[T](implicit
    check1: T <:< Product,
    check2: IsNotTuple[T]
  ): SP[T] = new SetProduct[T]

  @inline implicit def productToPlaceholder[T](implicit
    check1: T <:< Product,
    check2: IsNotTuple[T]
  ): ToPlaceholder[T] = new ToPlaceholder.FromProduct[T]
}
object ProductParameter extends ProductParameter

trait CompoundParameter extends ListParameter with ProductParameter
object CompoundParameter extends CompoundParameter

/** SetParameter for non-empty list types. */
class SetList[S, -T <: NonEmpty[S]](val c: SP[S]) extends SP[T] {
  def apply(param: T, pp: PositionedParameters): Unit = {
    param.foreach { item => c.asInstanceOf[SP[Any]](item, pp) }
  }
}

/** SetParameter for product types especially for case classes. */
class SetProduct[-T](implicit product: T <:< Product) extends SP[T] {
  def apply(prod: T, pp: PositionedParameters): Unit =
    for (v <- product(prod).productIterator) v match {
      case p: Product => new SetProduct[Product].apply(p, pp)
      case v => SP.SetSimpleProduct(Tuple1(v), pp)
    }
}

// $COVERAGE-OFF$

@implicitNotFound(msg = "Unsupported parameter type: ${T}.\n" +
  "[NOTE] You need an implicit of slick.jdbc.SetParameter[${T}] to pass a value of the type.")
sealed trait CheckParameter[-T]
object CheckParameter {
  implicit def valid[T](implicit c: SP[T]): CheckParameter[T] =
    new CheckParameter[T] {}
}

@implicitNotFound(msg = "A product is passed.\n" +
  "[NOTE] Use interpolation.CompoundParameter trait to enable passing a product.")
sealed trait CheckProduct[-T]
object CheckProduct {
  implicit def valid[T](implicit c: SP[T]): CheckProduct[T] =
    new CheckProduct[T] {}
}

@implicitNotFound(msg = "Illegal parameter type: ${T}.\n" +
  "[NOTE] A list is not allowed since it may be empty and breaks the query.\n" +
  "[NOTE] Pass a util.NonEmpty[] if you know that it is not empty.")
sealed trait CheckList[-T]
object CheckList {
  implicit def valid[T](implicit c: SP[T]): CheckList[T] =
    new CheckList[T] {}
}

@implicitNotFound(msg = "A non-empty list is passed.\n" +
  "[NOTE] Use interpolation.CompoundParameter trait to enable passing a non-empty list.")
sealed trait CheckNonEmpty[-T]
object CheckNonEmpty {
  implicit def valid[T](implicit c: SP[T]): CheckNonEmpty[T] =
    new CheckNonEmpty[T] {}
}

@implicitNotFound(msg = "A maybe-non-empty list is passed.\n" +
  "[NOTE] Break it into Some(_) or None to confirm that it is not empty.")
sealed trait CheckOptionNonEmpty[-T]
object CheckOptionNonEmpty {
  implicit def valid[T](implicit
    check: IsNotOptionNonEmpty[T],
    c: SP[T]
  ): CheckOptionNonEmpty[T] = new CheckOptionNonEmpty[T] {}
}

@implicitNotFound(msg = "Illegal parameter type: ${T}\n" +
  "[NOTE] An option is not allowed since it may be none and breaks the query.\n" +
  "[NOTE] Break it into Some(_) or None to confirm that it has a value.")
sealed trait CheckOption[-T]
object CheckOption {
  implicit def valid[T](implicit
    check: IsNotOption[T],
    c: SP[T]
  ): CheckOption[T] = new CheckOption[T] {}
}

sealed trait IsNotOptionNonEmpty[-T]
object IsNotOptionNonEmpty {
  implicit def valid[T]: IsNotOptionNonEmpty[T] = new IsNotOptionNonEmpty[T] {}
  implicit def ambig1[T]: IsNotOptionNonEmpty[Option[NonEmpty[T]]] =
    sys.error("unexpected")
  implicit def ambig2[T]: IsNotOptionNonEmpty[Option[NonEmpty[T]]] =
    sys.error("unexpected")
}

sealed trait IsNotOption[-T]
object IsNotOption {
  implicit def valid[T]: IsNotOption[T] = new IsNotOption[T] {}
  implicit def ambig1[S]: IsNotOption[Option[S]] = sys.error("unexpected")
  implicit def ambig2[S]: IsNotOption[Option[S]] = sys.error("unexpected")
}

sealed trait IsNotTuple[-T]
object IsNotTuple {
  implicit def valid[T]: IsNotTuple[T] = new IsNotTuple[T] {}
  implicit def ambig1[S](implicit tp: IsTuple[S]): IsNotTuple[S] =
    sys.error("unexpected")
  implicit def ambig2[S](implicit tp: IsTuple[S]): IsNotTuple[S] =
    sys.error("unexpected")
}

sealed trait IsTuple[-T]
object IsTuple {
  implicit def tuple2[T1, T2]: IsTuple[(T1, T2)] =
    new IsTuple[(T1, T2)] {}
  implicit def tuple3[T1, T2, T3]: IsTuple[(T1, T2, T3)] =
    new IsTuple[(T1, T2, T3)] {}
  implicit def tuple4[T1, T2, T3, T4]: IsTuple[(T1, T2, T3, T4)] =
    new IsTuple[(T1, T2, T3, T4)] {}
  implicit def tuple5[T1, T2, T3, T4, T5]: IsTuple[(T1, T2, T3, T4, T5)] =
    new IsTuple[(T1, T2, T3, T4, T5)] {}
  implicit def tuple6[T1, T2, T3, T4, T5, T6]: IsTuple[(T1, T2, T3, T4, T5, T6)] =
    new IsTuple[(T1, T2, T3, T4, T5, T6)] {}
  implicit def tuple7[T1, T2, T3, T4, T5, T6, T7]: IsTuple[(T1, T2, T3, T4, T5, T6, T7)] =
    new IsTuple[(T1, T2, T3, T4, T5, T6, T7)] {}
  implicit def tuple8[T1, T2, T3, T4, T5, T6, T7, T8]: IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8)] =
    new IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8)] {}
  implicit def tuple9[T1, T2, T3, T4, T5, T6, T7, T8, T9]: IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9)] =
    new IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9)] {}
  implicit def tuple10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10]: IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10)] =
    new IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10)] {}
  implicit def tuple11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11]: IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11)] =
    new IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11)] {}
  implicit def tuple12[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12]: IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12)] =
    new IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12)] {}
  implicit def tuple13[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13]: IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13)] =
    new IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13)] {}
  implicit def tuple14[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14]: IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14)] =
    new IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14)] {}
  implicit def tuple15[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15]: IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15)] =
    new IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15)] {}
  implicit def tuple16[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16]: IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16)] =
    new IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16)] {}
  implicit def tuple17[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17]: IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17)] =
    new IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17)] {}
  implicit def tuple18[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18]: IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18)] =
    new IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18)] {}
  implicit def tuple19[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19]: IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19)] =
    new IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19)] {}
  implicit def tuple20[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20]: IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20)] =
    new IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20)] {}
  implicit def tuple21[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21]: IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21)] =
    new IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21)] {}
  implicit def tuple22[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22]: IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22)] =
    new IsTuple[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22)] {}
}

// $COVERAGE-ON$
