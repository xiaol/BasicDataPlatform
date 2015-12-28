package com.bdp.testanotherway.dao

/**
 * Created by zhange on 15/11/14.
 *
 */

import com.bdp.testanotherway.model.Channel
import com.bdp.testanotherway.utils.DatabaseConfig.profile.api._

trait ChannelDao {

  def create

  def getAll: DBIO[Seq[Channel]]

  def get(id: Int): DBIO[Option[Channel]]

  //  def get(email: String): DBIO[Option[(User, UserPassword)]]

  def add(channel: Channel): DBIO[Option[Int]]

  def delete(id: Int): DBIO[Int]

}

trait ChannelDaoSlickImpl extends ChannelDao {

  class Channels(tag: Tag) extends Table[Channel](tag, "channellist") {

    def id: Rep[Option[Int]] = column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)
    def cname: Rep[String] = column[String]("cname")
    def des: Rep[Option[String]] = column[Option[String]]("des")
    def img: Rep[Option[String]] = column[Option[String]]("img")
    def adrImg: Rep[Option[String]] = column[Option[String]]("adr_img")
    def iosImg: Rep[Option[String]] = column[Option[String]]("ios_img")
    def online: Rep[Int] = column[Int]("online", O.Default(0))

    def * = (id, cname, des, img, adrImg, iosImg, online) <> ((Channel.apply _).tupled, Channel.unapply)
  }

  val channels = TableQuery[Channels]

  override def create = channels.schema.create

  override def getAll: DBIO[Seq[Channel]] = channels.result

  override def get(id: Int): DBIO[Option[Channel]] = channels.filter(_.id === id).result.headOption

  //  override def get(email: String): DBIO[Option[(User, UserPassword)]] =
  //    (for {
  //      user <- users.filter(_.email === email)
  //      password <- PasswordDao.passwords.filter(_.id === user.id)
  //    } yield (user, password)).result.headOption

  override def add(channel: Channel): DBIO[Option[Int]] = {
    (channels returning channels.map(_.id)) += channel
  }

  override def delete(id: Int): DBIO[Int] = channels.filter(_.id === id).delete
}

object ChannelDao extends ChannelDaoSlickImpl
