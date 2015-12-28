package com.bdp.routers

import com.bdp.daos.{ NewsListDAOForApp, NewsListDAO }
import com.bdp.models.{ NewsEntityAppListPage, NewsEntityAppContentPage, NewsEntity }
import com.bdp.models.NewsEntityAppListPage._
import com.bdp.models.NewsEntityAppContentPage._
import com.bdp.utils.{ Base64Utils, JodaDateUtils, DateUtils, MediaTypesHelper }
import org.joda.time.LocalDateTime
import spray.routing._

import scala.language.postfixOps
import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global

import spray.httpx.SprayJsonSupport._
import spray.http.StatusCodes._
import spray.json.DefaultJsonProtocol._

/**
 * Created by zhange on 15/11/15.
 *
 */

trait NewsListRouter extends HttpService with MediaTypesHelper with NewsListDAO with NewsListDAOForApp with JodaDateUtils with Base64Utils {

  val newsListOperations: Route = newsPostRoute ~ newsReadRoute ~ newsPutRoute ~ newsDeleteRoute ~ newReadBDPRoute

  def newReadBDPRoute: Route = getJson {
    path("bdp" / "news" / "load") {
      parameters('cid.as[Long], 'page.as[Long]?, 'offset.as[Long]?, 'tstart.as[Long]) {
        (cid, page, offset, timestart) =>
          onComplete(getNewsListByChannelIdForLoad(cid, page.getOrElse(1L) - 1L, offset.getOrElse(20L), millisecondsToDatetime(timestart))) {
            case Success(newss) if newss.nonEmpty => complete {
              val newsResult = newss.map { n =>
                NewsEntityAppListPage(n.url, n.title, n.pubTime, n.pubName, n.pubUrl, n.channelId, n.imgStyle, n.imgList)
              }
              ResponseFormatMessage(0, "success", newsResult)
            }
            case Success(empty) => complete(ResponseErrorMessage(1, s"News not found with load: $cid, $page, $offset, $timestart"))
            case Failure(ex)    => complete(ResponseErrorMessage(2, s"An error occurred: ${ex.getMessage}"))
          }
      }
    } ~
      path("bdp" / "news" / "refresh") {
        parameters('cid.as[Long], 'page.as[Int]?, 'offset.as[Int]?, 'tstart.as[Long]) {
          (cid, page, offset, timeStart) =>
            val timesNow = getCurrentMilliseconds().toLong
            val startTime: Option[LocalDateTime] = if ((timesNow - timeStart) >= 12 * 60 * 60 * 1000) None else Some(millisecondsToDatetime(timeStart))
            onComplete(getNewsListByChannelIdForRefresh(cid, page.getOrElse(1) - 1, offset.getOrElse(20), startTime)) {
              case Success(newss) if newss.nonEmpty => complete {
                val newsResult = for (n <- newss) yield NewsEntityAppListPage(n.url, n.title, n.pubTime, n.pubName, n.pubUrl, n.channelId, n.imgStyle, n.imgList)
                ResponseFormatMessage(0, "success", newsResult)
              }
              case Success(empty) => complete(ResponseErrorMessage(1, s"News not found with refresh: $cid, $page, $offset, $timeStart"))
              case Failure(ex)    => complete(ResponseErrorMessage(2, s"An error occurred: ${ex.getMessage}"))
            }
        }
      } ~
      path("bdp" / "news" / "content") {
        parameters('url.as[String]) { url =>
          onComplete(getNewsContentByUrl(decodeBase64(url))) {
            case Success(news) if news.nonEmpty =>
              val n = news.get
              complete {
                val newsResult = NewsEntityAppContentPage(n.url, n.title, n.pubTime, n.pubName, n.pubUrl, n.channelId, n.imgNum, n.tags, n.descr, n.content)
                ResponseFormatMessage(0, "success", newsResult)
              }
            case Success(empty) => complete(ResponseErrorMessage(1, s"News content not found with url: $url"))
            case Failure(ex)    => complete(ResponseErrorMessage(2, s"An error occurred: ${ex.getMessage}"))
          }
        }
      }
  }

  def newsReadRoute: Route = getJson {
    path("news" / IntNumber / IntNumber) { (offset, limit) =>
      onComplete(getNewsList(offset, limit)) {
        case Success(newss) if newss.nonEmpty => complete(newss)
        case Success(empty)                   => complete(NotFound, "News not found")
        case Failure(ex)                      => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
    } ~
      path("news" / "url" / Segment) { url =>
        onComplete(getNewsByUrl(url)) {
          case Success(news) if news.nonEmpty => complete(news)
          case Success(None)                  => complete(NotFound, "News not found")
          case Failure(ex)                    => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      } ~
      path("news" / "title" / "equal" / Segment) { titleEqual =>
        onComplete(getNewsByTitleWithEqual(titleEqual)) {
          case Success(news) if news.nonEmpty => complete(news)
          case Success(None)                  => complete(NotFound, "News not found")
          case Failure(ex)                    => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      } ~
      path("news" / "title" / "like" / Segment) { titleLike =>
        onComplete(getNewsByTitleWithLike(titleLike)) {
          case Success(news) if news.nonEmpty => complete(news)
          case Success(empty)                 => complete(NotFound, "News not found")
          case Failure(ex)                    => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
  }

  def newsPostRoute: Route = postJson {
    path("news") {
      entity(as[NewsEntity]) { newsEntity =>
        onComplete(insertNews(newsEntity)) {
          case Success(news) => complete(news)
          case Failure(ex)   => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }
  }

  def newsPutRoute: Route = putJson {
    path("news") {
      entity(as[NewsEntity]) { newsEntity =>
        onComplete(updateNewsByUrl(newsEntity.url, newsEntity)) {
          case Success(news) if news.nonEmpty => complete(news)
          case Success(None)                  => complete(NotFound, s"Not found with url: ${newsEntity.url}")
          case Failure(ex)                    => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }
  }

  def newsDeleteRoute: Route = deleteJson {
    path("news" / "url" / Segment) { url =>
      onComplete(deleteNewsByUrl(url)) {
        case Success(1)  => complete(OK, "Success")
        case Success(x)  => complete(NotFound, s"Not found with url: $url")
        case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
    }
  }
}
