package com.bdp.crawler

//import java.sql.Timestamp
import org.joda.time._

import akka.event.Logging
import akka.actor.{ Props, ActorRef, Actor }

import com.bdp.crawler.SpiderDataPipelineServerActor._
import com.bdp.crawler.SpiderImageServerActor._
import com.bdp.daos.{ ComicChapterListDAO, ComicListDAO, NewsListDAO }
import com.bdp.models.{ ComicChapterEntity, ComicEntity, NewsEntity }
import com.bdp.rest.{ AccessTimeout, RestMessage, ResponseMessage }
import com.bdp.utils.{ RedisConfig, JodaDateUtils, DateUtils, Config }
import redis.{ RedisServer, RedisClientPool, RedisClient }

import scala.util.{ Failure, Success }
import scala.concurrent.duration._

import spray.json._
import DefaultJsonProtocol._

/**
 * Created by zhange on 15/10/26.
 *
 * 这是一个数据通道Actor,负责监听接口接收新的数据处理任务.
 * 来自抓取系统的处理任务,包括:
 *    根据抓取源分类属性异步的生成衍生数据的处理任务,比如:图片处理/NLP相关/离线任务/入库操作(PG/Mongo/SearchEngine),并对处理状态进行记录.
 *    对各个处理任务的状态进行查询和监控,并进行自动调整和告警通知.
 *
 * (抓取系统中的所有生成数据首先会存入Redis进行缓存,最后由Akka系统进行入库操作,包含PG/Mongo/SearchEngine等)
 *
 */

object SpiderDataPipelineServerActor {
  case object CreateDataPipelineTaskSucces
  case object CreateDataPipelineTaskFailed
  case class SpiderDataPipelineTask(key: String) extends RestMessage
  case class SpiderComicPipelineTask(key: String) extends RestMessage

  case class ContentBlock(name: String, value: String)
  case class ContentBlockList(contentBlockList: List[ContentBlock])
}

object ContentBlockProtocol extends DefaultJsonProtocol {
  implicit val contentBlockFormat = jsonFormat2(ContentBlock.apply)
  implicit val contentBlockListFormat = jsonFormat1(ContentBlockList.apply)
}

class SpiderDataPipelineServerActor(dataPipelineActor: ActorRef) extends Actor {

  override def receive = {
    case spiderDataPipelineTask: SpiderDataPipelineTask =>
      println(s"Get data pipeline task $spiderDataPipelineTask")
      dataPipelineActor ! spiderDataPipelineTask
    case spiderComicPipelineTask: SpiderComicPipelineTask =>
      println(s"Get data pipeline task $spiderComicPipelineTask")
      dataPipelineActor ! spiderComicPipelineTask
    case CreateDataPipelineTaskSucces =>
      println("Create data task succes"); context.parent ! ResponseMessage("succes")
    case CreateDataPipelineTaskFailed =>
      println("Create data task failed"); context.parent ! ResponseMessage("failed")
    case _ =>
  }
}

class DataPipelineActor extends Actor with RedisConfig with JodaDateUtils with NewsListDAO with ComicListDAO with ComicChapterListDAO {

  val log = Logging(context.system, this)

  val elasticPipelineActor = context.actorOf(Props[ElasticPipelineActor], "ElasticPipelineActor")

  val imagePipelineActor = context.actorOf(Props[ImagePipelineActor], "ImagePipelineActor")
  val imagePipelineServerActor = context.actorOf(Props(new SpiderImagePipelineServerActor(imagePipelineActor)), "SpiderImagePipelineServerActor")

