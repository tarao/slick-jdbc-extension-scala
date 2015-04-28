package com.github.tarao
package slickjdbc
package getresult

import scala.language.implicitConversions
import helper.{UnitSpec, TraitSingletonBehavior}
import org.scalamock.scalatest.MockFactory
import java.sql.ResultSet
import java.io.{
  InputStream,
  ByteArrayInputStream,
  InputStreamReader,
  BufferedReader,
  Reader
}

class TypeBinderSpec extends UnitSpec with MockFactory {
  def column[T](rs: ResultSet, index: Int, expected: T)(implicit
    binder: TypeBinder[T]
  ) { binder.apply(rs, index) should be (expected) }
  def column[T](rs: ResultSet, field: String, expected: T)(implicit
    binder: TypeBinder[T]
  ) { binder.apply(rs, field) should be (expected) }
  def throwingFromColumn[T](rs: ResultSet, index: Int)(implicit
    binder: TypeBinder[T]
  ) { a [NoSuchElementException] should be thrownBy binder.apply(rs, index) }
  def throwingFromColumn[T](rs: ResultSet, field: String)(implicit
    binder: TypeBinder[T]
  ) { a [NoSuchElementException] should be thrownBy binder.apply(rs, field) }

  describe("TypeBinder[String]") {
    it("should be able to get a String value") {
      val rs = mock[ResultSet]
      (rs.getString(_: Int)).expects(1).twice.returning("foo bar")
      (rs.getString(_: String)).expects("column1").twice.returning("foo bar")

      it should behave like column(rs, 1, Option("foo bar"))
      it should behave like column(rs, "column1", Option("foo bar"))

      assertTypeError("""
        it should behave like column(rs, 1, "foo bar")
      """)
      assertTypeError("""
        it should behave like column(rs, "column1", "foo bar")
      """)

      import AutoUnwrapOption._

      it should behave like column(rs, 1, "foo bar")
      it should behave like column(rs, "column1", "foo bar")
    }

    it("should not be able to get a String from null") {
      val rs = mock[ResultSet]
      (rs.getString(_: Int)).expects(0).twice.returning(null)
      (rs.getString(_: String)).expects("null").twice.returning(null)

      it should behave like column[Option[String]](rs, 0, None)
      it should behave like column[Option[String]](rs, "null", None)

      import AutoUnwrapOption._

      it should behave like throwingFromColumn[String](rs, 0)
      it should behave like throwingFromColumn[String](rs, "null")
    }
  }

  describe("TypeBinder[BigDecimal]") {
    it("should be able to get a BigDecimal value") {
      val rs = mock[ResultSet]
      (rs.getBigDecimal(_: Int)).expects(1).repeat(2).returning(
        new java.math.BigDecimal("1234567")
      )
      (rs.getBigDecimal(_: String)).expects("column1").repeat(2).returning(
        new java.math.BigDecimal("1234567")
      )
      (rs.getBigDecimal(_: Int)).expects(2).repeat(2).returning(
        new java.math.BigDecimal("12345678901234567")
      )
      (rs.getBigDecimal(_: String)).expects("column2").repeat(2).returning(
        new java.math.BigDecimal("12345678901234567")
      )

      it should behave like column(rs, 1, Option(BigDecimal("1234567")))
      it should behave like column(rs, "column1", Option(BigDecimal("1234567")))

      it should behave like
        column(rs, 2, Option(BigDecimal("12345678901234567")))
      it should behave like
        column(rs, "column2", Option(BigDecimal("12345678901234567")))

      assertTypeError("""
        it should behave like column(rs, 1, BigDecimal("1234567"))
      """)
      assertTypeError("""
        it should behave like column(rs, "column1", BigDecimal("1234567"))
      """)

      import AutoUnwrapOption._

      it should behave like column(rs, 1, BigDecimal("1234567"))
      it should behave like column(rs, "column1", BigDecimal("1234567"))

      it should behave like
        column(rs, 2, BigDecimal("12345678901234567"))
      it should behave like
        column(rs, "column2", BigDecimal("12345678901234567"))
    }

    it("should not be able to get a BigDecimal from null") {
      val rs = mock[ResultSet]
      (rs.getBigDecimal(_: Int)).expects(0).repeat(2).returning(null)
      (rs.getBigDecimal(_: String)).expects("null").repeat(2).returning(null)

      it should behave like column[Option[BigDecimal]](rs, 0, None)
      it should behave like column[Option[BigDecimal]](rs, "null", None)

      import AutoUnwrapOption._

      it should behave like throwingFromColumn[BigDecimal](rs, 0)
      it should behave like throwingFromColumn[BigDecimal](rs, "null")
    }
  }

