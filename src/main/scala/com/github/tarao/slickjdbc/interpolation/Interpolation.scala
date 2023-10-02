package com.github.tarao
package slickjdbc
package interpolation

import scala.language.implicitConversions
import slick.jdbc.{SQLActionBuilder, SetParameter, TypedParameter}
import slick.sql.SqlAction
import slick.dbio.{Effect, NoStream}

trait SQLInterpolation {
  implicit def interpolation(s: StringContext): SQLInterpolationImpl = SQLInterpolationImpl(s)

  implicit def literalTypeCanBeTypedParameter[A <: Literal](a: A): TypedParameter[A] = {
    val sp = slick.jdbc.SetParameter.SetString.contramap[A](_.toString())
    slick.jdbc.TypedParameter.typedParameter(a)(sp)
  }
  implicit def setParameterCanBeTypedParameter[A : SetParameter](a: A): TypedParameter[A] = {
    slick.jdbc.TypedParameter.typedParameter(a)
  }
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
