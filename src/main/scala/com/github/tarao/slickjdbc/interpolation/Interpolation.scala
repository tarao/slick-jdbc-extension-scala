package com.github.tarao
package slickjdbc
package interpolation

import com.github.tarao.nonempty.NonEmpty
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

case class Placeholders(val value: Any, val topLevel: Boolean = true)
    extends Literal {
  def toCompleteString: String = {
    def rec(v: Any) = Placeholders(v, false).toString
    val (single, elements) = value match {
      case l: NonEmpty[Any] => (false, l.map(rec))
      case _ => (true, Iterator.single(new Placeholders(this, false) {
        override def toCompleteString: String = "?"
      }.toString) )
    }
    if (single) elements.toSeq.head else elements.mkString("(", ", ", ")")
  }
  private def stripParen(str: String) = str.stripPrefix("(").stripSuffix(")")
  private def dropLast(str: String) = str.stripSuffix("?")
  override def toString = {
    val s = toCompleteString
    if (topLevel) dropLast(stripParen(s)) else s
  }
}
