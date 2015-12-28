package com.bdp.routers

import com.bdp.daos.SpiderSourceListDAO
import com.bdp.models.SpiderSourceEntity
import com.bdp.utils.MediaTypesHelper
import spray.routing._

import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global

import spray.httpx.SprayJsonSupport._

import spray.http.StatusCodes._

/**
 * Created by zhange on 15/11/15.
 *
 *
 *
 * 爬虫的抓取源管理和实时调度接口.
 * 主要涉及对数据表PostgreSQL/SpiderSetup的操作.
 *
 * id:抓取源ID
 * sname:爬虫名称
 * statu:开启状态
 *
 * GET:
 *    id:返回该id对应抓取源的所有属性字段;
 *    sname:返回等于该爬虫名的抓取源列表,每个抓取源包含基本字段(spider_name,start_title,channel_name,frequency,status);
 *    statu:返回开启/关闭状态的抓取源列表,每个抓取源包含基本字段;
 *
 * POST:
 *    根据提交的字段属性创建新的抓取源,同时通知调度器创建调度任务;
 *
 * PUT:
 *    根据ID和字段属性修改该抓取源,同时通知调度器重启调度任务;
 *
 * DELETE:
 *    根据ID删除该抓取源,同时通知调度器关闭调度任务;
 *
 */

trait SpiderSourceListRouter extends HttpService with MediaTypesHelper with SpiderSourceListDAO {

  val spiderSourceListOperations: Route = sourcePostRoute ~ sourceReadRoute ~ sourcePutRoute ~ sourceDeleteRoute

  def sourceReadRoute: Route = getJson {
    path("spider" / "sources") {
      onComplete(getSpiderSourceList()) {
        case Success(sources) if sources.nonEmpty => complete(sources)
        case Success(empty)                       => complete(NotFound, "Sources not found")
        case Failure(ex)                          => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
    } ~
      path("spider" / "sources" / "id" / LongNumber) { sourceId =>
        onComplete(getSpiderSourceById(sourceId)) {
          case Success(source) if source.nonEmpty => complete(source)
          case Success(None)                      => complete(NotFound, "Source not found")
          case Failure(ex)                        => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      } ~
      path("spider" / "sources" / "channel" / LongNumber) { channelId =>
        onComplete(getSpiderSourceByChannelId(channelId)) {
          case Success(sources) if sources.nonEmpty => complete(sources)
          case Success(empty)                       => complete(NotFound, "Sources not found")
          case Failure(ex)                          => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
  }

  def sourcePostRoute: Route = postJson {
    path("spider" / "sources") {
      entity(as[SpiderSourceEntity]) { spiderSourceEntity =>
        onComplete(insertSpiderSource(spiderSourceEntity)) {
          case Success(source) => complete(source)
          case Failure(ex)     => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }
  }

  def sourcePutRoute: Route = putJson {
    path("spider" / "sources") {
      entity(as[SpiderSourceEntity]) { spiderSourceEntity =>
        if (spiderSourceEntity.id.isEmpty) { complete(BadRequest, "Must provide ID when updating.") }
        else {
          onComplete(updateSpiderSourceById(spiderSourceEntity.id.get, spiderSourceEntity)) {
            case Success(monitor) if monitor.nonEmpty => complete(monitor)
            case Success(None)                        => complete(NotFound, s"Not found with ID: ${spiderSourceEntity.id.get}")
            case Failure(ex)                          => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
          }
        }
      }
    }
  }

  def sourceDeleteRoute: Route = deleteJson {
    path("spider" / "sources" / "id" / LongNumber) { sourceId =>
      onComplete(deleteSpiderSourceById(sourceId)) {
        case Success(1)  => complete(OK, "Success")
        case Success(x)  => complete(NotFound, s"Not found with ID: $sourceId")
        case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
    }
  }
}
