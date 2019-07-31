package com.github.tarao
package slickjdbc

import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.autoUnwrap
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.refineV
import helper.{UnitSpec, TestDB, Repository}
import interpolation.{SQLInterpolation, CompoundParameter, TableName}
import getresult.{GetResult, AutoUnwrapOption, TypeBinder}
import slick.jdbc.{SetParameter => SP, PositionedParameters}

case class URL(url: String)
case class Entry(id: Long, url: URL)

class MyURL(val url: String)

/** A sample repository trait */
trait EntryRepository extends Repository
    with SQLInterpolation with CompoundParameter
    with GetResult with AutoUnwrapOption {

  implicit object SetMyURL extends SP[MyURL] {
    def apply(v: MyURL, pp: PositionedParameters): Unit = {
      pp.setString(v.url)
    }
  }

  implicit val getEntryResult = getResult { Entry(
    column("entry_id"),
    column("url")
  ) }
  implicit val getTupleResult = getResult {
    ( column[Long]("entry_id"), column[String]("url") )
  }
  implicit def urlBinder(implicit
    binder: TypeBinder[Option[String]]
  ): TypeBinder[Option[URL]] = binder.map(_.map(URL(_)))

  val table = TableName("entry")

  def add1(entry: Entry) =
    db.run { sqlu"""
    | INSERT INTO ${table} (entry_id, url)
    | VALUES (${entry.id}, ${entry.url})
    """ }

  def add2(entry: Entry) =
    db.run { sqlu"""
    | INSERT INTO ${table} (entry_id, url)
    | VALUES ${(entry.id, entry.url)}
    """ }

  def add1(entries: Seq[Entry] Refined NonEmpty) =
    db.run { sqlu"""
    | INSERT INTO ${table} (entry_id, url)
    | VALUES $entries
    """ }

  def add2(entries: Seq[(Long, URL)] Refined NonEmpty) =
    db.run { sqlu"""
    | INSERT INTO ${table} (entry_id, url)
    | VALUES $entries
    """ }

  def add3(entries: Seq[(Long, MyURL)] Refined NonEmpty) =
    db.run { sqlu"""
    | INSERT INTO ${table} (entry_id, url)
    | VALUES $entries
    """ }

  def add4(entries: Option[util.NonEmpty[Entry]]) = entries match {
    case Some(entries) => db.run { sqlu"""
    | INSERT INTO ${table} (entry_id, url)
    | VALUES $entries
    """ }
    case None => ()
  }

  def add5(entries: Option[util.NonEmpty[(Long, URL)]]) = entries match {
    case Some(entries) => db.run { sqlu"""
    | INSERT INTO ${table} (entry_id, url)
    | VALUES $entries
    """ }
    case None => ()
  }

  def add6(entries: Option[util.NonEmpty[(Long, MyURL)]]) = entries match {
    case Some(entries) => db.run { sqlu"""
    | INSERT INTO ${table} (entry_id, url)
    | VALUES $entries
    """ }
    case None => ()
  }

  def find(entryId: Long): Option[Entry] =
    db.run { sql"""
    | SELECT * FROM ${table}
    | WHERE entry_id = $entryId
    """.as[Entry] }.headOption

  def findAsTuple(entryId: Long): Option[(Long, String)] =
    db.run { sql"""
    | SELECT * FROM ${table}
    | WHERE entry_id = $entryId
    """.as[(Long, String)] }.headOption

  def find1(entryIds: Seq[Long] Refined NonEmpty) =
    db.run { sql"""
    | SELECT * FROM ${table}
    | WHERE entry_id IN ($entryIds)
    | ORDER BY entry_id ASC
    """.as[Entry] }

  def find2(entryIds: Option[util.NonEmpty[Long]]) = entryIds match {
    case Some(ids) => db.run { sql"""
    | SELECT * FROM ${table}
    | WHERE entry_id IN ($ids)
    | ORDER BY entry_id ASC
    """.as[Entry] }
    case None => Seq.empty
  }

  def findByUrls1(urls: Seq[URL] Refined NonEmpty) =
    db.run { sql"""
    | SELECT * FROM ${table}
    | WHERE url IN ($urls)
    | ORDER BY entry_id ASC
    """.as[Entry] }

  def findByUrls2(urls: Option[util.NonEmpty[URL]]) = urls match {
    case Some(urls) => db.run { sql"""
    | SELECT * FROM ${table}
    | WHERE url IN ($urls)
    | ORDER BY entry_id ASC
    """.as[Entry] }
    case None => Seq.empty
  }
}

class IntegrationSpec extends UnitSpec with TestDB with EntryRepository {
  def freshEntry = {
    val id = helper.FreshId()
    Entry(id, URL("http://example.com/" + id))
  }

  describe("SELECTing a record") {
    it("should succeed") {
      val entry = freshEntry
      add1(entry)
      val result = find(entry.id)
      result shouldBe Some(entry)
    }

    it("should fail if nothing matched") {
      find(helper.FreshId.max) shouldBe empty
    }
  }

  describe("SELECTing a record as a tuple") {
    it("should succeed") {
      val entry = freshEntry
      add1(entry)
      val result = findAsTuple(entry.id)
      result shouldBe Some( (entry.id, entry.url.url) )
    }

    it("should fail if nothing matched") {
      findAsTuple(helper.FreshId.max) shouldBe empty
    }
  }

