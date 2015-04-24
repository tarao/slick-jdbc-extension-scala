package com.github.tarao
package slickjdbc
package interpolation

import scala.language.implicitConversions
import slick.jdbc.SQLActionBuilder
import slick.profile.SqlAction
import slick.dbio.{NoStream, Effect}

trait SQLInterpolation {
  implicit def interpolation(s: StringContext) = SQLInterpolationImpl(s)
}
object SQLInterpolation extends SQLInterpolation

case class SQLInterpolationImpl(s: StringContext) extends AnyVal {
  import scala.language.experimental.macros

  def sql(param: Any*): SQLActionBuilder =
    macro MacroTreeBuilder.sqlImpl
  def sqlu(param: Any*): SqlAction[Int, NoStream, Effect] =
    macro MacroTreeBuilder.sqluImpl
}

trait Literal
class SimpleString(value: String) extends Literal {
  override def toString = value
}
case class TableName(name: String) extends SimpleString(name)
class ListPlaceholders(list: util.NonEmpty[Any]) extends Literal {
  override def toString = "?, " * (list.size-1)
}
