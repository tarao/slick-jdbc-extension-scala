package com.github.tarao
package slickjdbc
package interpolation

import scala.annotation.implicitNotFound
import slick.jdbc.{SetParameter => SP, PositionedParameters}
import util.NonEmpty

trait ListParameter {
  @inline implicit
  def createSetList[T](implicit c: SP[T]): SetListParameter[T, NonEmpty[T]] =
    new SetListParameter[T, NonEmpty[T]](c)
}
object ListParameter extends ListParameter

/** SetParameter for non-empty list types. */
class SetListParameter[S, -T <: NonEmpty[S]](val c: SP[S]) extends SP[T] {
  def apply(param: T, pp: PositionedParameters): Unit = {
    param.foreach { item => c.asInstanceOf[SP[Any]](item, pp) }
  }
}

@implicitNotFound(msg = "Unsupported parameter type: ${T}.\n" +
  "[NOTE] You need an implicit of slick.jdbc.SetParameter[${T}] to pass a value of the type.")
sealed trait CheckParameter[-T]
object CheckParameter {
  implicit def valid[T](implicit c: SP[T]): CheckParameter[T] =
    new CheckParameter[T] {}
}

@implicitNotFound(msg = "Illegal parameter type: ${T}.\n" +
  "[NOTE] A list is not allowed since it may be empty and breaks the query.\n" +
  "[NOTE] Pass a util.NonEmpty[] if you know that it is not empty.")
sealed trait CheckListParameter[-T]
object CheckListParameter {
  implicit def valid[T](implicit c: SP[T]): CheckListParameter[T] =
    new CheckListParameter[T] {}
}

@implicitNotFound(msg = "Non-empty list is passed.\n" +
  "[NOTE] Use interpolation.ListParameter trait to enable passing a non-empty list.")
sealed trait CheckNonEmptyParameter[-T]
object CheckNonEmptyParameter {
  implicit def valid[T](implicit c: SP[T]): CheckNonEmptyParameter[T] =
    new CheckNonEmptyParameter[T] {}
}

@implicitNotFound(msg = "Maybe-non-empty list is passed.\n" +
  "[NOTE] Break it into Some(_) or None to confirm that it is not empty.")
sealed trait CheckOptionNonEmptyParameter[-T]
object CheckOptionNonEmptyParameter {
  implicit def valid[T](implicit c: SP[T]): CheckOptionNonEmptyParameter[T] =
    new CheckOptionNonEmptyParameter[T] {}
}
