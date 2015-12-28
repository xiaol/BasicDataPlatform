package com.bdp.daos

import com.bdp.models.SpiderProjectEntity
import com.bdp.utils.SlickPostgresConfig

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by zhange on 15/11/16.
 *
 */

trait SpiderProjectListTable extends SlickPostgresConfig {

  import driver.api._

  class SpiderProjectList(tag: Tag) extends Table[SpiderProjectEntity](tag, "spiderprojectlist") {
    def pname = column[String]("pname", O.PrimaryKey)
    def serverList = column[List[String]]("server_list")

    def * = (pname, serverList) <> ((SpiderProjectEntity.apply _).tupled, SpiderProjectEntity.unapply)
  }

  protected val spiderProjectList = TableQuery[SpiderProjectList]
}

trait SpiderProjectListDao extends SpiderProjectListTable {

  import driver.api._

  def getSpiderProjectList(): Future[Seq[SpiderProjectEntity]] = db.run(spiderProjectList.result)

  def getSpiderProjectByName(projectName: String): Future[Option[SpiderProjectEntity]] = db.run(spiderProjectList.filter(_.pname === projectName).result.headOption)

  def insertSpiderProject(spiderProject: SpiderProjectEntity): Future[SpiderProjectEntity] = db.run(spiderProjectList returning spiderProjectList += spiderProject)

  def updateSpiderProjectByName(projectName: String, spiderProjectUpdate: SpiderProjectEntity): Future[Option[SpiderProjectEntity]] = getSpiderProjectByName(projectName).flatMap {
    case Some(project) =>
      db.run(spiderProjectList.filter(_.pname === projectName).update(spiderProjectUpdate).map(_ => Some(spiderProjectUpdate)))
    case None => Future.successful(None)
  }

  def deleteSpiderProjectByName(projectName: String): Future[Int] = db.run(spiderProjectList.filter(_.pname === projectName).delete)
}
