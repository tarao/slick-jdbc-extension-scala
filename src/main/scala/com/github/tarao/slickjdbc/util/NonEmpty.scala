package com.github.tarao
package slickjdbc
package util

import scala.language.implicitConversions

sealed trait NonEmpty[+T] {
  def traversable: Traversable[T]
}
object NonEmpty {
  implicit def fromTraversable[T](t: Traversable[T]): Option[NonEmpty[T]] = {
    if (t.isEmpty) None
    else Some(new NonEmpty[T]{ val traversable = t })
  }

  implicit def toTraversable[T](ne: NonEmpty[T]): Traversable[T] =
    ne.traversable

  def apply[T](head: T, elements: T*): NonEmpty[T] = new NonEmpty[T] {
    val traversable = head +: Seq(elements: _*)
  }
}
