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
}
