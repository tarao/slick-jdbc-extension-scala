package com.github.tarao
package slickjdbc
package helper

import scala.language.implicitConversions
import scala.concurrent.duration.Duration
import org.scalatest.{FunSpec, BeforeAndAfterAll, BeforeAndAfterEach}
import slick.jdbc.H2Profile.api.Database

case class Timeout(duration: Duration)
object Timeout {
  implicit val forever: Timeout = Timeout(Duration.Inf)
}

class DBRunner(val db: Database) {
  import scala.concurrent.{Future, Await}
  import slick.driver.H2Driver.api.Database
  import slick.dbio.{DBIOAction, NoStream, Effect}

  def run[R](a: DBIOAction[R, NoStream, Nothing])(implicit
    timeout: Timeout
  ): R = Await.result(db.run(a), timeout.duration)

  def close = db.close
}

object FreshId {
  var id = 0
  def apply() = { id = max; id }
  def max = { id + 1 }
}

trait Repository {
  def db: DBRunner
}

trait TestDB extends BeforeAndAfterAll with BeforeAndAfterEach {
  self: FunSpec =>

  lazy val config = {
    import com.typesafe.config.{ConfigFactory, ConfigValueFactory => V}
    import slick.jdbc.JdbcDataSource

    // Rewrite database name to thread local one so that writing from
    // multiple test threads run parallel won't conflict each other.
    val c = ConfigFactory.load.getConfig("h2memtest")
    val name = "test" + Thread.currentThread.getId
    val url = c.getString("url").replaceFirst("""\btest\b""", name)
    c.withValue("url", V.fromAnyRef(url))
  }

  lazy val db = new DBRunner(Database.forConfig("", config))

  override def beforeAll {
    import slick.driver.H2Driver.api._

    db.run { sqlu"""
      CREATE TABLE IF NOT EXISTS entry (
        entry_id BIGINT NOT NULL PRIMARY KEY,
        url VARCHAR(2048) NOT NULL UNIQUE
      )
    """ }

    db.run { sqlu"""
      CREATE TABLE IF NOT EXISTS ids (
        id BIGINT NOT NULL PRIMARY KEY
      )
    """ }

    super.beforeAll
  }

  override def afterAll {
    db.close
    super.afterAll
  }
}
