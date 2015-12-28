package com.bdp.daos

import com.bdp.models.SpiderQueueEntity
import com.bdp.utils.SlickPostgresConfig

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by zhange on 15/11/15.
 *
 */

trait SpiderQueueListTable extends SlickPostgresConfig with SpiderProjectListDao {

  import driver.api._

  class SpiderQueueList(tag: Tag) extends Table[SpiderQueueEntity](tag, "spiderqueuelist") {
    def queueName = column[String]("queue_name")
    def spiderName = column[String]("spider_name")
    def projectName = column[String]("project_name")

    def projectNameFK = foreignKey("PROJECTNAME_FK", projectName, spiderProjectList)(_.pname, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)

    def * = (queueName, spiderName, projectName) <> ((SpiderQueueEntity.apply _).tupled, SpiderQueueEntity.unapply)
  }

  protected val spiderQueueList = TableQuery[SpiderQueueList]
}

trait SpiderQueueListDAO extends SpiderQueueListTable {

  import driver.api._

  def getSpiderQueueList(): Future[Seq[SpiderQueueEntity]] = db.run(spiderQueueList.result)

  def getSpiderQueueByQueueName(queueName: String): Future[Option[SpiderQueueEntity]] = db.run(spiderQueueList.filter(_.queueName === queueName).result.headOption)

  def getSpiderQueueBySpiderName(spiderName: String): Future[Option[SpiderQueueEntity]] = db.run(spiderQueueList.filter(_.spiderName === spiderName).result.headOption)

  def getSpiderQueuesByProjectName(projectName: String): Future[Option[SpiderQueueEntity]] = db.run(spiderQueueList.filter(_.projectName === projectName).result.headOption)

  def insertSpiderQueue(spiderQueue: SpiderQueueEntity): Future[SpiderQueueEntity] = db.run(spiderQueueList returning spiderQueueList += spiderQueue)

  def updateSpiderQueueByQueueName(queueName: String, spiderQueueUpdate: SpiderQueueEntity): Future[Option[SpiderQueueEntity]] = getSpiderQueueByQueueName(queueName).flatMap {
    case Some(queue) =>
      db.run(spiderQueueList.filter(_.queueName === queueName).update(spiderQueueUpdate).map(_ => Some(spiderQueueUpdate)))
    case None => Future.successful(None)
  }

  def deleteSpiderQueueByQueueName(queueName: String): Future[Int] = db.run(spiderQueueList.filter(_.queueName === queueName).delete)
}