  describe("TypeBinder[Boolean]") {
    it("should be able to get a Boolean value") {
      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(1).repeat(2).returning(
        new java.lang.Boolean(true)
      )
      (rs.getObject(_: String)).expects("column1").repeat(2).returning(
        new java.lang.Boolean(true)
      )
      (rs.getObject(_: Int)).expects(2).repeat(2).returning(
        new java.lang.Boolean(false)
      )
      (rs.getObject(_: String)).expects("column2").repeat(2).returning(
        new java.lang.Boolean(false)
      )
      (rs.getObject(_: Int)).expects(3).repeat(2).returning(
        java.math.BigDecimal.ONE
      )
      (rs.getObject(_: String)).expects("column3").repeat(2).returning(
        java.math.BigDecimal.ONE
      )
      (rs.getObject(_: Int)).expects(4).repeat(2).returning(
        java.math.BigDecimal.ZERO
      )
      (rs.getObject(_: String)).expects("column4").repeat(2).returning(
        java.math.BigDecimal.ZERO
      )
      (rs.getObject(_: Int)).expects(5).repeat(2).returning(
        new java.lang.Integer(1)
      )
      (rs.getObject(_: String)).expects("column5").repeat(2).returning(
        new java.lang.Integer(1)
      )
      (rs.getObject(_: Int)).expects(6).repeat(2).returning(
        new java.lang.Integer(0)
      )
      (rs.getObject(_: String)).expects("column6").repeat(2).returning(
        new java.lang.Integer(0)
      )
      (rs.getObject(_: Int)).expects(7).repeat(2).returning(
        new java.lang.Float(1.0)
      )
      (rs.getObject(_: String)).expects("column7").repeat(2).returning(
        new java.lang.Float(1.0)
      )
      (rs.getObject(_: Int)).expects(8).repeat(2).returning(
        new java.lang.Float(0.0)
      )
      (rs.getObject(_: String)).expects("column8").repeat(2).returning(
        new java.lang.Float(0.0)
      )
      (rs.getObject(_: Int)).expects(9).repeat(2).returning(
        new java.lang.String("1")
      )
      (rs.getObject(_: String)).expects("column9").repeat(2).returning(
        new java.lang.String("1")
      )
      (rs.getObject(_: Int)).expects(10).repeat(2).returning(
        new java.lang.String("0")
      )
      (rs.getObject(_: String)).expects("column10").repeat(2).returning(
        new java.lang.String("0")
      )
      (rs.getObject(_: Int)).expects(11).repeat(2).returning(
        new java.lang.String("hoge")
      )
      (rs.getObject(_: String)).expects("column11").repeat(2).returning(
        new java.lang.String("hoge")
      )
      (rs.getObject(_: Int)).expects(12).repeat(2).returning(
        new java.lang.String("")
      )
      (rs.getObject(_: String)).expects("column12").repeat(2).returning(
        new java.lang.String("")
      )

      val N = 12

      for (i <- 1 to N) {
        val b = i % 2 != 0
        it should behave like column(rs, i, Option(b))
        it should behave like column(rs, "column"+i, Option(b))
      }

      assertTypeError("""
        it should behave like column(rs, 1, true)
      """)
      assertTypeError("""
        it should behave like column(rs, "column1", true)
      """)

      import AutoUnwrapOption._

      for (i <- 1 to N) {
        val b = i % 2 != 0
        it should behave like column(rs, i, b)
        it should behave like column(rs, "column"+i, b)
      }
    }

    it("should not be able to get a Boolean value from null") {
      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(0).repeat(2).returning(null)
      (rs.getObject(_: String)).expects("null").repeat(2).returning(null)

      it should behave like column[Option[Boolean]](rs, 0, None)
      it should behave like column[Option[Boolean]](rs, "null", None)

      import AutoUnwrapOption._

      it should behave like throwingFromColumn[Boolean](rs, 0)
      it should behave like throwingFromColumn[Boolean](rs, "null")
    }
  }

  describe("TypeBinder[Byte]") {
    it("should be able to get a Byte value") {
      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(1).repeat(2).returning(
        new java.lang.Byte(12.toByte)
      )
      (rs.getObject(_: String)).expects("column1").repeat(2).returning(
        new java.lang.Byte(12.toByte)
      )
      (rs.getObject(_: Int)).expects(2).repeat(2).returning(
        new java.lang.Byte(224.toByte)
      )
      (rs.getObject(_: String)).expects("column2").repeat(2).returning(
        new java.lang.Byte(224.toByte)
      )
      (rs.getObject(_: Int)).expects(3).repeat(2).returning(
        new java.lang.Integer(1281)
      )
      (rs.getObject(_: String)).expects("column3").repeat(2).returning(
        new java.lang.Integer(1281)
      )
      (rs.getObject(_: Int)).expects(4).repeat(2).returning(
        new java.lang.Integer(-1281)
      )
      (rs.getObject(_: String)).expects("column4").repeat(2).returning(
        new java.lang.Integer(-1281)
      )
      (rs.getObject(_: Int)).expects(5).repeat(2).returning("12")
      (rs.getObject(_: String)).expects("column5").repeat(2).returning("12")
      (rs.getObject(_: Int)).expects(6).repeat(2).returning("-3")
      (rs.getObject(_: String)).expects("column6").repeat(2).returning("-3")
      (rs.getObject(_: Int)).expects(7).repeat(2).returning("010")
      (rs.getObject(_: String)).expects("column7").repeat(2).returning("010")

      it should behave like column(rs, 1, Option(12.toByte))
      it should behave like column(rs, "column1", Option(12.toByte))
      it should behave like column(rs, 2, Option(224.toByte))
      it should behave like column(rs, "column2", Option(224.toByte))
      it should behave like column(rs, 3, Option(1.toByte))
      it should behave like column(rs, "column3", Option(1.toByte))
      it should behave like column(rs, 4, Option(255.toByte))
      it should behave like column(rs, "column4", Option(255.toByte))
      it should behave like column(rs, 5, Option(12.toByte))
      it should behave like column(rs, "column5", Option(12.toByte))
      it should behave like column(rs, 6, Option(-3.toByte))
      it should behave like column(rs, "column6", Option(-3.toByte))
      it should behave like column(rs, 7, Option(10.toByte))
      it should behave like column(rs, "column7", Option(10.toByte))

      assertTypeError("""
        it should behave like column(rs, 1, 12.toByte)
      """)
      assertTypeError("""
        it should behave like column(rs, "column1", 12.toByte)
      """)

      import AutoUnwrapOption._

      it should behave like column(rs, 1, 12.toByte)
      it should behave like column(rs, "column1", 12.toByte)
      it should behave like column(rs, 2, 224.toByte)
      it should behave like column(rs, "column2", 224.toByte)
      it should behave like column(rs, 3, 1.toByte)
      it should behave like column(rs, "column3", 1.toByte)
      it should behave like column(rs, 4, 255.toByte)
      it should behave like column(rs, "column4", 255.toByte)
      it should behave like column(rs, 5, 12.toByte)
      it should behave like column(rs, "column5", 12.toByte)
      it should behave like column(rs, 6, -3.toByte)
      it should behave like column(rs, "column6", -3.toByte)
      it should behave like column(rs, 7, 10.toByte)
      it should behave like column(rs, "column7", 10.toByte)
    }

    it("should not be able to get a Byte value from an invalid rep.") {
      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(1).repeat(2).returning("")
      (rs.getObject(_: String)).expects("column1").repeat(2).returning("")
      (rs.getObject(_: Int)).expects(2).repeat(2).returning("1281")
      (rs.getObject(_: String)).expects("column2").repeat(2).returning("1281")
      (rs.getObject(_: Int)).expects(3).repeat(2).returning("foo")
      (rs.getObject(_: String)).expects("column3").repeat(2).returning("foo")

      val N = 3

      for (i <- 1 to N) {
        it should behave like column[Option[Byte]](rs, i, None)
        it should behave like column[Option[Byte]](rs, "column"+i, None)
      }

      import AutoUnwrapOption._

      for (i <- 1 to N) {
        it should behave like throwingFromColumn[Byte](rs, i)
        it should behave like throwingFromColumn[Byte](rs, "column"+i)
      }
    }

    it("should not be able to get a Byte from null") {
      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(0).repeat(2).returning(null)
      (rs.getObject(_: String)).expects("null").repeat(2).returning(null)

      it should behave like column[Option[Byte]](rs, 0, None)
      it should behave like column[Option[Byte]](rs, "null", None)

      import AutoUnwrapOption._

      it should behave like throwingFromColumn[Byte](rs, 0)
      it should behave like throwingFromColumn[Byte](rs, "null")
    }
  }

