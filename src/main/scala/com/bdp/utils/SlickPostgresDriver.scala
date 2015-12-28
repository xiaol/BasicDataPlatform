package com.bdp.utils

import com.github.tminglei.slickpg._
import com.github.tminglei.slickpg.utils.SimpleArrayUtils._
import com.github.tminglei.slickpg.PgArraySupport
import slick.driver.PostgresDriver

/**
 * Created by zhange on 15/11/3.
 *
 */

trait SlickPostgresDriver extends PostgresDriver
    with ExPostgresDriver
    with PgArraySupport
    with PgDateSupportJoda
    with PgRangeSupport
    with PgHStoreSupport
    with PgSearchSupport
    with PgNetSupport
    with PgLTreeSupport
    with PgSprayJsonSupport
    with array.PgArrayJdbcTypes {

  override val pgjson = "jsonb"

  val plainAPI = new API with SprayJsonPlainImplicits with JodaDateTimePlainImplicits

  override val api = MyAPI

  object MyAPI extends API
      with ArrayImplicits
      with DateTimeImplicits
      with NetImplicits
      with LTreeImplicits
      with RangeImplicits
      with HStoreImplicits
      with SearchImplicits
      with SearchAssistants
      with CustomArrayImplicitsPlus
      with JsonImplicits {
    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)
  }

  trait CustomArrayImplicitsPlus {
    implicit val simpleLongBufferTypeMapper = new SimpleArrayJdbcType[Long]("int8").to(_.toBuffer)
    implicit val simpleStrVectorTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toVector)
    ///
    implicit val advancedStringListTypeMapper = new AdvancedArrayJdbcType[String]("text",
      fromString(identity)(_).orNull, mkString(identity))
  }
}

object SlickPostgresDriver extends SlickPostgresDriver

trait SlickPostgresConfig {

  val driver = SlickPostgresDriver

  import driver.api._

  //  def db = Database.forConfig("postgres-TEST")  // can't use def, it will create a new pool for each query
  val db = Database.forConfig("postgres-BDP")

  implicit val session: Session = db.createSession()
}