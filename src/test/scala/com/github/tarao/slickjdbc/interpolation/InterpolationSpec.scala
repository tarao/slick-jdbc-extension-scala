package com.github.tarao
package slickjdbc
package interpolation

import helper.{UnitSpec, TraitSingletonBehavior}
import slick.jdbc.SQLActionBuilder
import slick.profile.SqlAction
import slick.dbio.{NoStream, Effect}
import util.NonEmpty

class InterpolationSpec extends UnitSpec
    with TraitSingletonBehavior {
  def canonicalQuery(query: String) =
    query.replaceAll("[\r\n\t ]+", " ").trim

  def theExactQuery(sql: => SQLActionBuilder)(query: String) {
    val result = sql
    result.queryParts.mkString should equal (query)
  }

  def anIdenticalQuery(sql: => SQLActionBuilder)(query: String) {
    canonicalQuery(sql.queryParts.mkString) should equal (canonicalQuery(query))
  }

  def theExactStatement(sql: => SqlAction[Int, NoStream, Effect])
    (statement: String) {
    val result = sql
    result.statements.mkString should equal (statement)
  }

  def anIdenticalStatement(sql: => SqlAction[Int, NoStream, Effect])
    (statement: String) {
    val expected = canonicalQuery(statement)
    canonicalQuery(sql.statements.mkString) should equal(expected)
  }

  describe("object SQLInterpolation") {
    it("should inherit the trait") {
      it should behave like exportingTheTraitMethods
        [SQLInterpolation](SQLInterpolation)
    }
  }

  describe("SQLInterpolation.sql") {
    it("should not be callable without enabling it") {
      assertTypeError(""" sql"test" """)
    }

    import SQLInterpolation._
    implicit val translators: Traversable[query.Translator] = Seq.empty

    it("should return an action builder with a specified string") {
      it should behave like theExactQuery{ sql"test" }("test")
      it should behave like anIdenticalQuery {
        sql"""
          SELECT * FROM entry
          LIMIT 1
        """
      }("SELECT * FROM entry LIMIT 1")
    }

    it("should embed simple values") {
      val id = 1234
      val url = "http://github.com/tarao/"

      it should behave like anIdenticalQuery {
        sql"""
          SELECT * FROM entry
          WHERE entry_id = $id AND url = $url
        """
      }("SELECT * FROM entry WHERE entry_id = ? AND url = ?")
    }

    it("should embed a literal string with #${}") {
      val tableName = "entry"

      it should behave like anIdenticalQuery {
        sql"""
          SELECT * FROM #${tableName}
          LIMIT 1
        """
      }("SELECT * FROM entry LIMIT 1")
    }

    it("should embed a literal value") {
      val tableName = TableName("entry")

      it should behave like anIdenticalQuery {
        sql"""
          SELECT * FROM ${tableName}
          LIMIT 1
        """
      }("SELECT * FROM entry LIMIT 1")
    }

    it("should embed a custom literal value") {
      val tableName = new Literal { override def toString = "entry" }

      it should behave like anIdenticalQuery {
        sql"""
          SELECT * FROM ${tableName}
          LIMIT 1
        """
      }("SELECT * FROM entry LIMIT 1")
    }

    it("should not embed an undefined value") {
      assertTypeError(""" sql"SELECT * FROM $undefined" """)
    }

    it("should not embed a non-empty list") {
      val entryIds = NonEmpty(1, 2, 3, 4)

      assertTypeError("""
        sql"SELECT * FROM entry WHERE entry_id IN ($entryIds)"
      """)
    }

    it("should embed a non-empty list if it is explicitly enabled ") {
      import ListParameter._

      val entryIds = NonEmpty(1, 2, 3, 4)

      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN ($entryIds)"
      }("SELECT * FROM entry WHERE entry_id IN (?, ?, ?, ?)")

      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN (${NonEmpty(5)})"
      }("SELECT * FROM entry WHERE entry_id IN (?)")
    }

    it("should not embed an option non-empty list") {
      import ListParameter._

      val entryIds = Option(NonEmpty(1, 2, 3, 4))

      assertTypeError("""
        sql"SELECT * FROM entry WHERE entry_id IN ($entryIds)"
      """)

      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN (${entryIds.get})"
      }("SELECT * FROM entry WHERE entry_id IN (?, ?, ?, ?)")
    }

    it("should not embed a maybe-empty list") {
      val entryIds = Seq(1, 2, 3, 4)

      assertTypeError("""
        sql"SELECT * FROM entry WHERE entry_id IN ($entryIds)"
      """)

      import ListParameter._

      assertTypeError("""
        sql"SELECT * FROM entry WHERE entry_id IN ($entryIds)"
      """)
    }

    it("should not embed a user-defined class value") {
      class Foo
      val param = new Foo

      assertTypeError("""
        sql"SELECT * FROM entry WHERE param = $param"
      """
      )
    }

    it("should embed a user-defined class value with custom SetParameter") {
      import slick.jdbc.{SetParameter => SP, PositionedParameters}
      class Foo { override def toString = "foo" }
      implicit object SetFoo extends SP[Foo] {
        def apply(v: Foo, pp: PositionedParameters) {
          pp.setString(v.toString)
        }
      }

      val param = new Foo

      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE param = $param"
      }("SELECT * FROM entry WHERE param = ?")
    }
  }

  describe("SQLInterpolation.sql with query translation") {
    import SQLInterpolation._

    it("should provide a default translation") {
      sql"test" // just compiles
    }

    it("should provide a custom translation") {
      implicit val translators: Traversable[query.Translator] = Seq(
        new query.Translator {
          def apply(q: String, context: query.Context) = q + " translated"
        }
      )
      it should behave like theExactQuery{ sql"test" }("test translated")
    }

    it("should provide a custom ordered translation") {
      implicit val translators: Traversable[query.Translator] = Seq(
        new query.Translator {
          def apply(q: String, context: query.Context) = q + " translated"
        },
        new query.Translator {
          def apply(q: String, context: query.Context) = q + " more"
        }
      )
      it should behave like theExactQuery{ sql"test" }("test translated more")
    }
  }

  describe("SQLInterpolation.sqlu") {
    it("should not be callable without enabling it") {
      assertTypeError(""" sqlu"test" """)
    }

    import SQLInterpolation._
    implicit val translators: Traversable[query.Translator] = Seq.empty

    it("should return an action with a specified string") {
      it should behave like theExactStatement{ sqlu"test" }("test")
      it should behave like anIdenticalStatement {
        sqlu"""
          INSERT INTO entry (entry_id)
          VALUES (1)
        """
      }("INSERT INTO entry (entry_id) VALUES (1)")
    }

    it("should embed simple values") {
      val id = 1234
      val url = "http://github.com/tarao/"

      it should behave like anIdenticalStatement {
        sqlu"""
          INSERT INTO entry (entry_id, url)
          VALUES ($id, $url)
        """
      }("INSERT INTO entry (entry_id, url) VALUES (?, ?)")
    }

    it("should embed a literal string with #${}") {
      val tableName = "entry"

      it should behave like anIdenticalStatement {
        sqlu"""
          INSERT INTO #${tableName} (entry_id)
          VALUES (1)
        """
      }("INSERT INTO entry (entry_id) VALUES (1)")
    }

    it("should embed a literal value") {
      val tableName = TableName("entry")

      it should behave like anIdenticalStatement {
        sqlu"""
          INSERT INTO ${tableName} (entry_id)
          VALUES (1)
        """
      }("INSERT INTO entry (entry_id) VALUES (1)")
    }

    it("should embed a custom literal value") {
      val tableName = new Literal { override def toString = "entry" }

      it should behave like anIdenticalStatement {
        sqlu"""
          INSERT INTO ${tableName} (entry_id)
          VALUES (1)
        """
      }("INSERT INTO entry (entry_id) VALUES (1)")
    }

    it("should not embed an undefined value") {
      assertTypeError("""
        sqlu"INSERT INTO $undefined (entry_id) VALUES (1)"
      """)
    }

    it("should embed a non-empty list if it is explicitly enabled ") {
      import ListParameter._

      val entryIds = NonEmpty(1, 2, 3, 4)

      it should behave like anIdenticalStatement {
        sqlu"""
          UPDATE entry
          SET flag = 1
          WHERE entry_id IN ($entryIds)
        """
      }("UPDATE entry SET flag = 1 WHERE entry_id IN (?, ?, ?, ?)")

      it should behave like anIdenticalStatement {
        sqlu"""
          UPDATE entry
          SET flag = 1
          WHERE entry_id IN (${NonEmpty(5)})
        """
      }("UPDATE entry SET flag = 1 WHERE entry_id IN (?)")
    }

    it("should not embed an option non-empty list") {
      import ListParameter._

      val entryIds = Option(NonEmpty(1, 2, 3, 4))

      assertTypeError("""
        sqlu"UPDATE entry SET flag = 1 WHERE entry_id IN ($entryIds)"
      """)

      it should behave like anIdenticalStatement {
        sqlu"""
          UPDATE entry
          SET flag = 1
          WHERE entry_id IN (${entryIds.get})
        """
      }("UPDATE entry SET flag = 1 WHERE entry_id IN (?, ?, ?, ?)")
    }

    it("should not embed a maybe-empty list") {
      val entryIds = Seq(1, 2, 3, 4)

      assertTypeError("""
        sqlu"UPDATE entry SET flag = 1 WHERE entry_id IN ($entryIds)"
      """)

      import ListParameter._

      assertTypeError("""
        sqlu"UPDATE entry SET flag = 1 WHERE entry_id IN ($entryIds)"
      """)
    }

    it("should not embed a user-defined class value") {
      class Foo
      val param = new Foo

      assertTypeError("""
        sqlu"UPDATE entry SET flag = 1 WHERE param = $param"
      """
      )
    }

    it("should embed a user-defined class value with custom SetParameter") {
      import slick.jdbc.{SetParameter => SP, PositionedParameters}
      class Foo { override def toString = "foo" }
      implicit object SetFoo extends SP[Foo] {
        def apply(v: Foo, pp: PositionedParameters) {
          pp.setString(v.toString)
        }
      }

      val param = new Foo

      it should behave like anIdenticalStatement {
        sqlu"UPDATE entry SET flag = 1 WHERE param = $param"
      }("UPDATE entry SET flag = 1 WHERE param = ?")
    }
  }

  describe("SQLInterpolation.sqlu with query translation") {
    import SQLInterpolation._

    it("should provide a default translation") {
      sqlu"test" // just compiles
    }

    it("should provide a custom translation") {
      implicit val translators: Traversable[query.Translator] = Seq(
        new query.Translator {
          def apply(q: String, context: query.Context) = q + " translated"
        }
      )
      it should behave like theExactStatement{ sqlu"test" }("test translated")
    }

    it("should provide a custom ordered translation") {
      implicit val translators: Traversable[query.Translator] = Seq(
        new query.Translator {
          def apply(q: String, context: query.Context) = q + " translated"
        },
        new query.Translator {
          def apply(q: String, context: query.Context) = q + " more"
        }
      )
      it should behave like theExactStatement{
        sqlu"test"
      }("test translated more")
    }
  }
}

class ListParameterSpec extends UnitSpec
    with TraitSingletonBehavior {
  describe("object ListParameter") {
    it should behave like exportingTheTraitMethods
      [ListParameter](ListParameter)
  }
}
