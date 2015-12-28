package com.bdp.daos

import com.bdp.models.SpiderSourceEntity
import com.bdp.utils.SlickPostgresConfig
import org.joda.time.LocalDateTime

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by zhange on 15/11/15.
 *
 */

trait SpiderSourceListTable extends SlickPostgresConfig with ChannelListDAO with SpiderQueueListDAO {

  import driver.api._

  class SpiderSourceList(tag: Tag) extends Table[SpiderSourceEntity](tag, "spidersourcelist") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def createTime = column[LocalDateTime]("create_time")
    def sourceUrl = column[String]("source_url")
    def sourceName = column[String]("source_name")
    def channelName = column[String]("channel_name")
    def channelId = column[Long]("channel_id")
    def descr = column[Option[String]]("descr")
    def queueName = column[String]("queue_name")
    def frequency = column[Int]("frequency")
    def status = column[Int]("status", O.Default(1))
    def taskConf = column[Option[Map[String, String]]]("task_conf")
    def online = column[Int]("online", O.Default(0))

    def channelNameFK = foreignKey("CHANNEL_FK", channelName, channelList)(_.cname, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)
    def channelIdFK = foreignKey("CHANNEL_FK", channelId, channelList)(_.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)
    def queueNameFK = foreignKey("QUEUE_FK", queueName, spiderQueueList)(_.queueName, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)

    def * = (id.?, createTime, sourceUrl, sourceName, channelName, channelId, descr, queueName, frequency, status, taskConf, online) <> ((SpiderSourceEntity.apply _).tupled, SpiderSourceEntity.unapply)
  }

  protected val spiderSourceList = TableQuery[SpiderSourceList]
}

trait SpiderSourceListDAO extends SpiderSourceListTable {

  import driver.api._

  def getSpiderSourceList(): Future[Seq[SpiderSourceEntity]] = db.run(spiderSourceList.result)

  def getSpiderSourceById(id: Long): Future[Option[SpiderSourceEntity]] = db.run(spiderSourceList.filter(_.id === id).result.headOption)

  def getSpiderSourceByChannelId(channelId: Long): Future[Seq[SpiderSourceEntity]] = db.run(spiderSourceList.filter(_.channelId === channelId).result)

  def insertSpiderSource(spiderSource: SpiderSourceEntity): Future[SpiderSourceEntity] = db.run(spiderSourceList returning spiderSourceList += spiderSource)

  def updateSpiderSourceById(id: Long, spiderSourceUpdate: SpiderSourceEntity): Future[Option[SpiderSourceEntity]] = getSpiderSourceById(id).flatMap {
    case Some(source) =>
      db.run(spiderSourceList.filter(_.id === id).update(spiderSourceUpdate).map(_ => Some(spiderSourceUpdate)))
    case None => Future.successful(None)
  }

  def deleteSpiderSourceById(id: Long): Future[Int] = db.run(spiderSourceList.filter(_.id === id).delete)

}