  describe("TypeBinder[Short]") {
    it("should be able to get a Short value") {
      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(1).repeat(2).returning(
        new java.lang.Short(12.toShort)
      )
      (rs.getObject(_: String)).expects("column1").repeat(2).returning(
        new java.lang.Short(12.toShort)
      )
      (rs.getObject(_: Int)).expects(2).repeat(2).returning(
        new java.lang.Short(38000.toShort)
      )
      (rs.getObject(_: String)).expects("column2").repeat(2).returning(
        new java.lang.Short(38000.toShort)
      )
      (rs.getObject(_: Int)).expects(3).repeat(2).returning(
        new java.lang.Integer(129780)
      )
      (rs.getObject(_: String)).expects("column3").repeat(2).returning(
        new java.lang.Integer(129780)
      )
      (rs.getObject(_: Int)).expects(4).repeat(2).returning(
        new java.lang.Integer(-129781)
      )
      (rs.getObject(_: String)).expects("column4").repeat(2).returning(
        new java.lang.Integer(-129781)
      )
      (rs.getObject(_: Int)).expects(5).repeat(2).returning("12")
      (rs.getObject(_: String)).expects("column5").repeat(2).returning("12")
      (rs.getObject(_: Int)).expects(6).repeat(2).returning("-3")
      (rs.getObject(_: String)).expects("column6").repeat(2).returning("-3")
      (rs.getObject(_: Int)).expects(7).repeat(2).returning("010")
      (rs.getObject(_: String)).expects("column7").repeat(2).returning("010")

      it should behave like column(rs, 1, Option(12.toShort))
      it should behave like column(rs, "column1", Option(12.toShort))
      it should behave like column(rs, 2, Option(38000.toShort))
      it should behave like column(rs, "column2", Option(38000.toShort))
      it should behave like column(rs, 3, Option(64244.toShort))
      it should behave like column(rs, "column3", Option(64244.toShort))
      it should behave like column(rs, 4, Option(1291.toShort))
      it should behave like column(rs, "column4", Option(1291.toShort))
      it should behave like column(rs, 5, Option(12.toShort))
      it should behave like column(rs, "column5", Option(12.toShort))
      it should behave like column(rs, 6, Option(-3.toShort))
      it should behave like column(rs, "column6", Option(-3.toShort))
      it should behave like column(rs, 7, Option(10.toShort))
      it should behave like column(rs, "column7", Option(10.toShort))

      assertTypeError("""
        it should behave like column(rs, 1, 12.toShort)
      """)
      assertTypeError("""
        it should behave like column(rs, "column1", 12.toShort)
      """)

      import AutoUnwrapOption._

      it should behave like column(rs, 1, 12.toShort)
      it should behave like column(rs, "column1", 12.toShort)
      it should behave like column(rs, 2, 38000.toShort)
      it should behave like column(rs, "column2", 38000.toShort)
      it should behave like column(rs, 3, 64244.toShort)
      it should behave like column(rs, "column3", 64244.toShort)
      it should behave like column(rs, 4, 1291.toShort)
      it should behave like column(rs, "column4", 1291.toShort)
      it should behave like column(rs, 5, 12.toShort)
      it should behave like column(rs, "column5", 12.toShort)
      it should behave like column(rs, 6, -3.toShort)
      it should behave like column(rs, "column6", -3.toShort)
      it should behave like column(rs, 7, 10.toShort)
      it should behave like column(rs, "column7", 10.toShort)
    }

    it("should not be able to get a Short value from an invalid rep.") {
      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(1).repeat(2).returning("")
      (rs.getObject(_: String)).expects("column1").repeat(2).returning("")
      (rs.getObject(_: Int)).expects(2).repeat(2).returning("38000")
      (rs.getObject(_: String)).expects("column2").repeat(2).returning("38000")
      (rs.getObject(_: Int)).expects(3).repeat(2).returning("foo")
      (rs.getObject(_: String)).expects("column3").repeat(2).returning("foo")

      val N = 3

      for (i <- 1 to N) {
        it should behave like column[Option[Short]](rs, i, None)
        it should behave like column[Option[Short]](rs, "column"+i, None)
      }

      import AutoUnwrapOption._

      for (i <- 1 to N) {
        it should behave like throwingFromColumn[Short](rs, i)
        it should behave like throwingFromColumn[Short](rs, "column"+i)
      }
    }

    it("should not be able to get a Short from null") {
      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(0).repeat(2).returning(null)
      (rs.getObject(_: String)).expects("null").repeat(2).returning(null)

      it should behave like column[Option[Short]](rs, 0, None)
      it should behave like column[Option[Short]](rs, "null", None)

      import AutoUnwrapOption._

      it should behave like throwingFromColumn[Short](rs, 0)
      it should behave like throwingFromColumn[Short](rs, "null")
    }
  }

