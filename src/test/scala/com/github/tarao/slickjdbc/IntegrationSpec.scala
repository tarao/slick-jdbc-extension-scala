package com.github.tarao
package slickjdbc

import helper.{UnitSpec, TestDB, Repository}
import interpolation.{SQLInterpolation, CompoundParameter, TableName}
import getresult.{GetResult, AutoUnwrapOption, TypeBinder}

case class URL(url: String)
case class Entry(id: Long, url: URL)

/** A sample repository trait */
trait EntryRepository extends Repository
    with SQLInterpolation with CompoundParameter
    with GetResult with AutoUnwrapOption {
  import com.github.tarao.nonempty.NonEmpty

  implicit val getEntryResult = getResult { Entry(
    column("entry_id"),
    column("url")
  ) }
  implicit def urlBinder(implicit
    binder: TypeBinder[Option[String]]
  ): TypeBinder[Option[URL]] = binder.map(_.map(URL(_)))

  val table = TableName("entry")

  def add(entry: Entry) {
    db.run { sqlu"""
    | INSERT INTO ${table} (entry_id, url)
    | VALUES (${entry.id}, ${entry.url})
    """
  } }

  def add(entries: Option[NonEmpty[Entry]]) = { entries match {
    case Some(entries) => db.run { sqlu"""
    | INSERT INTO ${table} (entry_id, url)
    | VALUES $entries
    """ }
    case None => ()
  } }

  def find(entryId: Long): Option[Entry] =
    db.run { sql"""
    | SELECT * FROM ${table}
    | WHERE entry_id = $entryId
    """.as[Entry] }.headOption

  def find(entryIds: Option[NonEmpty[Long]]) = entryIds match {
    case Some(ids) => db.run { sql"""
    | SELECT * FROM ${table}
    | WHERE entry_id IN ($ids)
    | ORDER BY entry_id ASC
    """.as[Entry] }
    case None => Seq.empty
  }

  def findByUrls(urls: Option[NonEmpty[URL]]) = urls match {
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
      add(entry)
      val result = find(entry.id)
      result shouldBe Some(entry)
    }

    it("should fail if nothing matched") {
      find(helper.FreshId.max) shouldBe empty
    }
  }

  describe("SELECTing multiple entries") {
    it("should succeed") {
      val entries = Iterator.continually{ freshEntry }.take(10).toSeq
      for (e <- entries) add(e)

      val entryIds = scala.util.Random.shuffle(entries.map(_.id)).toSeq
      val result1 = find(entryIds)
      result1 should be (entries)

      val urls = scala.util.Random.shuffle(entries.map(_.url)).toSeq
      val result2 = findByUrls(urls)
      result2 should be (entries)
    }

    it("should return an empty list if nothing matched") {
      val entryIds =  Iterator.continually{ helper.FreshId()+0L }.take(10).toSeq
      val result = find(scala.util.Random.shuffle(entryIds).toSeq)
      result shouldBe Seq.empty
    }
  }

  describe("INSERTing multiple entries") {
    it("should succeed") {
      val entries = Iterator.continually{ freshEntry }.take(10).toSeq
      add(entries)

      val result = find(entries.map(_.id).toSeq)
      result should be (entries)
    }
  }
}
