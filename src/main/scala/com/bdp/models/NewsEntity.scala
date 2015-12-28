package com.bdp.models

import org.joda.time._
import spray.json._
import spray.json.DefaultJsonProtocol
import com.bdp.utils.JodaDateUtilsJsonProtocol._

/**
 * Created by zhange on 15/11/9.
 *
 */

case class NewsEntity(
  url: String,
  insertTime: LocalDateTime,
  pubTime: LocalDateTime,
  title: String,
  tags: Option[List[String]] = None, // TEXT[]
  author: Option[String] = None,
  pubName: Option[String] = None,
  pubUrl: Option[String] = None,
  content: JsValue, // JSON
  contentHtml: String,
  descr: Option[String] = None,
  imgStyle: Int,

  imgList: Option[List[String]] = None, // TEXT[]
  imgNum: Int,

  compress: Option[String] = None,
  ners: Option[Map[String, String]] = None, // HStore

  channelId: Long,
  spiderSourceID: Long,
  spiderSourceOnline: Int,
  taskConf: Option[Map[String, String]] = None, // HStore

  taskLog: Option[Map[String, String]] = None, // HStore
  status: Int)

object NewsEntity extends DefaultJsonProtocol {
  implicit val newsEntityFormat = jsonFormat22(NewsEntity.apply)
}

case class NewsEntityAppListPage(
  url: String,
  title: String,
  pubTime: LocalDateTime,
  pubName: Option[String] = None,
  pubUrl: Option[String] = None,
  channelId: Long,
  imgStyle: Int,
  imgList: Option[List[String]] = None)

object NewsEntityAppListPage extends DefaultJsonProtocol {
  implicit val newsEntityAppListPageFormat = jsonFormat8(NewsEntityAppListPage.apply)
}

case class NewsEntityAppContentPage(
  url: String,
  title: String,
  pubTime: LocalDateTime,
  pubName: Option[String] = None,
  pubUrl: Option[String] = None,
  channelId: Long,
  imgNum: Int,
  tags: Option[List[String]] = None,
  descr: Option[String] = None,
  content: JsValue)

object NewsEntityAppContentPage extends DefaultJsonProtocol {
  implicit val newsEntityAppContentPageFormat = jsonFormat10(NewsEntityAppContentPage.apply)
}