  describe("TypeBinder[Int]") {
    it("should be able to get a Int value") {
      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(1).repeat(2).returning(
        new java.lang.Integer(12)
      )
      (rs.getObject(_: String)).expects("column1").repeat(2).returning(
        new java.lang.Integer(12)
      )
      (rs.getObject(_: Int)).expects(2).repeat(2).returning(
        new java.lang.Integer(3000000000L.toInt)
      )
      (rs.getObject(_: String)).expects("column2").repeat(2).returning(
        new java.lang.Integer(3000000000L.toInt)
      )
      (rs.getObject(_: Int)).expects(3).repeat(2).returning("12")
      (rs.getObject(_: String)).expects("column3").repeat(2).returning("12")
      (rs.getObject(_: Int)).expects(4).repeat(2).returning("-3")
      (rs.getObject(_: String)).expects("column4").repeat(2).returning("-3")
      (rs.getObject(_: Int)).expects(5).repeat(2).returning("010")
      (rs.getObject(_: String)).expects("column5").repeat(2).returning("010")

      it should behave like column(rs, 1, Option(12))
      it should behave like column(rs, "column1", Option(12))
      it should behave like column(rs, 2, Option(3000000000L.toInt))
      it should behave like column(rs, "column2", Option(3000000000L.toInt))
      it should behave like column(rs, 3, Option(12))
      it should behave like column(rs, "column3", Option(12))
      it should behave like column(rs, 4, Option(-3))
      it should behave like column(rs, "column4", Option(-3))
      it should behave like column(rs, 5, Option(10))
      it should behave like column(rs, "column5", Option(10))

      assertTypeError("""
        it should behave like column(rs, 1, 12)
      """)
      assertTypeError("""
        it should behave like column(rs, "column1", 12)
      """)

      import AutoUnwrapOption._

      it should behave like column(rs, 1, 12)
      it should behave like column(rs, "column1", 12)
      it should behave like column(rs, 2, 3000000000L.toInt)
      it should behave like column(rs, "column2", 3000000000L.toInt)
      it should behave like column(rs, 3, 12)
      it should behave like column(rs, "column3", 12)
      it should behave like column(rs, 4, -3)
      it should behave like column(rs, "column4", -3)
      it should behave like column(rs, 5, 10)
      it should behave like column(rs, "column5", 10)
    }

    it("should not be able to get a Int value from an invalid rep.") {
      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(1).repeat(2).returning("")
      (rs.getObject(_: String)).expects("column1").repeat(2).returning("")
      (rs.getObject(_: Int)).expects(2).repeat(2).returning("6000000000")
      (rs.getObject(_: String)).expects("column2").repeat(2).returning("6000000000")
      (rs.getObject(_: Int)).expects(3).repeat(2).returning("foo")
      (rs.getObject(_: String)).expects("column3").repeat(2).returning("foo")

      val N = 3

      for (i <- 1 to N) {
        it should behave like column[Option[Int]](rs, i, None)
        it should behave like column[Option[Int]](rs, "column"+i, None)
      }

      import AutoUnwrapOption._

      for (i <- 1 to N) {
        it should behave like throwingFromColumn[Int](rs, i)
        it should behave like throwingFromColumn[Int](rs, "column"+i)
      }
    }

    it("should not be able to get a Int from null") {
      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(0).repeat(2).returning(null)
      (rs.getObject(_: String)).expects("null").repeat(2).returning(null)

      it should behave like column[Option[Int]](rs, 0, None)
      it should behave like column[Option[Int]](rs, "null", None)

      import AutoUnwrapOption._

      it should behave like throwingFromColumn[Int](rs, 0)
      it should behave like throwingFromColumn[Int](rs, "null")
    }
  }

