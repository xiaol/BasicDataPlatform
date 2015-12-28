package com.bdp.routers

import com.bdp.daos.SpiderMonitorListDAO
import com.bdp.models.SpiderMonitorEntity
import com.bdp.utils.MediaTypesHelper
import spray.routing._

import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global

import spray.httpx.SprayJsonSupport._

import spray.http.StatusCodes._

/**
 * Created by zhange on 15/11/15.
 * 爬虫的详情解析状态统计接口.
 * 主要涉及对数据表PostgreSQL/SpiderExtractorMonitor的操作,包括对各个抓取队列状态的查询管理.
 *
 * sname:爬虫名称;
 * dtlist:日期列表;
 * hlist:小时列表;
 *
 * GET:
 *    sname必选,dtlist必选,hlist可选;
 *    提供hlist时返回对应小时的各类型数据,否则返回dtlist中所有存在的各个小时数据.*
 *
 */

trait SpiderMonitorListRouter extends HttpService with MediaTypesHelper with SpiderMonitorListDAO {

  val spiderMonitorListOperations: Route = monitorPostRoute ~ monitorReadRoute ~ monitorPutRoute ~ monitorDeleteRoute

  def monitorReadRoute: Route = getJson {
    path("spider" / "monitors") {
      onComplete(getSpiderMonitorList()) {
        case Success(monitors) if monitors.nonEmpty => complete(monitors)
        case Success(empty)                         => complete(NotFound, "Monitors not found")
        case Failure(ex)                            => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
    } ~
      path("spider" / "monitors" / "id" / LongNumber) { monitorId =>
        onComplete(getSpiderMonitorById(monitorId)) {
          case Success(monitor) if monitor.nonEmpty => complete(monitor)
          case Success(None)                        => complete(NotFound, s"Monitor not found with ID: $monitorId")
          case Failure(ex)                          => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      } ~
      path("spider" / "monitors" / "qname" / Segment) { queueName =>
        onComplete(getSpiderMonitorByQueueName(queueName)) {
          case Success(monitors) if monitors.nonEmpty => complete(monitors)
          case Success(empty)                         => complete(NotFound, "Monitors not found")
          case Failure(ex)                            => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
  }

  def monitorPostRoute: Route = postJson {
    path("spider" / "monitors") {
      entity(as[SpiderMonitorEntity]) { spiderMonitorEntity =>
        onComplete(insertSpiderMonitor(spiderMonitorEntity)) {
          case Success(monitor) => complete(monitor)
          case Failure(ex)      => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }
  }

  def monitorPutRoute: Route = putJson {
    path("spider" / "monitors") {
      entity(as[SpiderMonitorEntity]) { spiderMonitorEntity =>
        if (spiderMonitorEntity.id.isEmpty) { complete(BadRequest, "Must provide ID when updating.") }
        else {
          onComplete(updateSpiderMonitorById(spiderMonitorEntity.id.get, spiderMonitorEntity)) {
            case Success(monitor) if monitor.nonEmpty => complete(monitor)
            case Success(None)                        => complete(NotFound, s"Not found with ID: ${spiderMonitorEntity.id.get}")
            case Failure(ex)                          => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
          }
        }
      }
    }
  }

  def monitorDeleteRoute: Route = deleteJson {
    path("spider" / "monitors" / "id" / LongNumber) { monitorId =>
      onComplete(deleteSpiderMonitorById(monitorId)) {
        case Success(1)  => complete(OK, "Success")
        case Success(x)  => complete(NotFound, s"Not found with ID: $monitorId")
        case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
    }
  }
}
