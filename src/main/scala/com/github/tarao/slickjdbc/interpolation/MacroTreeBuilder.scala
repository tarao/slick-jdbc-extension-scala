package com.github.tarao
package slickjdbc
package interpolation

import interpolation.{Literal => LiteralParameter}
import scala.reflect.macros.blackbox.Context

private[interpolation] class MacroTreeBuilder(val c: Context) {
  import c.universe._
  import scala.collection.mutable.ListBuffer
  import slick.jdbc.SQLActionBuilder
  import slick.profile.SqlAction
  import slick.dbio.{NoStream, Effect}
  import util.NonEmpty

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
  private lazy val CheckParameter =
    tq"""$interpolation.${TypeName("CheckParameter")}"""
  private lazy val CheckList =
    tq"""$interpolation.${TypeName("CheckList")}"""
  private lazy val CheckNonEmpty =
    tq"""$interpolation.${TypeName("CheckNonEmpty")}"""
  private lazy val CheckOptionNonEmpty =
    tq"""$interpolation.${TypeName("CheckOptionNonEmpty")}"""
  private def checkParameter(required: Type, base: Tree = CheckParameter) =
    q"implicitly[$base[$required]]"
  private val Translators =
    tq"Traversable[$NS.query.Translator]"

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
    //      StringContext("some text with a #", "", " value")
    //    ).sql(new ListPlaceholders(list), list)
    // ~> ActionBasedSQLInterpolation(
    //      StringContext("some text with a #", "", " value")
    //    ).sql(new ListPlaceholders(list), list)
    // => ActionBasedSQLInterpolation(
    //      StringContext("some text with a #", "", " value")
    //    ).sql("?, ?, ", list)
    // => SQLActionBuilder(Seq("some text with a ?, ?, ? value"), ...)
    //
    // Note that the third "?" is be inserted by a conversion of
    // argument `list`.
    val queryParts = new ListBuffer[Tree]
    val params = new ListBuffer[c.Expr[Any]]
    param.toList.iterator.zip(rawQueryParts.iterator).foreach { zipped =>
      val (param, s, literal) = zipped match { case (param, s) => {
        val literal = s.reverseIterator.takeWhile(_ == '#').length % 2 == 1
        if (param.actualType <:< typeOf[LiteralParameter])
          (param, s + { if (literal) "" else "#" }, true)
        else (param, s, literal)
      } }
      if (!literal && param.actualType <:< typeOf[NonEmpty[Any]]) {
        // for "?, ?, ?, ..."
        params.append(c.Expr(q"new $interpolation.ListPlaceholders(${param})"))
        queryParts.append(q""" ${s + "#"} """)
        // for the last "?" (inserted by ActionBasedSQLInterpolation)
        params.append(param)
        queryParts.append(q""" ${""} """)
      } else {
        params.append(param)
        queryParts.append(q"$s")
      }

      // Insert parameter type checker for a fine type error message
      if (!literal) {
        if (param.actualType <:< typeOf[NonEmpty[Any]])
          stats.append(checkParameter(param.actualType, CheckNonEmpty))
        else if (param.actualType <:< typeOf[Option[NonEmpty[Any]]])
          stats.append(checkParameter(param.actualType, CheckOptionNonEmpty))
        else if (param.actualType <:< typeOf[Traversable[Any]])
          stats.append(checkParameter(param.actualType, CheckList))
        else
          stats.append(checkParameter(param.actualType))
      }
    }
    queryParts.append(q"${rawQueryParts.last}")

    // Call the original SQL interpolation of
    // `ActionBasedSQLInterpolation`.  And translate the query string
    // by `SQLActionTranslator`.
    stats.append(q"""
      $NS.query.Translator.translateBuilder(
        new slick.jdbc.ActionBasedSQLInterpolation(
          StringContext(..$queryParts)
        ).sql(..$params)
      )(implicitly[$Translators])
    """)
    q"{ ..$stats }"
  }

  def sqlImpl(param: c.Expr[Any]*): c.Expr[SQLActionBuilder] =
    c.Expr(invokeInterpolation(param: _*))

  def sqluImpl(param: c.Expr[Any]*): c.Expr[SqlAction[Int, NoStream, Effect]] =
    c.Expr(q""" ${invokeInterpolation(param: _*)}.asUpdate """)
}