  describe("TypeBinder[Long]") {
    it("should be able to get a Long value") {
      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(1).repeat(2).returning(
        new java.lang.Long(12)
      )
      (rs.getObject(_: String)).expects("column1").repeat(2).returning(
        new java.lang.Long(12)
      )
      (rs.getObject(_: Int)).expects(2).repeat(2).returning(
        new java.lang.Long(6000000000L)
      )
      (rs.getObject(_: String)).expects("column2").repeat(2).returning(
        new java.lang.Long(6000000000L)
      )
      (rs.getObject(_: Int)).expects(3).repeat(2).returning(
        new java.math.BigInteger("1"+"0"*19)
      )
      (rs.getObject(_: String)).expects("column3").repeat(2).returning(
        new java.math.BigInteger("1"+"0"*19)
      )
      (rs.getObject(_: Int)).expects(4).repeat(2).returning("12")
      (rs.getObject(_: String)).expects("column4").repeat(2).returning("12")
      (rs.getObject(_: Int)).expects(5).repeat(2).returning("-3")
      (rs.getObject(_: String)).expects("column5").repeat(2).returning("-3")
      (rs.getObject(_: Int)).expects(6).repeat(2).returning("010")
      (rs.getObject(_: String)).expects("column6").repeat(2).returning("010")

      it should behave like column(rs, 1, Option(12))
      it should behave like column(rs, "column1", Option(12))
      it should behave like column(rs, 2, Option(6000000000L))
      it should behave like column(rs, "column2", Option(6000000000L))
      it should behave like column(rs, 3, Option(BigInt("1"+"0"*19).longValue))
      it should behave like column(rs, "column3", Option(BigInt("1"+"0"*19).longValue))
      it should behave like column(rs, 4, Option(12))
      it should behave like column(rs, "column4", Option(12))
      it should behave like column(rs, 5, Option(-3))
      it should behave like column(rs, "column5", Option(-3))
      it should behave like column(rs, 6, Option(10))
      it should behave like column(rs, "column6", Option(10))

      assertTypeError("""
        it should behave like column(rs, 1, 12)
      """)
      assertTypeError("""
        it should behave like column(rs, "column1", 12)
      """)

      import AutoUnwrapOption._

      it should behave like column(rs, 1, 12)
      it should behave like column(rs, "column1", 12)
      it should behave like column(rs, 2, 6000000000L)
      it should behave like column(rs, "column2", 6000000000L)
      it should behave like column(rs, 3, BigInt("1"+"0"*19).longValue)
      it should behave like column(rs, "column3", BigInt("1"+"0"*19).longValue)
      it should behave like column(rs, 4, 12)
      it should behave like column(rs, "column4", 12)
      it should behave like column(rs, 5, -3)
      it should behave like column(rs, "column5", -3)
      it should behave like column(rs, 6, 10)
      it should behave like column(rs, "column6", 10)
    }

    it("should not be able to get a Long value from an invalid rep.") {
      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(1).repeat(2).returning("")
      (rs.getObject(_: String)).expects("column1").repeat(2).returning("")
      (rs.getObject(_: Int)).expects(2).repeat(2).returning("1"+"0"*19)
      (rs.getObject(_: String)).expects("column2").repeat(2).returning("1"+"0"*19)
      (rs.getObject(_: Int)).expects(3).repeat(2).returning("foo")
      (rs.getObject(_: String)).expects("column3").repeat(2).returning("foo")

      val N = 3

      for (i <- 1 to N) {
        it should behave like column[Option[Long]](rs, i, None)
        it should behave like column[Option[Long]](rs, "column"+i, None)
      }

      import AutoUnwrapOption._

      for (i <- 1 to N) {
        it should behave like throwingFromColumn[Long](rs, i)
        it should behave like throwingFromColumn[Long](rs, "column"+i)
      }
    }

    it("should not be able to get a Long from null") {
      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(0).repeat(2).returning(null)
      (rs.getObject(_: String)).expects("null").repeat(2).returning(null)

      it should behave like column[Option[Long]](rs, 0, None)
      it should behave like column[Option[Long]](rs, "null", None)

      import AutoUnwrapOption._

      it should behave like throwingFromColumn[Long](rs, 0)
      it should behave like throwingFromColumn[Long](rs, "null")
    }
  }

  describe("TypeBinder[Float]") {
    it("should be able to get a Float value") {
      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(1).repeat(2).returning(
        new java.lang.Float(1.2f)
      )
      (rs.getObject(_: String)).expects("column1").repeat(2).returning(
        new java.lang.Float(1.2f)
      )
      (rs.getObject(_: Int)).expects(2).repeat(2).returning("1.2")
      (rs.getObject(_: String)).expects("column2").repeat(2).returning("1.2")
      (rs.getObject(_: Int)).expects(3).repeat(2).returning("-3.5")
      (rs.getObject(_: String)).expects("column3").repeat(2).returning("-3.5")
      (rs.getObject(_: Int)).expects(4).repeat(2).returning("010.8")
      (rs.getObject(_: String)).expects("column4").repeat(2).returning("010.8")
      (rs.getObject(_: Int)).expects(5).repeat(2).returning("30")
      (rs.getObject(_: String)).expects("column5").repeat(2).returning("30")
      (rs.getObject(_: Int)).expects(6).repeat(2).returning("1"+"0"*20)
      (rs.getObject(_: String)).expects("column6").repeat(2).returning("1"+"0"*20)

      it should behave like column(rs, 1, Option(1.2f))
      it should behave like column(rs, "column1", Option(1.2f))
      it should behave like column(rs, 2, Option(1.2f))
      it should behave like column(rs, "column2", Option(1.2f))
      it should behave like column(rs, 3, Option(-3.5f))
      it should behave like column(rs, "column3", Option(-3.5f))
      it should behave like column(rs, 4, Option(10.8f))
      it should behave like column(rs, "column4", Option(10.8f))
      it should behave like column(rs, 5, Option(30.0f))
      it should behave like column(rs, "column5", Option(30.0f))
      it should behave like column(rs, 6, Option(1.0e20f))
      it should behave like column(rs, "column6", Option(1.0e20f))

      assertTypeError("""
        it should behave like column(rs, 1, 1.2f)
      """)
      assertTypeError("""
        it should behave like column(rs, "column1", 1.2f)
      """)

      import AutoUnwrapOption._

      it should behave like column(rs, 1, 1.2f)
      it should behave like column(rs, "column1", 1.2f)
      it should behave like column(rs, 2, 1.2f)
      it should behave like column(rs, "column2", 1.2f)
      it should behave like column(rs, 3, -3.5f)
      it should behave like column(rs, "column3", -3.5f)
      it should behave like column(rs, 4, 10.8f)
      it should behave like column(rs, "column4", 10.8f)
      it should behave like column(rs, 5, 30.0f)
      it should behave like column(rs, "column5", 30.0f)
      it should behave like column(rs, 6, 1.0e20f)
      it should behave like column(rs, "column6", 1.0e20f)
    }

    it("should not be able to get a Float value from an invalid rep.") {
      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(1).repeat(2).returning("")
      (rs.getObject(_: String)).expects("column1").repeat(2).returning("")
      (rs.getObject(_: Int)).expects(2).repeat(2).returning("foo")
      (rs.getObject(_: String)).expects("column2").repeat(2).returning("foo")

      val N = 2

      for (i <- 1 to N) {
        it should behave like column[Option[Float]](rs, i, None)
        it should behave like column[Option[Float]](rs, "column"+i, None)
      }

      import AutoUnwrapOption._

      for (i <- 1 to N) {
        it should behave like throwingFromColumn[Float](rs, i)
        it should behave like throwingFromColumn[Float](rs, "column"+i)
      }
    }

    it("should not be able to get a Float from null") {
      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(0).repeat(2).returning(null)
      (rs.getObject(_: String)).expects("null").repeat(2).returning(null)

      it should behave like column[Option[Float]](rs, 0, None)
      it should behave like column[Option[Float]](rs, "null", None)

      import AutoUnwrapOption._

      it should behave like throwingFromColumn[Float](rs, 0)
      it should behave like throwingFromColumn[Float](rs, "null")
    }
  }

