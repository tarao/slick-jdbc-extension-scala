package com.github.tarao
package slickjdbc
package interpolation

class Placeholder extends Literal {
  import Placeholder.{stripParen, dropLast}
  def toSeq: Seq[Placeholder] = Seq(this)
  def toTopLevelString: String = dropLast(stripParen(toString))
  override def toString: String = "?"
}
object Placeholder {
  private def stripParen(str: String) = str.stripPrefix("(").stripSuffix(")")
  private def dropLast(str: String) = str.stripSuffix("?")

  def apply(): Placeholder = new Placeholder
  def repeat(n: Int): Seq[Placeholder] = (1 to n).map(_ => apply())

  class Nested(children: Placeholder*) extends Placeholder {
    override def toSeq: Seq[Placeholder] = children
    override def toString: String =
      s"""(${stripParen(children.map(_.toString).mkString(", "))})"""
  }
  object Nested {
    def apply(children: Placeholder*): Nested = new Nested(children: _*)
    def apply(n: Int): Nested = new Nested(Placeholder.repeat(n): _*)
  }
}
