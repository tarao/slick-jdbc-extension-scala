package com.github.tarao
package slickjdbc
package interpolation

import eu.timepit.refined.api.RefType
import eu.timepit.refined.collection.NonEmpty
import scala.annotation.implicitNotFound
import scala.language.higherKinds
import scala.language.implicitConversions
import slick.jdbc.{SetParameter => SP, PositionedParameters}

trait ListParameter {
  @inline implicit def createSetNonEmptyList[A, L[X] <: Iterable[X], F[_, _]](implicit
    c: SP[A],
    rt: RefType[F]
  ): SP[F[L[A], NonEmpty]] =
    new SetNonEmptyList[A, L, F, F[L[A], NonEmpty]](c, rt)

  @inline implicit def nonEmptyListToPlaceholder[A, L[X] <: Iterable[X], F[_, _]](implicit
    p: ToPlaceholder[A],
    rt: RefType[F]
  ): ToPlaceholder[F[L[A], NonEmpty]] =
    new ToPlaceholder.FromNonEmptyList[A, L, F, F[L[A], NonEmpty]](p, rt)
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
class SetNonEmptyList[A, L[X] <: Iterable[X], F[_, _], -T <: F[L[A], NonEmpty]](val c: SP[A], rt: RefType[F]) extends SP[T] {
  def apply(param: T, pp: PositionedParameters): Unit = {
    rt.unwrap(param).foreach(item => c.asInstanceOf[SP[Any]](item, pp))
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
sealed trait ValidParameter[-T]
object ValidParameter {
  implicit def valid[T](implicit c: SP[T]): ValidParameter[T] =
    new ValidParameter[T] {}
}

@implicitNotFound(msg = "A product ${T} is passed.\n" +
  "[NOTE] Use interpolation.CompoundParameter trait to enable passing a product.")
sealed trait ValidProduct[-T]
object ValidProduct {
  implicit def valid1[T](implicit
    c: SP[T],
    product: T <:< Product
  ): ValidProduct[T] = new ValidProduct[T] {}

  implicit def valid2[T](implicit check: IsNotProduct[T]): ValidProduct[T] =
    new ValidProduct[T] {}
}

@implicitNotFound(msg = "A non-empty list is passed.\n" +
  "[NOTE] Use interpolation.CompoundParameter trait to enable passing a non-empty list.")
sealed trait ValidRefinedNonEmpty[-T]
object ValidRefinedNonEmpty {
  implicit def valid1[A, L[X] <: Iterable[X], F[_, _]](implicit
    c: SP[F[L[A], NonEmpty]],
    rt: RefType[F]
  ): ValidRefinedNonEmpty[F[L[A], NonEmpty]] =
    new ValidRefinedNonEmpty[F[L[A], NonEmpty]] {}

  implicit def valid2[T](implicit
    check: IsNotRefinedNonEmpty[T]
  ): ValidRefinedNonEmpty[T] = new ValidRefinedNonEmpty[T] {}
}

@implicitNotFound(msg = "Illegal parameter type: ${T}.\n" +
  "[NOTE] A list is not allowed since it may be empty and breaks the query.\n" +
  "[NOTE] Pass a ${T} Refind NonEmpty if you know that it is not empty.")
sealed trait ListRejected[-T]
object ListRejected {
  implicit def valid[T](implicit check: IsNotList[T]): ListRejected[T] =
    new ListRejected[T] {}
}

@implicitNotFound(msg = "Illegal parameter type: ${T}\n" +
  "[NOTE] An option is not allowed since it may be none and breaks the query.\n" +
  "[NOTE] Break it into Some(_) or None to confirm that it has a value.")
sealed trait OptionRejected[-T]
object OptionRejected {
  implicit def valid[T](implicit check: IsNotOption[T]): OptionRejected[T] =
    new OptionRejected[T] {}
}

@implicitNotFound(msg = "Illegal parameter type: ${T}\n" +
  "[NOTE] Break it into Left(_) or Right(_) to confirm that it can be embedded into the query.")
sealed trait EitherRejected[-T]
object EitherRejected {
  implicit def valid[T](implicit check: IsNotEither[T]): EitherRejected[T] =
    new EitherRejected[T] {}
}

sealed trait IsNotProduct[-T]
object IsNotProduct {
  implicit def valid[T]: IsNotProduct[T] = new IsNotProduct[T] {}
  implicit def ambig1: IsNotProduct[Product] = sys.error("unexpected")
  implicit def ambig2: IsNotProduct[Product] = sys.error("unexpected")
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

sealed trait IsNotEither[-T]
object IsNotEither {
  implicit def valid[T]: IsNotEither[T] = new IsNotEither[T] {}
  implicit def ambig1[S](implicit either: S <:< Either[_, _]): IsNotEither[S] =
    sys.error("unexpected")
  implicit def ambig2[S](implicit either: S <:< Either[_, _]): IsNotEither[S] =
    sys.error("unexpected")
}

sealed trait IsNotList[-T]
object IsNotList {
  implicit def valid[T]: IsNotList[T] = new IsNotList[T] {}
  implicit def ambig1[S]: IsNotList[Iterable[S]] = sys.error("unexpected")
  implicit def ambig2[S]: IsNotList[Iterable[S]] = sys.error("unexpected")
}

sealed trait IsNotRefinedNonEmpty[-T]
object IsNotRefinedNonEmpty {
  implicit def valid[T]: IsNotRefinedNonEmpty[T] =
    new IsNotRefinedNonEmpty[T] {}

  implicit def ambig1[A, L[X] <: Iterable[X], F[_, _]](implicit
    rt: RefType[F]
  ): IsNotRefinedNonEmpty[F[L[A], NonEmpty]] = sys.error("unexpected")
  implicit def ambig2[A, L[X] <: Iterable[X], F[_, _]](implicit
    rt: RefType[F]
  ): IsNotRefinedNonEmpty[F[L[A], NonEmpty]] = sys.error("unexpected")
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