  describe("TypeBinder[Double]") {
    it("should be able to get a Double value") {
      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(1).repeat(2).returning(
        new java.lang.Double(1.2)
      )
      (rs.getObject(_: String)).expects("column1").repeat(2).returning(
        new java.lang.Double(1.2)
      )
      (rs.getObject(_: Int)).expects(2).repeat(2).returning("1.2")
      (rs.getObject(_: String)).expects("column2").repeat(2).returning("1.2")
      (rs.getObject(_: Int)).expects(3).repeat(2).returning("-3.5")
      (rs.getObject(_: String)).expects("column3").repeat(2).returning("-3.5")
      (rs.getObject(_: Int)).expects(4).repeat(2).returning("010.8")
      (rs.getObject(_: String)).expects("column4").repeat(2).returning("010.8")
      (rs.getObject(_: Int)).expects(5).repeat(2).returning("30")
      (rs.getObject(_: String)).expects("column5").repeat(2).returning("30")
      (rs.getObject(_: Int)).expects(6).repeat(2).returning("1"+"0"*20)
      (rs.getObject(_: String)).expects("column6").repeat(2).returning("1"+"0"*20)

      it should behave like column(rs, 1, Option(1.2))
      it should behave like column(rs, "column1", Option(1.2))
      it should behave like column(rs, 2, Option(1.2f))
      it should behave like column(rs, "column2", Option(1.2))
      it should behave like column(rs, 3, Option(-3.5f))
      it should behave like column(rs, "column3", Option(-3.5))
      it should behave like column(rs, 4, Option(10.8f))
      it should behave like column(rs, "column4", Option(10.8))
      it should behave like column(rs, 5, Option(30.0f))
      it should behave like column(rs, "column5", Option(30.0))
      it should behave like column(rs, 6, Option(1.0e20f))
      it should behave like column(rs, "column6", Option(1.0e20))

      assertTypeError("""
        it should behave like column(rs, 1, 1.2f)
      """)
      assertTypeError("""
        it should behave like column(rs, "column1", 1.2f)
      """)

      import AutoUnwrapOption._

      it should behave like column(rs, 1, 1.2)
      it should behave like column(rs, "column1", 1.2)
      it should behave like column(rs, 2, 1.2)
      it should behave like column(rs, "column2", 1.2)
      it should behave like column(rs, 3, -3.5)
      it should behave like column(rs, "column3", -3.5)
      it should behave like column(rs, 4, 10.8)
      it should behave like column(rs, "column4", 10.8)
      it should behave like column(rs, 5, 30.0)
      it should behave like column(rs, "column5", 30.0)
      it should behave like column(rs, 6, 1.0e20)
      it should behave like column(rs, "column6", 1.0e20)
    }

    it("should not be able to get a Double value from an invalid rep.") {
      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(1).repeat(2).returning("")
      (rs.getObject(_: String)).expects("column1").repeat(2).returning("")
      (rs.getObject(_: Int)).expects(2).repeat(2).returning("foo")
      (rs.getObject(_: String)).expects("column2").repeat(2).returning("foo")

      val N = 2

      for (i <- 1 to N) {
        it should behave like column[Option[Double]](rs, i, None)
        it should behave like column[Option[Double]](rs, "column"+i, None)
      }

      import AutoUnwrapOption._

      for (i <- 1 to N) {
        it should behave like throwingFromColumn[Double](rs, i)
        it should behave like throwingFromColumn[Double](rs, "column"+i)
      }
    }

    it("should not be able to get a Double from null") {
      val rs = mock[ResultSet]
      (rs.getObject(_: Int)).expects(0).repeat(2).returning(null)
      (rs.getObject(_: String)).expects("null").repeat(2).returning(null)

      it should behave like column[Option[Double]](rs, 0, None)
      it should behave like column[Option[Double]](rs, "null", None)

      import AutoUnwrapOption._

      it should behave like throwingFromColumn[Double](rs, 0)
      it should behave like throwingFromColumn[Double](rs, "null")
    }
  }

  describe("TypeBinder[java.net.URL]") {
    import java.net.URL

    it("should be able to get a URL value") {
      val rs = mock[ResultSet]
      (rs.getURL(_: Int)).expects(1).repeat(2).returning(
        new URL("http://github.com/tarao/")
      )
      (rs.getURL(_: String)).expects("column1").repeat(2).returning(
        new URL("http://github.com/tarao/")
      )

      it should behave like
        column(rs, 1, Option(new URL("http://github.com/tarao/")))
      it should behave like
        column(rs, "column1", Option(new URL("http://github.com/tarao/")))

      assertTypeError("""
        it should behave like
          column(rs, 1, new URL("http://github.com/tarao/"))
      """)
      assertTypeError("""
        it should behave like
          column(rs, "column1", new URL("http://github.com/tarao/"))
      """)

      import AutoUnwrapOption._

      it should behave like
        column(rs, 1, new URL("http://github.com/tarao/"))
      it should behave like
        column(rs, "column1", new URL("http://github.com/tarao/"))
    }

    it("should not be able to get a URL from null") {
      val rs = mock[ResultSet]
      (rs.getURL(_: Int)).expects(0).repeat(2).returning(null)
      (rs.getURL(_: String)).expects("null").repeat(2).returning(null)

      it should behave like column[Option[URL]](rs, 0, None)
      it should behave like column[Option[URL]](rs, "null", None)

      import AutoUnwrapOption._

      it should behave like throwingFromColumn[URL](rs, 0)
      it should behave like throwingFromColumn[URL](rs, "null")
    }
  }

