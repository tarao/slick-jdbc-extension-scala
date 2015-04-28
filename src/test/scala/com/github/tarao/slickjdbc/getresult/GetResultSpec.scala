package com.github.tarao
package slickjdbc
package getresult

import scala.reflect.Manifest
import helper.{UnitSpec, TraitSingletonBehavior}
import org.scalamock.scalatest.MockFactory
import java.sql.ResultSet
import slick.jdbc.{GetResult => GR, PositionedResult}

case class OptionedEntry(id: Option[Long], url: Option[String])
case class SimpleEntry(id: Long, url: String)

case class URL(url: String)
object URL {
  implicit def getUrl(implicit str: TypeBinder[String]): TypeBinder[URL] =
    str.map(URL(_))
}
case class UrlEntry(id: Long, url: URL)

trait GetResultBehavior { self: UnitSpec =>
  def positionedResult(rs: ResultSet) =
    new PositionedResult(rs) { def close {} }

  def mappingResult[R : Manifest](rs: ResultSet, expected: R)(implicit
    rconv: GR[R]
  ) {
    val result = rconv(positionedResult(rs).restart)
    result shouldBe a [R]
    result should be (expected)
  }
}

class GetResultSpec extends UnitSpec
    with GetResultBehavior with MockFactory {
  describe("Explicit usage of GetResult") {
    it("should resolve a result by named column") {
      implicit val getEntryResult = GetResult { r => OptionedEntry(
        r.column("entry_id"),
        r.column("url")
      ) }

      val id = 1234L
      val url = "http://github.com/tarao/"

      val rs = mock[ResultSet]
      (rs.getObject(_: String)).expects("entry_id").returning(
        new java.lang.Long(id)
      )
      (rs.getString(_: String)).expects("url").returning(url)

      it should behave like mappingResult(rs, OptionedEntry(
        Option(id),
        Option(url))
      )
    }

    it("should resolve a result by named column with options unwrapped") {
      import AutoUnwrapOption._

      implicit val getEntryResult = GetResult { r => SimpleEntry(
        r.column("entry_id"),
        r.column("url")
      ) }

      val id = 1234L
      val url = "http://github.com/tarao/"

      val rs = mock[ResultSet]
      (rs.getObject(_: String)).expects("entry_id").returning(
        new java.lang.Long(id)
      )
      (rs.getString(_: String)).expects("url").returning(url)

      it should behave like mappingResult(rs, SimpleEntry(id, url))
    }

    it("should resolve a result by named column with a custom type") {
      import AutoUnwrapOption._

      implicit val getEntryResult = GetResult { r => UrlEntry(
        r.column("entry_id"),
        r.column("url")
      ) }

      val id = 1234L
      val url = "http://github.com/tarao/"

      val rs = mock[ResultSet]
      (rs.getObject(_: String)).expects("entry_id").returning(
        new java.lang.Long(id)
      )
      (rs.getString(_: String)).expects("url").returning(url)

      it should behave like mappingResult(rs, UrlEntry(id, URL(url)))
    }

    it("should resolve a result by positioned column") {
      implicit val getEntryResult = GetResult { r => OptionedEntry(
        r.<<,
        r.<<?
      ) }

      val id = 1234L
      val url = "http://github.com/tarao/"

      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(1).returning(
        new java.lang.Long(id)
      )
      (rs.getString(_: Int)).expects(2).returning(url)

      it should behave like mappingResult(rs, OptionedEntry(
        Option(id),
        Option(url))
      )
    }

    it("should resolve a result by positioned column with options unwrapped") {
      import AutoUnwrapOption._

      implicit val getEntryResult = GetResult { r => SimpleEntry(
        r.<<,
        r.skip.<<
      ) }

      val id = 1234L
      val url = "http://github.com/tarao/"

      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(1).returning(
        new java.lang.Long(id)
      )
      (rs.getString(_: Int)).expects(3).returning(url)

      it should behave like mappingResult(rs, SimpleEntry(id, url))
    }

    it("should resolve a result by positioned column with a custom type") {
      import AutoUnwrapOption._

      implicit val getEntryResult = GetResult { r => UrlEntry(
        r.skip.<<,
        r.<<
      ) }

      val id = 1234L
      val url = "http://github.com/tarao/"

      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(2).returning(
        new java.lang.Long(id)
      )
      (rs.getString(_: Int)).expects(3).returning(url)

      it should behave like mappingResult(rs, UrlEntry(id, URL(url)))
    }
  }
}

class GetResultTraitSpec extends UnitSpec
    with GetResult
    with GetResultBehavior with MockFactory {
  describe("Implicit usage of GetResult") {
    it("should resolve a result by named column") {
      implicit val getEntryResult = getResult { OptionedEntry(
        column("entry_id"),
        column("url")
      ) }

      val id = 1234L
      val url = "http://github.com/tarao/"

      val rs = mock[ResultSet]
      (rs.getObject(_: String)).expects("entry_id").returning(
        new java.lang.Long(id)
      )
      (rs.getString(_: String)).expects("url").returning(url)

      it should behave like mappingResult(rs, OptionedEntry(
        Option(id),
        Option(url))
      )
    }

    it("should resolve a result by named column with options unwrapped") {
      import AutoUnwrapOption._

      implicit val getEntryResult = getResult { SimpleEntry(
        column("entry_id"),
        column("url")
      ) }

      val id = 1234L
      val url = "http://github.com/tarao/"

      val rs = mock[ResultSet]
      (rs.getObject(_: String)).expects("entry_id").returning(
        new java.lang.Long(id)
      )
      (rs.getString(_: String)).expects("url").returning(url)

      it should behave like mappingResult(rs, SimpleEntry(id, url))
    }

    it("should resolve a result by named column with a custom type") {
      import AutoUnwrapOption._

      implicit val getEntryResult = getResult { UrlEntry(
        column("entry_id"),
        column("url")
      ) }

      val id = 1234L
      val url = "http://github.com/tarao/"

      val rs = mock[ResultSet]
      (rs.getObject(_: String)).expects("entry_id").returning(
        new java.lang.Long(id)
      )
      (rs.getString(_: String)).expects("url").returning(url)

      it should behave like mappingResult(rs, UrlEntry(id, URL(url)))
    }

    it("should resolve a result by positioned column") {
      implicit val getEntryResult = getResult { OptionedEntry(
        <<,
        <<?
      ) }

      val id = 1234L
      val url = "http://github.com/tarao/"

      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(1).returning(
        new java.lang.Long(id)
      )
      (rs.getString(_: Int)).expects(2).returning(url)

      it should behave like mappingResult(rs, OptionedEntry(
        Option(id),
        Option(url))
      )
    }

    it("should resolve a result by positioned column with options unwrapped") {
      import AutoUnwrapOption._

      implicit val getEntryResult = getResult { SimpleEntry(
        <<,
        skip.<<
      ) }

      val id = 1234L
      val url = "http://github.com/tarao/"

      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(1).returning(
        new java.lang.Long(id)
      )
      (rs.getString(_: Int)).expects(3).returning(url)

      it should behave like mappingResult(rs, SimpleEntry(id, url))
    }

    it("should resolve a result by positioned column with a custom type") {
      import AutoUnwrapOption._

      implicit val getEntryResult = getResult { UrlEntry(
        skip.<<,
        <<
      ) }

      val id = 1234L
      val url = "http://github.com/tarao/"

      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(2).returning(
        new java.lang.Long(id)
      )
      (rs.getString(_: Int)).expects(3).returning(url)

      it should behave like mappingResult(rs, UrlEntry(id, URL(url)))
    }
  }
}
