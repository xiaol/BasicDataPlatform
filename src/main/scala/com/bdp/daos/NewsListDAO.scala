package com.bdp.daos

import org.joda.time._

import com.bdp.models.{ NewsEntityAppListPage, NewsEntity }
import com.bdp.utils.SlickPostgresConfig
import spray.json.JsValue

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by zhange on 15/11/15.
 *
 */

trait NewsListTable extends SlickPostgresConfig with ChannelListDAO with SpiderSourceListDAO {

  import driver.api._

  class NewsList(tag: Tag) extends Table[NewsEntity](tag, "newslist") {
    def url = column[String]("url", O.PrimaryKey)

    //    def insertTime = column[Timestamp]("insert_time")
    def insertTime = column[LocalDateTime]("insert_time")

    //    def pubTime = column[Timestamp]("pub_time")
    def pubTime = column[LocalDateTime]("pub_time")

    def title = column[String]("title")

    def tags = column[Option[List[String]]]("tags")

    def author = column[Option[String]]("author")

    def pubName = column[Option[String]]("pub_name")

    def pubUrl = column[Option[String]]("pub_url")

    def content = column[JsValue]("content")

    def contentHtml = column[String]("content_html")

    def descr = column[Option[String]]("descr")

    def imgStyle = column[Int]("img_style", O.Default(0))

    def imgList = column[Option[List[String]]]("img_list")

    def imgNum = column[Int]("img_num", O.Default(0))

    def compress = column[Option[String]]("compress")

    def ners = column[Option[Map[String, String]]]("ners")

    def channelId = column[Long]("channel_id")

    def spiderSourceID = column[Long]("spider_source_id")

    def spiderSourceOnline = column[Int]("spider_source_online")

    def taskConf = column[Option[Map[String, String]]]("task_conf")

    def taskLog = column[Option[Map[String, String]]]("task_log")

    def status = column[Int]("status", O.Default(1))

    def channelIdFK = foreignKey("CHANNEL_FK", channelId, channelList)(_.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)

    def spiderSourceIDFK = foreignKey("SPIDER_SOURCE_FK", spiderSourceID, spiderSourceList)(_.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)

    def * = (url, insertTime, pubTime, title, tags, author, pubName, pubUrl, content, contentHtml, descr, imgStyle, imgList, imgNum, compress, ners, channelId, spiderSourceID, spiderSourceOnline, taskConf, taskLog, status) <> ((NewsEntity.apply _).tupled, NewsEntity.unapply)
  }

  protected val newsList = TableQuery[NewsList]
}

trait NewsListDAO extends NewsListTable {

  import driver.api._

  def getNewsList(offset: Int, limit: Int): Future[Seq[NewsEntity]] = db.run(newsList.sortBy(_.pubTime.asc).drop(offset).take(limit).result)

  def getNewsByUrl(url: String): Future[Option[NewsEntity]] = db.run(newsList.filter(_.url === url).result.headOption)

  def getNewsByTitleWithEqual(title: String): Future[Option[NewsEntity]] = db.run(newsList.filter(_.title === title).result.headOption)

  def getNewsByTitleWithLike(title: String): Future[Seq[NewsEntity]] = db.run(newsList.filter(_.title like title).result)

  def insertNews(news: NewsEntity): Future[NewsEntity] = db.run(newsList returning newsList += news)

  def updateNewsByUrl(url: String, newsUpdate: NewsEntity): Future[Option[NewsEntity]] = getNewsByUrl(url).flatMap {
    case Some(news) =>
      db.run(newsList.filter(_.url === url).update(newsUpdate).map(_ => Some(newsUpdate)))
    case None => Future.successful(None)
  }

  def deleteNewsByUrl(url: String): Future[Int] = db.run(newsList.filter(_.url === url).delete)
}

trait NewsListDAOForApp extends NewsListTable {
  import driver.api._

  //  val getNewsListByChannelIdForLoadAction = Compiled((channelId: ConstColumn[Long], page: ConstColumn[Long], offset: ConstColumn[Long], startTime: ConstColumn[LocalDateTime]) => newsList.filter(_.channelId === channelId).filter(_.pubTime < startTime).sortBy(_.pubTime.desc).drop(page).take(offset))
  //  def getNewsListByChannelIdForLoad(channelId: Long, page: Long, offset: Long, startTime: LocalDateTime) = {
  //    db.run(getNewsListByChannelIdForLoadAction(channelId, page * offset, offset, startTime).result)
  //  }

  def getNewsListByChannelIdForLoad(channelId: Long, page: Long, offset: Long, startTime: LocalDateTime) = {
    db.run(newsList.filter(_.channelId === channelId).filter(_.pubTime < startTime).sortBy(_.pubTime.desc).drop(page * offset).take(offset).result)
  }

  def getNewsListByChannelIdForRefresh(channelId: Long, page: Int, offset: Int, startTime: Option[LocalDateTime]): Future[Seq[NewsEntity]] = {
    startTime match {
      case Some(stime) => db.run(newsList.filter(_.channelId === channelId).filter(_.pubTime > stime).sortBy(_.pubTime.asc).drop(page * offset).take(offset).result)
      case None        => db.run(newsList.filter(_.channelId === channelId).sortBy(_.pubTime.desc).drop(page * offset).take(offset).result)
    }
  }

  def getNewsContentByUrl(url: String): Future[Option[NewsEntity]] = {
    db.run(newsList.filter(_.url === url).result.headOption)
  }
}