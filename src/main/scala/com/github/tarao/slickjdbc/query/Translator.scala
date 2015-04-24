package com.github.tarao
package slickjdbc
package query

import slick.jdbc.SQLActionBuilder

trait Context {
  import java.lang.StackTraceElement

  def caller = Thread.currentThread.getStackTrace.reverse.takeWhile { trace =>
    trace.getClassName != getClass.getName
  }.lastOption getOrElse new StackTraceElement("Unknown", "method", null, -1)
}

trait Translator {
  def apply(query: String, context: Context): String
}

object MarginStripper extends Translator {
  def apply(query: String, context: Context) = query.stripMargin
}

object CallerCommenter extends Translator {
  def apply(query: String, context: Context) =
    new SQLComment(context.caller).embedTo(query)
}

case class SQLComment(comment: Any) {
  import scala.util.matching.Regex

  def escaped = comment.toString.replaceAllLiterally("*/", """*\\/""")

  def embedTo(query: String) =
    query.replaceFirst(" ", Regex.quoteReplacement(s" /* ${escaped} */ "))
}

object Translator extends Context {
  val defaultTranslators = Seq(MarginStripper, CallerCommenter)
  def translate(query: String)(implicit
    translators: Traversable[Translator]
  ) = translators.foldLeft(query) { (q, translate) => translate(q, this) }
  def translateBuilder(builder: SQLActionBuilder)(implicit
    translators: Traversable[Translator]
  ): SQLActionBuilder = {
    val query = builder.queryParts.iterator.map(String.valueOf).mkString
    SQLActionBuilder(Seq(translate(query)), builder.unitPConv)
  }
}