  override def receive = {
    case SpiderDataPipelineTask(key) =>
      val parentSender = sender()

      import context.dispatcher
      val newsCacheFuture = redisClient.hgetall[String](key)

      newsCacheFuture.onComplete {
        case Success(newsCache) if newsCache.isEmpty => parentSender ! CreateDataPipelineTaskFailed
        case Success(newsCache) =>

          //println(newsCache)

          parentSender ! CreateDataPipelineTaskSucces

          context.actorOf(Props(new Actor() {

            import context.dispatcher
            val timeoutMessager = context.system.scheduler.scheduleOnce(500.seconds, self, AccessTimeout)

            var listPageImageFlag: Option[Boolean] = None
            var listPageImageList: Option[List[String]] = None

            var contentPageImageFlag: Option[Boolean] = None
            var contentPageImageList: Option[ImageOssObjectList] = None

            val contentStr: Option[String] = newsCache.get("content")
            val contentBlockList: Option[ContentBlockList] = contentStr match {
              case Some(str) => Some(parseContent(str))
              case _         => None
            }

            override def receive = {
              case ListImageResultList(imageList) =>
                listPageImageList = Some(imageList)
                checkComplete()
              case imageOssObjectList: ImageOssObjectList =>
                println("Get ImageOssObjectList of content image.")
                contentPageImageList = Some(imageOssObjectList)
                listPageImageFlag match {
                  case None =>
                    println("The listPageImageFlag is None ,process listPageImage.")
                    listPageImageFlag = Some(true)
                    imagePipelineServerActor ! ListImageOssTaskList(imageOssObjectList)
                  case _ =>
                }
                checkComplete()
              case "otherTaskConf" =>
              case AccessTimeout   => insertNewsTableAndShutDown()
              case _               =>
            }

            newsCache.getOrElse("task_conf", "null") match {
              case "null" => println("No task conf get")
              case _ =>
                self ! "otherTaskConf"
                println(s"Get task conf, start to process in sub actor.")
            }

            newsCache.get("img_num") match {
              case None      => println("No images num get.")
              case Some("0") => println("This content has no images.")
              case Some(img_num) =>
                println(s"Images num get is $img_num, process content images.")
                contentBlockList match {
                  case None => println("No content get.")
                  case Some(blockList) =>
                    contentPageImageFlag = Some(true)
                    val contentImageTaskList = ContentImageTaskList(for (block <- blockList.contentBlockList.filter(_.name equals "img")) yield block.value)
                    println(contentImageTaskList)
                    imagePipelineServerActor ! contentImageTaskList
                }
            }

            //            newsCache.get("img_list") match {
            //              case None       =>
            //              case Some("[]") =>
            //              case Some(imgList) =>
            //                println(s"Get img_list: $imgList")
            //                listPageImageFlag = Some(true)
            //                imagePipelineServerActor ! ListImageTaskList(imgList.parseJson.convertTo[List[String]])
            //              //                val imgPairList = imgList.parseJson.convertTo[List[Map[String, String]]]
            //              //                imagePipelineServerActor ! ListImageTaskList(for (imgPair <- imgPairList) yield imgPair.head._2)
            //            }

            checkComplete()

            def checkComplete() = {
              val contentImageTaskStatus: Boolean = (contentPageImageFlag, contentPageImageList) match {
                case (Some(_), Some(_)) => true
                case (Some(_), None)    => false
                case (None, _)          => true
              }

              val listPageImageTaskStatus: Boolean = (listPageImageFlag, listPageImageList) match {
                case (Some(_), Some(_)) => true
                case (Some(_), None)    => false
                case (None, _)          => true
              }

              (contentImageTaskStatus, listPageImageTaskStatus) match {
                case (true, true) => insertNewsTableAndShutDown()
                case _            =>
              }
            }

            def insertNewsTableAndShutDown() = {
              // insert NewsTable with NewsCache field form Redis
              // update NewsCache by processed fields to Redis
              println("All task ready complete. Insert to NewsTable and update to Redis.")

              val url: String = key
              val insertTime: LocalDateTime = dateTimeStrToDateTime(getCurrentDatetime())
              val pubTime: LocalDateTime = newsCache.get("pub_time") match {
                case Some(datetimeStr) => dateTimeStrToDateTime(datetimeStr)
                case None              => dateTimeStrToDateTime(getCurrentDatetime())
              }
              val title: String = newsCache.get("title").get
              val tags: Option[List[String]] = newsCache.get("keywords") match {
                case None              => None
                case Some(keywordsStr) => Some(keywordsStr.split(",").toList)
              }
              val author: Option[String] = newsCache.get("author") match {
                case None       => None
                case Some(auth) => Some(auth)
              }
              val pubName: Option[String] = newsCache.get("pub_name") match {
                case None       => None
                case Some(pubn) => Some(pubn)
              }
              val pubUrl: Option[String] = newsCache.get("pub_url") match {
                case None       => None
                case Some(pubu) => Some(pubu)
              }

              //contentBlockList.toJson
              //              val content:JsValue = newsCache.get("content").get.parseJson
              //              val content:JsValue = contentBlockList.toJson

              val content: JsValue = (contentStr, contentPageImageList) match {
                case (Some(str), Some(ossList)) =>
                  var contentTemp = str
                  ossList.ossList.foreach { ossObject =>
                    contentTemp = contentTemp.replace(ossObject.originUrl, ossObject.OssUrl)
                  }
                  contentTemp.parseJson
                case _ => JsArray()
              }

              val contentHtml: String = newsCache.get("content_html").get
              val descr: Option[String] = newsCache.get("synopsis") match {
                case None       => None
                case Some(desc) => Some(desc)
              }
              val (imgStyle: Int, imgList: Option[List[String]]) = listPageImageList match {
                case Some(imageList) if imageList.size >= 3 => (3, Some(imageList.slice(0, 3)))
                case Some(imageList) if imageList.size == 2 => (2, Some(imageList))
                case Some(imageList) if imageList.size == 1 => (1, Some(imageList))
                case Some(imageList) if imageList.isEmpty   => (0, None)
                case None                                   => (0, None)
              }
              val imgNum: Int = newsCache.get("img_num") match {
                case None          => 0
                case Some("0")     => 0
                case Some(img_num) => img_num.toInt
              }
              val compress: Option[String] = None
              val ners: Option[Map[String, String]] = None
              val channelId: Long = newsCache.get("channel_id").get.toLong
              //              val spiderSourceID: Long = newsCache.get("source_id").get.toLong
              val spiderSourceID: Long = 4.toLong
              val spiderSourceOnline: Int = newsCache.get("source_online").get.toInt
              val taskConf: Option[Map[String, String]] = None
              val taskLog: Option[Map[String, String]] = None
              val status: Int = 0

              val newsEntity = NewsEntity(
                url,
                insertTime,
                pubTime,
                title,
                tags,
                author,
                pubName,
                pubUrl,
                content,
                contentHtml,
                descr,
                imgStyle,
                imgList,
                imgNum,
                compress,
                ners,
                channelId,
                spiderSourceID,
                spiderSourceOnline,
                taskConf,
                taskLog,
                status
              )
              val pipelineTaskMonitorProcessFailed = List(getCurrentDatetime().split(":").head, "pipelinetask", "procfailed").mkString(":")

              insertNews(news = newsEntity) onComplete {
                case Success(_) => println("Inert into newsListTable success.")
                case Failure(err) =>
                  println("Inert into newsListTable failed.")
                  redisClient.lpush(pipelineTaskMonitorProcessFailed, url)
              }

              elasticPipelineActor ! newsEntity

              context.stop(self)
            }

          }))

        case Failure(err) => sender ! CreateDataPipelineTaskFailed
      }

    case SpiderComicPipelineTask(key) =>
      val parentSender = sender()

      import context.dispatcher
      val comicChapterCacheFuture = redisClient.hgetall[String](key)

      comicChapterCacheFuture.onComplete {
        case Success(comicChapterCache) if comicChapterCache.isEmpty => parentSender ! CreateDataPipelineTaskFailed
        case Success(comicChapterCache) =>
          parentSender ! CreateDataPipelineTaskSucces

          val comicUrl: String = comicChapterCache.get("comic_url").get
          val comicId: Long = comicChapterCache.get("comic_id").get.toLong
          val downloadUrl: String = comicChapterCache.get("download_url").get
          val name: Option[String] = comicChapterCache.get("name") match {
            case Some(n) => Some(n)
            case None    => None
          }
          val originName: Option[String] = comicChapterCache.get("origin_name") match {
            case Some(n) => Some(n)
            case None    => None
          }
          val author: String = comicChapterCache.get("author").get
          val category: Option[String] = comicChapterCache.get("category") match {
            case Some(n) => Some(n)
            case None    => None
          }
          val areaLocation: Option[String] = comicChapterCache.get("area_location") match {
            case Some(n) => Some(n)
            case None    => None
          }
          val tags: Option[List[String]] = comicChapterCache.get("tags") match {
            case Some(n) => Some(n.parseJson.convertTo[List[String]])
            case None    => None
          }
          val summary: Option[String] = comicChapterCache.get("summary") match {
            case Some(n) => Some(n)
            case None    => None
          }
          val pubStatus: Int = comicChapterCache.get("pub_status").get.toInt
          val lastUpdateDate: Option[String] = comicChapterCache.get("last_update_date") match {
            case Some(n) => Some(n)
            case None    => None
          }
          val titleImage: Option[String] = comicChapterCache.get("title_image") match {
            case Some(n) => Some(n)
            case None    => None
          }
          val popularity: Option[String] = comicChapterCache.get("popularity") match {
            case Some(n) => Some(n)
            case None    => None
          }
          val chapterNum: Int = comicChapterCache.get("chapter_num").get.toInt

          val comicEntity: ComicEntity = ComicEntity(comicUrl, comicId, downloadUrl, name, originName, author, category, areaLocation, tags, summary, pubStatus, lastUpdateDate, titleImage, popularity, chapterNum)

          val chapterUrl = comicChapterCache.get("chapter_url").get
          val chapterOrder = comicChapterCache.get("chapter_order").get.toInt
          val chapterName = comicChapterCache.get("name").get
          val images = comicChapterCache.get("images").get.parseJson.convertTo[List[String]]

          val comicChapterEntity = ComicChapterEntity(None, chapterUrl, chapterOrder, comicUrl, chapterName, images)

          getComicByUrl(comicUrl).onComplete {
            case Success(comic) if comic.nonEmpty =>
              insertComicChapter(comicChapterEntity).onComplete {
                case Success(_)   => println("Comic already exists, insert into comic chapter success.")
                case Failure(err) => println("Comic already exists, insert into comic chapter failed.")
              }
            case Success(_) =>
              insertComic(comicEntity).onComplete {
                case Success(_) =>
                  println("Comic doesn't exists, Insert into comic success.")
                  elasticPipelineActor ! comicEntity
                  insertComicChapter(comicChapterEntity).onComplete {
                    case Success(_)   => println("After insert into comic, insert into comic chapter success.")
                    case Failure(err) => println("After insert into comic, insert into comic chapter failed.")
                  }
                case Failure(err) => println("Comic doesn't exists, Insert into comic failed.")
              }
            case Failure(err) => println("Query comic by url failed.")
          }

        case Failure(err) => sender ! CreateDataPipelineTaskFailed
      }

    case _ =>
  }

  def parseContent(contentStr: String) = {
    val contentJson = contentStr.parseJson.convertTo[List[Map[String, String]]]
    ContentBlockList(for (block <- contentJson) yield ContentBlock(block.head._1, block.head._2))
  }
}
