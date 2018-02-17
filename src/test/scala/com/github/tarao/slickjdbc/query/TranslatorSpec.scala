package com.github.tarao
package slickjdbc
package query

import helper.UnitSpec

class ContextSpec extends UnitSpec {
  describe("Context.caller") {
    it("should return a caller") {
      val context = new Context {}
      context.caller.toString should fullyMatch regex (
        "com[.]github[.]tarao[.]slickjdbc[.]query[.]ContextSpec.*[(]TranslatorSpec[.]scala:[0-9]+[)]"
      )
    }
  }
}

class SQLCommentSpec extends UnitSpec {
  describe("SQLComment") {
    it("should do nothing for a query without whitespace") {
      SQLComment("comment").embedTo("query") should equal ("query")
    }

    it("should insert a comment") {
      SQLComment("comment").embedTo("some query") should equal (
        "some /* comment */ query"
      )
    }

    it("should escape a comment") {
      SQLComment("/* comment */").embedTo("some query") should equal (
        """some /* /* comment *\\/ */ query"""
      )
    }

    it("should insert any comment that can be a string") {
      val obj = new { override def toString = "comment" }
      SQLComment(obj).embedTo("some query") should equal (
        """some /* comment */ query"""
      )
    }
  }
}

class CallerCommenterSpec extends UnitSpec {
  describe("CallerCommenter") {
    it("should embed a caller information as a comment") {
      implicit val translators : Traversable[Translator] = Seq(CallerCommenter)
      val query = Translator.translate("SELECT * FROM entry LIMIT 1")
      val caller = "com[.]github[.]tarao[.]slickjdbc[.]query[.]CallerCommenterSpec.*[(]TranslatorSpec[.]scala:[0-9]+[)]"
      query should fullyMatch regex (
        s"SELECT /[*] $caller [*]/ [*] FROM entry LIMIT 1"
      )
    }
  }
}

class MarginStripperSpec extends UnitSpec {
  describe("MarginStripper") {
    it("should strip margin by '|'") {
      implicit val translators : Traversable[Translator] = Seq(MarginStripper)
      val query = Translator.translate("""
      | SELECT * FROM entry
      | LIMIT 1""")
      query should equal ("\n SELECT * FROM entry\n LIMIT 1")
    }
  }
}

class TranslatorSpec extends UnitSpec {
  describe("Translator") {
    it("should a Context") {
      Translator shouldBe a [Context]
    }

    it("should pass itself as a context") {
      Translator.translate("")(Seq(
        new Translator {
          def apply(query: String, context: Context) = {
            context should equal (Translator)
            query
          }
        }
      ))
    }

    it("should do nothing for empty translators") {
      Translator.translate("foo")(Seq.empty) should equal ("foo")
    }
  }
}