  describe("TypeBinder[java.sql.Date]") {
    import java.sql.Date

    it("should be able to get a Date value") {
      val today = new Date(System.currentTimeMillis)

      val rs = mock[ResultSet]
      (rs.getDate(_: Int)).expects(1).repeat(2).returning(today)
      (rs.getDate(_: String)).expects("column1").repeat(2).returning(today)

      it should behave like column(rs, 1, Option(today))
      it should behave like column(rs, "column1", Option(today))

      assertTypeError("""
        it should behave like column(rs, 1, today)
      """)
      assertTypeError("""
        it should behave like column(rs, "column1", today)
      """)

      import AutoUnwrapOption._

      it should behave like column(rs, 1, today)
      it should behave like column(rs, "column1", today)
    }

    it("should not be able to get a Date from null") {
      val rs = mock[ResultSet]
      (rs.getDate(_: Int)).expects(0).repeat(2).returning(null)
      (rs.getDate(_: String)).expects("null").repeat(2).returning(null)

      it should behave like column[Option[Date]](rs, 0, None)
      it should behave like column[Option[Date]](rs, "null", None)

      import AutoUnwrapOption._

      it should behave like throwingFromColumn[Date](rs, 0)
      it should behave like throwingFromColumn[Date](rs, "null")
    }
  }

  describe("TypeBinder[java.sql.Time]") {
    import java.sql.Time

    it("should be able to get a Time value") {
      val now = new Time(System.currentTimeMillis)

      val rs = mock[ResultSet]
      (rs.getTime(_: Int)).expects(1).repeat(2).returning(now)
      (rs.getTime(_: String)).expects("column1").repeat(2).returning(now)

      it should behave like column(rs, 1, Option(now))
      it should behave like column(rs, "column1", Option(now))

      assertTypeError("""
        it should behave like column(rs, 1, now)
      """)
      assertTypeError("""
        it should behave like column(rs, "column1", now)
      """)

      import AutoUnwrapOption._

      it should behave like column(rs, 1, now)
      it should behave like column(rs, "column1", now)
    }

    it("should not be able to get a Time from null") {
      val rs = mock[ResultSet]
      (rs.getTime(_: Int)).expects(0).repeat(2).returning(null)
      (rs.getTime(_: String)).expects("null").repeat(2).returning(null)

      it should behave like column[Option[Time]](rs, 0, None)
      it should behave like column[Option[Time]](rs, "null", None)

      import AutoUnwrapOption._

      it should behave like throwingFromColumn[Time](rs, 0)
      it should behave like throwingFromColumn[Time](rs, "null")
    }
  }

  describe("TypeBinder[java.sql.Timestamp]") {
    import java.sql.Timestamp

    it("should be able to get a Timestamp value") {
      val now = new Timestamp(System.currentTimeMillis)

      val rs = mock[ResultSet]
      (rs.getTimestamp(_: Int)).expects(1).repeat(2).returning(now)
      (rs.getTimestamp(_: String)).expects("column1").repeat(2).returning(now)

      it should behave like column(rs, 1, Option(now))
      it should behave like column(rs, "column1", Option(now))

      assertTypeError("""
        it should behave like column(rs, 1, now)
      """)
      assertTypeError("""
        it should behave like column(rs, "column1", now)
      """)

      import AutoUnwrapOption._

      it should behave like column(rs, 1, now)
      it should behave like column(rs, "column1", now)
    }

    it("should not be able to get a Timestamp from null") {
      val rs = mock[ResultSet]
      (rs.getTimestamp(_: Int)).expects(0).repeat(2).returning(null)
      (rs.getTimestamp(_: String)).expects("null").repeat(2).returning(null)

      it should behave like column[Option[Timestamp]](rs, 0, None)
      it should behave like column[Option[Timestamp]](rs, "null", None)

      import AutoUnwrapOption._

      it should behave like throwingFromColumn[Timestamp](rs, 0)
      it should behave like throwingFromColumn[Timestamp](rs, "null")
    }
  }

  describe("TypeBinder[Array[Byte]]") {
    it("should be able to get an Array[Byte] value") {
      val rs = mock[ResultSet]
      (rs.getBytes(_: Int)).expects(1).repeat(2).returning(
        Array[Byte](1, 2, 3)
      )
      (rs.getBytes(_: String)).expects("column1").repeat(2).returning(
        Array[Byte](1, 2, 3)
      )
      (rs.getBytes(_: Int)).expects(2).repeat(2).returning(
        Array[Byte]()
      )
      (rs.getBytes(_: String)).expects("column2").repeat(2).returning(
        Array[Byte]()
      )

      implicitly[TypeBinder[Option[Array[Byte]]]].apply(rs, 1).get should
        be (Array[Byte](1, 2, 3))
      implicitly[TypeBinder[Option[Array[Byte]]]].apply(rs, "column1").get should
        be (Array[Byte](1, 2, 3))
      implicitly[TypeBinder[Option[Array[Byte]]]].apply(rs, 2).get should
        be (Array[Byte]())
      implicitly[TypeBinder[Option[Array[Byte]]]].apply(rs, "column2").get should
        be (Array[Byte]())

      assertTypeError("""
        it should behave like column(rs, 1, Array[Byte](1, 2, 3))
      """)
      assertTypeError("""
        it should behave like column(rs, "column1", Array[Byte](1, 2, 3))
      """)

      import AutoUnwrapOption._

      it should behave like column(rs, 1, Array[Byte](1, 2, 3))
      it should behave like column(rs, "column1", Array[Byte](1, 2, 3))
      it should behave like column(rs, 2, Array[Byte]())
      it should behave like column(rs, "column2", Array[Byte]())
    }

    it("should not be able to get an Array[Byte] from null") {
      val rs = mock[ResultSet]
      (rs.getBytes(_: Int)).expects(0).repeat(2).returning(null)
      (rs.getBytes(_: String)).expects("null").repeat(2).returning(null)

      it should behave like column[Option[Array[Byte]]](rs, 0, None)
      it should behave like column[Option[Array[Byte]]](rs, "null", None)

      import AutoUnwrapOption._

      it should behave like throwingFromColumn[Array[Byte]](rs, 0)
      it should behave like throwingFromColumn[Array[Byte]](rs, "null")
    }
  }

