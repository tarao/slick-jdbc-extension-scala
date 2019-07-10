package com.github.tarao
package slickjdbc

import scala.language.implicitConversions

package object util {
  type NonEmpty[T] = cats.data.NonEmptyList[T]
  object NonEmpty {
    def apply[T](args: T*): NonEmpty[T] = {
      val list = args.toList
      cats.data.NonEmptyList.fromListUnsafe(list)
    }
    implicit def fromTraversable[T](iterable: Iterable[T]): Option[NonEmpty[T]] = {
      cats.data.NonEmptyList.fromList(iterable.toList)
    }
  }
}