  describe("SELECTing multiple entries") {
    it("should succeed") {
      val entries1 = Iterator.continually{ freshEntry }.take(10).toSeq
      for (e <- entries1) add1(e)

      val entryIds = scala.util.Random.shuffle(entries1.map(_.id)).toSeq
      val result1 = find2(entryIds)
      result1 should be (entries1)

      val entries2 = Iterator.continually{ freshEntry }.take(10).toSeq
      for (e <- entries2) add2(e)

      val urls = scala.util.Random.shuffle(entries2.map(_.url)).toSeq
      val result2 = findByUrls2(urls)
      result2 should be (entries2)
    }

    it("should return an empty list if nothing matched") {
      val entryIds =  Iterator.continually{ helper.FreshId()+0L }.take(10).toSeq
      val result = find2(scala.util.Random.shuffle(entryIds).toSeq)
      result shouldBe Seq.empty
    }
  }

  describe("INSERTing multiple entries") {
    it("should succeed") {
      locally {
        val entries = Iterator.continually{ freshEntry }.take(10).toSeq
        val Right(entries1) = refineV[NonEmpty](entries)
        add1(entries1)

        val Right(entryIds) = refineV[NonEmpty](entries.map(_.id).toSeq)
        val result = find1(entryIds)
        result should be (entries)
      }

      locally {
        val entries = Iterator.continually{ freshEntry }.take(10).toSeq
        val Right(entries1) =
          refineV[NonEmpty](entries.map{ e => (e.id, e.url) })
        add2(entries1)

        val Right(urls) = refineV[NonEmpty](entries.map(_.url).toSeq)
        val result = findByUrls1(urls)
        result should be (entries)
      }

      locally {
        val entries = Iterator.continually{ freshEntry }.take(10).toSeq
        val Right(entries1) =
          refineV[NonEmpty](entries.map{ e => (e.id, new MyURL(e.url.url)) })
        add3(entries1)

        val Right(urls) = refineV[NonEmpty](entries.map(_.url).toSeq)
        val result = findByUrls1(urls)
        result should be (entries)
      }

      locally {
        val entries = Iterator.continually{ freshEntry }.take(10).toSeq
        add4(entries)

        val result = find2(entries.map(_.id).toSeq)
        result should be (entries)
      }

      locally {
        val entries = Iterator.continually{ freshEntry }.take(10).toSeq
        add5(entries.map{ e => (e.id, e.url) })

        val result = findByUrls2(entries.map(_.url).toSeq)
        result should be (entries)
      }

      locally {
        val entries = Iterator.continually{ freshEntry }.take(10).toSeq
        add6(entries.map{ e => (e.id, new MyURL(e.url.url)) })

        val result = findByUrls2(entries.map(_.url).toSeq)
        result should be (entries)
      }
    }
  }
}

/** A sample repository trait */
trait IdsRepository extends Repository
    with SQLInterpolation with CompoundParameter
    with GetResult with AutoUnwrapOption {

  def add1(ids: Seq[Tuple1[Long]] Refined NonEmpty) =
    db.run { sqlu"""
    | INSERT INTO ids (id)
    | VALUES ($ids)
    """ }

  def add2(ids: Option[util.NonEmpty[Tuple1[Long]]]) = ids match {
    case Some(ids) => db.run { sqlu"""
    | INSERT INTO ids (id)
    | VALUES ($ids)
    """ }
    case None => ()
  }

  def addBadly1(ids: Seq[Long] Refined NonEmpty) =
    db.run { sqlu"""
    | INSERT INTO ids (id)
    | VALUES ($ids)
    """ }

  def addBadly2(ids: Option[util.NonEmpty[Long]]) = ids match {
    case Some(ids) => db.run { sqlu"""
    | INSERT INTO ids (id)
    | VALUES ($ids)
    """ }
    case None => ()
  }

  def find1(ids: Seq[Long] Refined NonEmpty) =
    db.run { sql"""
    | SELECT * FROM ids
    | WHERE id IN $ids
    | ORDER BY id ASC
    """.as[Long] }

  def find2(ids: Option[util.NonEmpty[Long]]) = ids match {
    case Some(ids) => db.run { sql"""
    | SELECT * FROM ids
    | WHERE id IN $ids
    | ORDER BY id ASC
    """.as[Long] }
    case None => Seq.empty
  }
}

class SingleTupleSpec extends UnitSpec with TestDB with IdsRepository {
  def freshId = helper.FreshId().asInstanceOf[Long]

  describe("SELECTing multiple records INSERTed by single tuples") {
    it("should succeed") {
      locally {
        val ids = Iterator.continually{ freshId }.take(10).toSeq
        val tuples = ids.map(Tuple1(_))
        val Right(tuples1) = refineV[NonEmpty](tuples)
        add1(tuples1)

        val Right(ids1) =
          refineV[NonEmpty](scala.util.Random.shuffle(ids).toSeq)
        val result = find1(ids1)
        result should be (ids)
      }

      locally {
        val ids = Iterator.continually{ freshId }.take(10).toSeq
        val tuples = ids.map(Tuple1(_))
        add2(tuples)

        val result = find2(scala.util.Random.shuffle(ids).toSeq)
        result should be (ids)
      }
    }
  }

  describe("INSERTing single columns by a list") {
    it("should fail") {
      locally {
        val ids = Iterator.continually{ freshId }.take(10).toSeq
        val Right(ids1) = refineV[NonEmpty](ids)
        a [java.sql.SQLException] should be thrownBy addBadly1(ids1)
      }

      locally {
        val ids = Iterator.continually{ freshId }.take(10).toSeq
        a [java.sql.SQLException] should be thrownBy addBadly2(ids)
      }
    }
  }
}
