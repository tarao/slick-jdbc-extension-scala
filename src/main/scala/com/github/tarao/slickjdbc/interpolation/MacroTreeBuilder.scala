package com.github.tarao
package slickjdbc
package interpolation

import interpolation.{Literal => LiteralParameter}
import scala.reflect.macros.blackbox.Context

private[interpolation] class MacroTreeBuilder(val c: Context) {
  import c.universe._
  import scala.collection.mutable.ListBuffer
  import slick.sql.SqlAction
  import slick.dbio.{NoStream, Effect}

  def abort(msg: String) = c.abort(c.enclosingPosition, msg)

  // Retrieve string parts of interpolation from caller context
  lazy val rawQueryParts: List[String] = {
    // match SQLInterpolationImpl(StringContext(strArg: _*)).sql(params: _*)
    val Apply(Select(Apply(_, List(Apply(_, strArg))), _), params) =
      c.macroApplication
    strArg map {
      case Literal(Constant(x: String)) => x
      case _ => abort("The interpolation must be a string literal")
    }
  }

  private val NS = q"com.github.tarao.slickjdbc"
  private val interpolation = q"$NS.interpolation"
  private lazy val ListRejected =
    tq"""$interpolation.${TypeName("ListRejected")}"""
  private lazy val OptionRejected =
    tq"""$interpolation.${TypeName("OptionRejected")}"""
  private lazy val EitherRejected =
    tq"""$interpolation.${TypeName("EitherRejected")}"""
  private lazy val ValidParameter =
    tq"""$interpolation.${TypeName("ValidParameter")}"""
  private lazy val ValidProduct =
    tq"""$interpolation.${TypeName("ValidProduct")}"""
  private lazy val ValidRefinedNonEmpty =
    tq"""$interpolation.${TypeName("ValidRefinedNonEmpty")}"""
  private def ensure(required: Type, base: Tree = ValidParameter) =
    q"implicitly[$base[$required]]"
  private val ToPlaceholder =
    tq"""$interpolation.${TypeName("ToPlaceholder")}"""
  private def toPlaceholder(target: Type, base: Tree = ToPlaceholder) =
    q"implicitly[$base[$target]]"

  def invokeInterpolation(param: c.Expr[Any]*): Tree = {
    val stats = new ListBuffer[Tree]

    // Additional features of SQL interpolation by preprocessing
    // string parts and arguments.  The interpolation translates to
    // another (expanded) interpolation call of
    // `ActionBasedSQLInterpolation`.
    //
    // [Embedding literals]
    //
    // A parameter of type `Literal` is embedded as a literal string
    // by using "#" expansion.
    //
    // val literal = new Literal { def toString = "string" }
    // sql"some text with a ${literal} value"
    // ~> SQLInterpolationImpl(
    //      StringContext("some text with a ", " value")
    //    ).sql(literal)
    // ~> ActionBasedSQLInterpolation(
    //      StringContext("some text with a #", " value")
    //    ).sql(literal)
    // => SQLActionBuilder(Seq("some text with a string value"), ...)
    //
    // [Embedding non-empty lists]
    //
    // A parameter of type `NonEmpty[Any]` is embedded with repeated
    // "?"s.  "?"s (except the last one) are inserted as a literal
    // string parameter not as a literal string part of
    // `StringContext` since the number of elements is not known at
    // compile time.
    //
    // val list = NonEmpty(1, 2, 3)
    // sql"some text with a ${list} value"
    // ~> SQLInterpolationImpl(
    //      StringContext("some text with a (#", "", ")#", " value")
    //    ).sql(new Placeholders(list), list, "")
    // ~> ActionBasedSQLInterpolation(
    //      StringContext("some text with a (#", "", ")#", " value")
    //    ).sql(new Placeholders(list), list, "")
    // => ActionBasedSQLInterpolation(
    //      StringContext("some text with a (#", "", ")#", " value")
    //    ).sql("?, ?, ", list, "")
    // => SQLActionBuilder(Seq("some text with a (?, ?, ?) value"), ...)
    //
    // Note that the third "?" is inserted by a conversion of argument
    // `list`.
    val queryParts = new ListBuffer[Tree]
    val params = new ListBuffer[c.Expr[Any]]
    def pushLiteral(literal: String) = {
        params.append(c.Expr(q""" ${""} """))
        queryParts.append(q""" ${literal + "#"} """)
    }
    def pushLiteralValue(expr: c.Expr[Any]) = {
      params.append(c.Expr(q"""slick.jdbc.TypedParameter.typedParameter($expr)(new ${interpolation}.SetLiteral)"""))
    }
    def pushValue(expr: c.Expr[Any]) = {
      params.append(c.Expr(q"""slick.jdbc.TypedParameter.typedParameter($expr)"""))
    }
    def mayCompleteParen(param: c.Expr[Any], s: String)(block: => Unit) = {
      if (!s.matches("""(?s).*\(\s*""")) {
        pushLiteralValue(c.Expr(q""" ${toPlaceholder(param.actualType)}.open """))
        queryParts.append(q""" ${"#"} """)
        block
        pushLiteralValue(c.Expr(q""" ${toPlaceholder(param.actualType)}.close """))
        queryParts.append(q""" ${"#"} """)
      } else block
    }
    param.toList.iterator.zip(rawQueryParts.iterator).foreach { zipped =>
      val (param, s, literal) = zipped match { case (param, s) => {
        val literal = s.reverseIterator.takeWhile(_ == '#').length % 2 == 1
        if (param.actualType <:< typeOf[LiteralParameter])
          (param, s + { if (literal) "" else "#" }, true)
        else (param, s, literal)
      } }
      if (!literal) {
        pushLiteral(s)

        mayCompleteParen(param, s) {
          // for "?, ?, ?, ..." except the last one
          pushLiteralValue(c.Expr(q"""
            ${toPlaceholder(param.actualType)}
              .apply(${param})
              .toTopLevelString
          """))
          queryParts.append(q""" ${"#"} """)

          // for the last "?" (inserted by ActionBasedSQLInterpolation)
          pushValue(param)
          queryParts.append(q""" ${""} """)
        }
      } else {
        pushLiteralValue(param)
        queryParts.append(q"$s")
      }

      if (!literal) {
        // Insert parameter type checker for a fine type error message.

        // The order is significant since there can be a type matches
        // with multiple conditions for example an
        // Option[NonEmpty[Any]] is also a Product.

        stats.append(ensure(param.actualType, ValidRefinedNonEmpty))
        stats.append(ensure(param.actualType, ListRejected))

        param.actualType.foreach { t =>
          if (t <:< typeOf[Any]) {
            stats.append(ensure(t, OptionRejected))
            stats.append(ensure(t, EitherRejected))
          }
        }

        stats.append(ensure(param.actualType, ValidProduct))
        stats.append(ensure(param.actualType, ValidParameter))
      }
    }
    queryParts.append(q"${rawQueryParts.last}")

    stats.append(q"""new $interpolation.SQLActionBuilder(Seq(..$queryParts), Seq(..$params))""")
    q"{ ..$stats }"

  }

  def sqlImpl(params: c.Expr[Any]*): c.Expr[SQLActionBuilder] =
    c.Expr(invokeInterpolation(params: _*))

  def sqluImpl(params: c.Expr[Any]*): c.Expr[SqlAction[Int, NoStream, Effect]] =
    c.Expr(q""" ${invokeInterpolation(params: _*)}.asUpdate """)
}
