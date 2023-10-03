package com.github.tarao
package slickjdbc
package interpolation

import java.sql.PreparedStatement
import scala.language.implicitConversions
import slick.SlickException
import slick.jdbc.{
  GetResult,
  PositionedParameters,
  PositionedResult,
  SQLInterpolation => SlickInterpolation,
  SetParameter,
  StatementInvoker,
  StreamingInvokerAction,
  TypedParameter
}
import slick.sql.{SqlAction, SqlStreamingAction}
import slick.dbio.{Effect, NoStream}

trait SQLInterpolation {
  implicit def interpolation(s: StringContext): SQLInterpolationImpl = SQLInterpolationImpl(s)
}
object SQLInterpolation extends SQLInterpolation

case class SQLInterpolationImpl(s: StringContext) extends AnyVal {
  import scala.language.experimental.macros

  def sql(params: Any*): SQLActionBuilder =
    macro MacroTreeBuilder.sqlImpl

  def sqlu(params: Any*): SqlAction[Int, NoStream, Effect] =
    macro MacroTreeBuilder.sqluImpl
}

trait Literal
class SimpleString(value: String) extends Literal {
  override def toString = value
}
case class TableName(name: String) extends SimpleString(name)

object GetUpdateValue extends GetResult[Int] {
  def apply(pr: PositionedResult) =
    throw new SlickException("Update statements should not return a ResultSet")
}

case class SQLActionBuilder(strings: Seq[String], params: Seq[TypedParameter[_]]) {
  def as[R](implicit
    getResult: GetResult[R],
    translators: Iterable[query.Translator]
  ): SqlStreamingAction[Vector[R], R, Effect] = {
    val (sql, unitPConv) =
      SlickInterpolation.parse(strings, params.asInstanceOf[Seq[TypedParameter[Any]]])
    val translatedStatements = List(query.Translator.translate(sql))
    new StreamingInvokerAction[Vector[R], R, Effect] {
      def statements = translatedStatements
      protected[this] def createInvoker(statements: Iterable[String]) = new StatementInvoker[R] {
        val getStatement = statements.head
        protected def setParam(st: PreparedStatement) = unitPConv((), new PositionedParameters(st))
        protected def extractValue(rs: PositionedResult): R = getResult(rs)
      }
      protected[this] def createBuilder = Vector.newBuilder[R]
    }
  }

  def asUpdate(implicit
    translators: Iterable[query.Translator]
  ) = as[Int](GetUpdateValue, translators).head
}
