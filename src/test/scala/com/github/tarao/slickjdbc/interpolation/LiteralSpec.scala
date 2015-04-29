package com.github.tarao
package slickjdbc
package interpolation

import helper.UnitSpec

class LiteralSpec extends UnitSpec {
  describe("SimpleString") {
    it("should be a Literal which prints to specified string") {
      val simple = new SimpleString("foo")
      simple shouldBe a [Literal]
      simple.toString should equal ("foo")
    }
  }

  describe("TableName") {
    it("should be a Literal which prints to specified name") {
      val table = new TableName("bar")
      table shouldBe a [Literal]
      table.toString should equal ("bar")
    }

    it("should provide a constructor method") {
      val table = TableName("baz")
      table shouldBe a [Literal]
      table.toString should equal ("baz")
    }
  }

  describe("Placeholders") {
    import util.NonEmpty

    it("should be a Literal which takes a non-empty list") {
      val placeholders = new Placeholders(NonEmpty(1, 2, 3))
      placeholders shouldBe a [Literal]
    }

    it("should prints to '?'s x (list size - 1)") {
      val p1 = new Placeholders(NonEmpty(1, 2, 3))
      p1.toString should equal ("?, ?, ")
      val p2 = new Placeholders(NonEmpty(1, 2, 3, 4, 5))
      p2.toString should equal ("?, ?, ?, ?, ")
      val p3 = new Placeholders(NonEmpty(1))
      p3.toString should equal ("")
    }

    it("should prints to '?'s x (list size) with parenthesis at non-toplevel") {
      val p1 = new Placeholders(NonEmpty(1, 2, 3), false)
      p1.toString should equal ("(?, ?, ?)")
      val p2 = new Placeholders(NonEmpty(1, 2, 3, 4, 5), false)
      p2.toString should equal ("(?, ?, ?, ?, ?)")
      val p3 = new Placeholders(NonEmpty(1), false)
      p3.toString should equal ("(?)")
    }
  }
}
