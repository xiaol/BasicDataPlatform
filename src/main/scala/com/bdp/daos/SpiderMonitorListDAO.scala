package com.bdp.daos

import com.bdp.models.SpiderMonitorEntity
import com.bdp.utils.SlickPostgresConfig
import org.joda.time.LocalDate

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by zhange on 15/11/15.
 *
 */

trait SpiderMonitorListTable extends SlickPostgresConfig with SpiderQueueListDAO {

  import driver.api._

  class SpiderMonitorList(tag: Tag) extends Table[SpiderMonitorEntity](tag, "spidermonitorlist") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def queueName = column[String]("queue")
    def dateDay = column[LocalDate]("date_day")
    def dateHour = column[Int]("date_hour")
    def createList = column[Option[List[String]]]("create_list")
    def succesList = column[Option[List[String]]]("succes_list")
    def failedList = column[Option[List[String]]]("failed_list")

    def queueNameFK = foreignKey("QUEUE_FK", queueName, spiderQueueList)(_.queueName, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)

    def * = (id.?, queueName, dateDay, dateHour, createList, succesList, failedList) <> ((SpiderMonitorEntity.apply _).tupled, SpiderMonitorEntity.unapply)
  }

  protected val spiderMonitorList = TableQuery[SpiderMonitorList]

}

trait SpiderMonitorListDAO extends SpiderMonitorListTable {

  import driver.api._

  def getSpiderMonitorList(): Future[Seq[SpiderMonitorEntity]] = db.run(spiderMonitorList.result)

  def getSpiderMonitorById(id: Long): Future[Option[SpiderMonitorEntity]] = db.run(spiderMonitorList.filter(_.id === id).result.headOption)

  def getSpiderMonitorByQueueName(queueName: String): Future[Option[SpiderMonitorEntity]] = db.run(spiderMonitorList.filter(_.queueName === queueName).result.headOption)

  def insertSpiderMonitor(spiderMonitor: SpiderMonitorEntity): Future[SpiderMonitorEntity] = db.run(spiderMonitorList returning spiderMonitorList += spiderMonitor)

  def updateSpiderMonitorById(id: Long, spiderMonitorUpdate: SpiderMonitorEntity): Future[Option[SpiderMonitorEntity]] = getSpiderMonitorById(id).flatMap {
    case Some(monitor) =>
      db.run(spiderMonitorList.filter(_.id === id).update(spiderMonitorUpdate).map(_ => Some(spiderMonitorUpdate)))
    case None => Future.successful(None)
  }

  def deleteSpiderMonitorById(id: Long): Future[Int] = db.run(spiderMonitorList.filter(_.id === id).delete)
}