  class Reader2Seq(val r: Reader) {
    def toSeq = {
      val reader = new BufferedReader(r)
      Iterator.continually{reader.read}.takeWhile(_ >= 0).toSeq
    }
  }
  implicit def reader2Seq(r: Reader) = new Reader2Seq(r)

  class InputStream2Seq(val is: InputStream) {
    def toSeq = {
      val reader = new BufferedReader(new InputStreamReader(is))
      Iterator.continually{reader.read}.takeWhile(_ >= 0).toSeq
    }
  }
  implicit def inputStream2Seq(is: InputStream) =
    new Reader2Seq(new InputStreamReader(is))

  describe("TypeBinder[java.io.InputStream]") {
    it("should be able to get an InputStream value") {
      val rs = mock[ResultSet]
      (rs.getBinaryStream(_: Int)).expects(1).returning(
        new ByteArrayInputStream(Array[Byte](1, 2, 3))
      )
      (rs.getBinaryStream(_: String)).expects("column1").returning(
        new ByteArrayInputStream(Array[Byte](1, 2, 3))
      )
      (rs.getBinaryStream(_: Int)).expects(2).returning(
        new ByteArrayInputStream(Array[Byte](4, 5, 6))
      )
      (rs.getBinaryStream(_: String)).expects("column2").returning(
        new ByteArrayInputStream(Array[Byte](4, 5, 6))
      )

      implicitly[TypeBinder[Option[InputStream]]].apply(rs, 1)
        .get.toSeq should be (Seq(1, 2, 3))
      implicitly[TypeBinder[Option[InputStream]]].apply(rs, "column1")
        .get.toSeq should be (Seq(1, 2, 3))

      assertTypeError("""
        implicitly[TypeBinder[InputStream]].apply(rs, 2)
          .toSeq should be (Seq(4, 5, 6))
      """)
      assertTypeError("""
        implicitly[TypeBinder[InputStream]].apply(rs, "column2")
          .toSeq should be (Seq(4, 5, 6))
      """)

      import AutoUnwrapOption._

      implicitly[TypeBinder[InputStream]].apply(rs, 2)
        .toSeq should be (Seq(4, 5, 6))
      implicitly[TypeBinder[InputStream]].apply(rs, "column2")
        .toSeq should be (Seq(4, 5, 6))
    }

    it("should not be be able to get an InputStream from null") {
      val rs = mock[ResultSet]
      (rs.getBinaryStream(_: Int)).expects(0).repeat(2).returning(null)
      (rs.getBinaryStream(_: String)).expects("null").repeat(2).returning(null)

      it should behave like column[Option[InputStream]](rs, 0, None)
      it should behave like column[Option[InputStream]](rs, "null", None)

      import AutoUnwrapOption._

      it should behave like throwingFromColumn[InputStream](rs, 0)
      it should behave like throwingFromColumn[InputStream](rs, "null")
    }
  }

  describe("TypeBinder[java.io.Reader]") {
    it("should be able to get an InputStream value") {
      val rs = mock[ResultSet]
      (rs.getCharacterStream(_: Int)).expects(1).returning(
        new InputStreamReader(new ByteArrayInputStream("foo bar".getBytes))
      )
      (rs.getCharacterStream(_: String)).expects("column1").returning(
        new InputStreamReader(new ByteArrayInputStream("foo bar".getBytes))
      )
      (rs.getCharacterStream(_: Int)).expects(2).returning(
        new InputStreamReader(new ByteArrayInputStream("foo bar".getBytes))
      )
      (rs.getCharacterStream(_: String)).expects("column2").returning(
        new InputStreamReader(new ByteArrayInputStream("foo bar".getBytes))
      )

      implicitly[TypeBinder[Option[Reader]]].apply(rs, 1)
        .get.toSeq.map(_.asInstanceOf[Char]).mkString should be ("foo bar")
        equal (Seq(1, 2, 3))
      implicitly[TypeBinder[Option[Reader]]].apply(rs, "column1")
        .get.toSeq.map(_.asInstanceOf[Char]).mkString should be ("foo bar")

      assertTypeError("""
        implicitly[TypeBinder[Reader]].apply(rs, 2)
          .toSeq.map(_.asInstanceOf[Char]).mkString should be ("foo bar")
      """)
      assertTypeError("""
        implicitly[TypeBinder[Reader]].apply(rs, "column2")
          .toSeq.map(_.asInstanceOf[Char]).mkString should be ("foo bar")
      """)

      import AutoUnwrapOption._

      implicitly[TypeBinder[Reader]].apply(rs, 2)
        .toSeq.map(_.asInstanceOf[Char]).mkString should be ("foo bar")
      implicitly[TypeBinder[Reader]].apply(rs, "column2")
        .toSeq.map(_.asInstanceOf[Char]).mkString should be ("foo bar")
    }

    it("should not be be able to get an Reader from null") {
      val rs = mock[ResultSet]
      (rs.getCharacterStream(_: Int)).expects(0).repeat(2).returning(null)
      (rs.getCharacterStream(_: String)).expects("null").repeat(2).returning(null)

      it should behave like column[Option[Reader]](rs, 0, None)
      it should behave like column[Option[Reader]](rs, "null", None)

      import AutoUnwrapOption._

      it should behave like throwingFromColumn[Reader](rs, 0)
      it should behave like throwingFromColumn[Reader](rs, "null")
    }
  }
}

class AutoUnwrapOptionSpec extends UnitSpec
    with TraitSingletonBehavior {
  describe("object AutoUnwrapOption") {
    it ("shuold inherit the trait") {
      it should behave like exportingTheTraitMethods
        [AutoUnwrapOption](AutoUnwrapOption)
    }
  }
}
