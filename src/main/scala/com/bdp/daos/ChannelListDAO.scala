package com.bdp.daos

import com.bdp.models.ChannelEntity
import com.bdp.utils.SlickPostgresConfig

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by zhange on 15/11/14.
 *
 */

trait ChannelListTable extends SlickPostgresConfig {

  import driver.api._

  class ChannelList(tag: Tag) extends Table[ChannelEntity](tag, "channellist") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def cname = column[String]("cname")
    def des = column[Option[String]]("des")
    def img = column[Option[String]]("img")
    def adrImg = column[Option[String]]("adr_img")
    def iosImg = column[Option[String]]("ios_img")
    def online = column[Int]("online", O.Default(0))

    def * = (id.?, cname, des, img, adrImg, iosImg, online) <> ((ChannelEntity.apply _).tupled, ChannelEntity.unapply)
  }

  protected val channelList = TableQuery[ChannelList]
}

trait ChannelListDAO extends ChannelListTable {

  import driver.api._

  def getChannelList(): Future[Seq[ChannelEntity]] = db.run(channelList.result)

  def getChannelById(id: Long): Future[Option[ChannelEntity]] = db.run(channelList.filter(_.id === id).result.headOption)

  def getChannelByName(name: String): Future[Option[ChannelEntity]] = db.run(channelList.filter(_.cname === name).result.headOption)

  def getChannelsByOline(online: Int): Future[Seq[ChannelEntity]] = db.run(channelList.filter(_.online === online).result)

  //  val getChannelsByOnlineAction = Compiled((online: ConstColumn[Int]) => channelList.filter(_.online === online))
  //  def getChannelsByOline(online: Int): Future[Seq[ChannelEntity]] = db.run(getChannelsByOnlineAction(online).result)

  def insertChannel(channel: ChannelEntity): Future[ChannelEntity] = db.run(channelList returning channelList += channel)

  def updateChannelById(id: Long, channelUpdate: ChannelEntity): Future[Option[ChannelEntity]] = getChannelById(id).flatMap {
    case Some(channel) =>
      db.run(channelList.filter(_.id === id).update(channelUpdate).map(_ => Some(channelUpdate)))
    case None => Future.successful(None)
  }

  def deleteChannelById(id: Long): Future[Int] = db.run(channelList.filter(_.id === id).delete)
}