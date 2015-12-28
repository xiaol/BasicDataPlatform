package com.bdp.testanotherway.service

import com.bdp.testanotherway.dao.ChannelDao
import com.bdp.testanotherway.model.Channel
import com.bdp.testanotherway.utils.DatabaseConfig._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by zhange on 15/11/14.
 *
 */

trait ChannelService {

  def channelDao = ChannelDao

  def getAll(): Future[Seq[Channel]]

  def get(id: Int): Future[Option[Channel]]

  def delete(id: Int): Future[Int]
}

object ChannelService extends ChannelService {

  override val channelDao = ChannelDao

  override def getAll(): Future[Seq[Channel]] = db.run {
    channelDao.getAll
  }

  override def get(id: Int): Future[Option[Channel]] = db.run {
    channelDao.get(id)
  }

  override def delete(id: Int): Future[Int] = db.run {
    channelDao.delete(id)
  }
}