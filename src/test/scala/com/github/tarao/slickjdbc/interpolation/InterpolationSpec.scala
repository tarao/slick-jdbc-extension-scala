package com.github.tarao
package slickjdbc
package interpolation

import util.NonEmpty
import helper.{UnitSpec, TraitSingletonBehavior}
import slick.jdbc.SQLActionBuilder
import slick.sql.SqlAction
import slick.dbio.{NoStream, Effect}

case class EmptyTuple()
case class Single[T](value: T)
case class Double[S, T](left: S, right: T)
case class Triple[S, T, U](left: S, middle: T, right: U)

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

    it("should embed a non-empty list if it is explicitly enabled") {
      import CompoundParameter._

      val entryIds = NonEmpty(1, 2, 3, 4)

      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN ($entryIds)"
      }("SELECT * FROM entry WHERE entry_id IN (?, ?, ?, ?)")

      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN $entryIds"
      }("SELECT * FROM entry WHERE entry_id IN (?, ?, ?, ?)")

      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN (${NonEmpty(5)})"
      }("SELECT * FROM entry WHERE entry_id IN (?)")

      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN ${NonEmpty(5)}"
      }("SELECT * FROM entry WHERE entry_id IN (?)")
    }

    it("should embed a non-empty set if it is explicitly enabled") {
      import CompoundParameter._

      val entryIds = NonEmpty.fromTraversable(Set(1, 2, 3, 4)).get

      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN ($entryIds)"
      }("SELECT * FROM entry WHERE entry_id IN (?, ?, ?, ?)")

      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN $entryIds"
      }("SELECT * FROM entry WHERE entry_id IN (?, ?, ?, ?)")
    }

    it("should not embed an option non-empty list") {
      import CompoundParameter._

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

      import CompoundParameter._

      assertTypeError("""
        sql"SELECT * FROM entry WHERE entry_id IN ($entryIds)"
      """)
    }

    it("should embed a tuple") {
      val entryIds = (1, 2, 3, 4)

      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN ($entryIds)"
      }("SELECT * FROM entry WHERE entry_id IN (?, ?, ?, ?)")

      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN $entryIds"
      }("SELECT * FROM entry WHERE entry_id IN (?, ?, ?, ?)")
    }

    it("should fail to embed a nullary product") {
      import CompoundParameter._

      val empty = EmptyTuple()
      a [java.sql.SQLException] should be thrownBy
        sql"SELECT * FROM entry WHERE param = $empty"
    }

    it("should not embed a product or a signle tuple") {
      val tuple1 = Tuple1(5)
      val single = Single(5)
      val double = Double(1, 2)
      val triple = Triple(1, 2, 3)

      assertTypeError("""
        sql"SELECT * FROM entry WHERE entry_id IN ($tuple1)"
      """)

      assertTypeError("""
        sql"SELECT * FROM entry WHERE entry_id IN ($single)"
      """)

      assertTypeError("""
        sql"SELECT * FROM entry WHERE entry_id IN ($double)"
      """)

      assertTypeError("""
        sql"SELECT * FROM entry WHERE entry_id IN ($triple)"
      """)
    }

    it("should embed a product or a signle tuple if they are explictly enabled") {
      val entryIds = (1, 2, 3, 4)
      val tuple1 = Tuple1(5)
      val single = Single(5)
      val double = Double(1, 2)
      val triple = Triple(1, 2, 3)

      import CompoundParameter._

      // check if it doesn't break the normal tuple behavior
      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN ($entryIds)"
      }("SELECT * FROM entry WHERE entry_id IN (?, ?, ?, ?)")

      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN $entryIds"
      }("SELECT * FROM entry WHERE entry_id IN (?, ?, ?, ?)")

      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN ($tuple1)"
      }("SELECT * FROM entry WHERE entry_id IN (?)")
      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN $tuple1"
      }("SELECT * FROM entry WHERE entry_id IN (?)")

      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN ($single)"
      }("SELECT * FROM entry WHERE entry_id IN (?)")
      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN $single"
      }("SELECT * FROM entry WHERE entry_id IN (?)")

      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN ($double)"
      }("SELECT * FROM entry WHERE entry_id IN (?, ?)")
      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN $double"
      }("SELECT * FROM entry WHERE entry_id IN (?, ?)")

      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN ($triple)"
      }("SELECT * FROM entry WHERE entry_id IN (?, ?, ?)")
      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN $triple"
      }("SELECT * FROM entry WHERE entry_id IN (?, ?, ?)")
    }

    it("should not embed a non-empty list of product") {
      val params = NonEmpty(
        Single("http://example.com/1"),
        Single("http://example.com/2"),
        Single("http://example.com/3")
      )

      assertTypeError("""
        sql"SELECT * FROM entry WHERE entry_id IN ($params)"
      """)
    }

    it("should embed a non-empty list of product if it is explicitly enabled") {
      // This works as if Single[Int] is an alias of Int
      val params = NonEmpty(
        Single(1),
        Single(2),
        Single(3)
      )

      import CompoundParameter._

      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN ($params)"
      }("SELECT * FROM entry WHERE entry_id IN (?, ?, ?)")

      it should behave like anIdenticalQuery {
        sql"SELECT * FROM entry WHERE entry_id IN $params"
      }("SELECT * FROM entry WHERE entry_id IN (?, ?, ?)")
    }

    it("should not embed an option value") {
      val param = Option(3)
      val params = NonEmpty(Option(3))
      val tuple = (Option(3), 2, 1)

      assertTypeError("""
        sql"SELECT * FROM entry WHERE param = $param"
      """)

      assertTypeError("""
        sql"SELECT * FROM entry WHERE params IN ($params)"
      """)

      assertTypeError("""
        sql"SELECT * FROM entry WHERE params IN ($tuple)"
      """)

      import CompoundParameter._

      assertTypeError("""
        sql"SELECT * FROM entry WHERE param = $param"
      """)

      assertTypeError("""
        sql"SELECT * FROM entry WHERE params IN ($params)"
      """)

      assertTypeError("""
        sql"SELECT * FROM entry WHERE params IN ($tuple)"
      """)
    }

    it("should not embed a user-defined class value") {
      class Foo
      val param = new Foo

      import CompoundParameter._

      assertTypeError("""
        sql"SELECT * FROM entry WHERE param = $param"
      """)
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

    it("should not embed a non-empty list") {
      val entryIds = NonEmpty(1, 2, 3, 4)

      assertTypeError("""
        sqlu"UPDATE entry SET flag = 1 WHERE entry_id IN ($entryIds)"
      """)
    }

    it("should embed a non-empty list if it is explicitly enabled") {
      import CompoundParameter._

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
          WHERE entry_id IN $entryIds
        """
      }("UPDATE entry SET flag = 1 WHERE entry_id IN (?, ?, ?, ?)")

      it should behave like anIdenticalStatement {
        sqlu"""
          UPDATE entry
          SET flag = 1
          WHERE entry_id IN (${NonEmpty(5)})
        """
      }("UPDATE entry SET flag = 1 WHERE entry_id IN (?)")

      it should behave like anIdenticalStatement {
        sqlu"""
          UPDATE entry
          SET flag = 1
          WHERE entry_id IN ${NonEmpty(5)}
        """
      }("UPDATE entry SET flag = 1 WHERE entry_id IN (?)")
    }

    it("should not embed an option non-empty list") {
      import CompoundParameter._

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

      import CompoundParameter._

      assertTypeError("""
        sqlu"UPDATE entry SET flag = 1 WHERE entry_id IN ($entryIds)"
      """)
    }

    it("should embed a tuple") {
      val entryIds = (1, 2, 3, 4)

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
          WHERE entry_id IN $entryIds
        """
      }("UPDATE entry SET flag = 1 WHERE entry_id IN (?, ?, ?, ?)")
    }

    it("should fail to embed a nullary product") {
      import CompoundParameter._

      val empty = EmptyTuple()
      a [java.sql.SQLException] should be thrownBy
        sqlu"UPDATE entry SET flag = 1 WHERE param = $empty"
    }

    it("should not embed a product or a signle tuple") {
      val tuple1 = Tuple1(5)
      val single = Single(5)
      val double = Double(1, 2)
      val triple = Triple(1, 2, 3)

      assertTypeError("""
        sqlu"UPDATE entry SET flag = 1 WHERE entry_id IN ($tuple1)"
      """)

      assertTypeError("""
        sqlu"UPDATE entry SET flag = 1 WHERE entry_id IN ($single)"
      """)

      assertTypeError("""
        sqlu"UPDATE entry SET flag = 1 WHERE entry_id IN ($double)"
      """)

      assertTypeError("""
        sqlu"UPDATE entry SET flag = 1 WHERE entry_id IN ($triple)"
      """)
    }

    it("should embed a product or a signle tuple if they are explictly enabled") {
      val entryIds = (1, 2, 3, 4)
      val tuple1 = Tuple1(5)
      val single = Single(5)
      val double = Double(1, 2)
      val triple = Triple(1, 2, 3)

      import CompoundParameter._

      // check if it doesn't break the normal tuple behavior
      it should behave like anIdenticalStatement {
        sqlu"UPDATE entry SET flag = 1 WHERE entry_id IN ($entryIds)"
      }("UPDATE entry SET flag = 1 WHERE entry_id IN (?, ?, ?, ?)")

      it should behave like anIdenticalStatement {
        sqlu"UPDATE entry SET flag = 1 WHERE entry_id IN $entryIds"
      }("UPDATE entry SET flag = 1 WHERE entry_id IN (?, ?, ?, ?)")

      it should behave like anIdenticalStatement {
        sqlu"UPDATE entry SET flag = 1 WHERE entry_id IN ($tuple1)"
      }("UPDATE entry SET flag = 1 WHERE entry_id IN (?)")
      it should behave like anIdenticalStatement {
        sqlu"UPDATE entry SET flag = 1 WHERE entry_id IN $tuple1"
      }("UPDATE entry SET flag = 1 WHERE entry_id IN (?)")

      it should behave like anIdenticalStatement {
        sqlu"UPDATE entry SET flag = 1 WHERE entry_id IN ($single)"
      }("UPDATE entry SET flag = 1 WHERE entry_id IN (?)")
      it should behave like anIdenticalStatement {
        sqlu"UPDATE entry SET flag = 1 WHERE entry_id IN $single"
      }("UPDATE entry SET flag = 1 WHERE entry_id IN (?)")

      it should behave like anIdenticalStatement {
        sqlu"UPDATE entry SET flag = 1 WHERE entry_id IN ($double)"
      }("UPDATE entry SET flag = 1 WHERE entry_id IN (?, ?)")
      it should behave like anIdenticalStatement {
        sqlu"UPDATE entry SET flag = 1 WHERE entry_id IN $double"
      }("UPDATE entry SET flag = 1 WHERE entry_id IN (?, ?)")

      it should behave like anIdenticalStatement {
        sqlu"UPDATE entry SET flag = 1 WHERE entry_id IN ($triple)"
      }("UPDATE entry SET flag = 1 WHERE entry_id IN (?, ?, ?)")
      it should behave like anIdenticalStatement {
        sqlu"UPDATE entry SET flag = 1 WHERE entry_id IN $triple"
      }("UPDATE entry SET flag = 1 WHERE entry_id IN (?, ?, ?)")
    }

    it("should not embed a non-empty list of product") {
      val params1 = NonEmpty(
        (1, "http://example.com/1"),
        (2, "http://example.com/2"),
        (3, "http://example.com/3")
      )

      val params2 = NonEmpty(
        Tuple1("http://example.com/1"),
        Tuple1("http://example.com/2"),
        Tuple1("http://example.com/3")
      )

      val params3 = NonEmpty(
        Single("http://example.com/1"),
        Single("http://example.com/2"),
        Single("http://example.com/3")
      )

      val params4 = NonEmpty(
        Double(1, "http://example.com/1"),
        Double(2, "http://example.com/2"),
        Double(3, "http://example.com/3")
      )

      assertTypeError("""
        sqlu"INSERT INTO entry (entry_id, url) VALUES ($params1)"
      """)

      assertTypeError("""
        sqlu"INSERT INTO entry (url) VALUES ($params2)"
      """)

      assertTypeError("""
        sqlu"UPDATE entry SET flag = 1 WHERE entry_id IN ($params3)"
      """)

      assertTypeError("""
        sqlu"INSERT INTO entry (entry_id, url) VALUES ($params4)"
      """)
    }

    it("should embed a non-empty list of product if it is explicitly enabled") {
      val params1 = NonEmpty(
        (1, "http://example.com/1"),
        (2, "http://example.com/2"),
        (3, "http://example.com/3")
      )

      val params2 = NonEmpty(
        Tuple1("http://example.com/1"),
        Tuple1("http://example.com/2"),
        Tuple1("http://example.com/3")
      )

      // This works as if Single[String] is an alias of String
      val params3 = NonEmpty(
        Single("http://example.com/1"),
        Single("http://example.com/2"),
        Single("http://example.com/3")
      )

      val params4 = NonEmpty(
        Double(1, "http://example.com/1"),
        Double(2, "http://example.com/2"),
        Double(3, "http://example.com/3")
      )

      import CompoundParameter._

      it should behave like anIdenticalStatement {
        sqlu"INSERT INTO entry (entry_id, url) VALUES ($params1)"
      }("INSERT INTO entry (entry_id, url) VALUES (?, ?), (?, ?), (?, ?)")

      it should behave like anIdenticalStatement {
        sqlu"INSERT INTO entry (entry_id, url) VALUES $params1"
      }("INSERT INTO entry (entry_id, url) VALUES (?, ?), (?, ?), (?, ?)")

      it should behave like anIdenticalStatement {
        sqlu"INSERT INTO entry (url) VALUES ($params2)"
      }("INSERT INTO entry (url) VALUES (?), (?), (?)")

      it should behave like anIdenticalStatement {
        sqlu"INSERT INTO entry (url) VALUES $params2"
      }("INSERT INTO entry (url) VALUES (?), (?), (?)")

      it should behave like anIdenticalStatement {
        sqlu"UPDATE entry SET flag = 1 WHERE entry_id IN ($params3)"
      }("UPDATE entry SET flag = 1 WHERE entry_id IN (?, ?, ?)")

      it should behave like anIdenticalStatement {
        sqlu"UPDATE entry SET flag = 1 WHERE entry_id IN $params3"
      }("UPDATE entry SET flag = 1 WHERE entry_id IN (?, ?, ?)")

      it should behave like anIdenticalStatement {
        sqlu"INSERT INTO entry (entry_id, url) VALUES ($params4)"
      }("INSERT INTO entry (entry_id, url) VALUES (?, ?), (?, ?), (?, ?)")

      it should behave like anIdenticalStatement {
        sqlu"INSERT INTO entry (entry_id, url) VALUES $params4"
      }("INSERT INTO entry (entry_id, url) VALUES (?, ?), (?, ?), (?, ?)")
    }

    it("should not embed an option value") {
      val param = Option(3)
      val params = NonEmpty(Option(3))
      val tuple = (Option(3), 2, 1)

      assertTypeError("""
        sqlu"UPDATE entry SET flag = 1 WHERE param = $param"
      """)

      assertTypeError("""
        sqlu"UPDATE entry SET flag = 1 WHERE params IN ($params)"
      """)

      assertTypeError("""
        sqlu"UPDATE entry SET flag = 1 WHERE params IN ($tuple)"
      """)

      import CompoundParameter._

      assertTypeError("""
        sqlu"UPDATE entry SET flag = 1 WHERE param = $param"
      """)

      assertTypeError("""
        sqlu"UPDATE entry SET flag = 1 WHERE params IN ($params)"
      """)

      assertTypeError("""
        sqlu"UPDATE entry SET flag = 1 WHERE params IN ($tuple)"
      """)
    }

    it("should not embed a user-defined class value") {
      class Foo
      val param = new Foo

      import CompoundParameter._

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

class CompoundParameterSpec extends UnitSpec
    with TraitSingletonBehavior {
  describe("object ListParameter") {
    it("should inherit the trait") {
      it should behave like exportingTheTraitMethods
        [ListParameter](ListParameter)
    }
  }

  describe("object ProductParameter") {
    it("should inherit the trait") {
      it should behave like exportingTheTraitMethods
        [ProductParameter](ProductParameter)
    }
  }

  describe("object CompoundParameter") {
    it("should inherit the trait") {
      val compoundParameter = new CompoundParameter {}
      compoundParameter shouldBe a[ListParameter]
      compoundParameter shouldBe a[ProductParameter]

      CompoundParameter shouldBe a[ListParameter]
      CompoundParameter shouldBe a[ProductParameter]
    }
  }
}
