package com.bdp.routers

import com.bdp.daos.SpiderQueueListDAO
import com.bdp.models.SpiderQueueEntity
import com.bdp.utils.MediaTypesHelper
import spray.routing._

import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global

import spray.httpx.SprayJsonSupport._

import spray.http.StatusCodes._

/**
 * Created by zhange on 15/11/15.
 *
 * 爬虫抓取队列(爬虫列表)管理接口.
 * 主要负责抓取任务队列(爬虫)的CRUD操作,由数据库表SpiderQueueList存储.
 *
 * (修改或删除队列时需确保对应的爬虫已经关闭)
 * 各个爬虫的当前抓取任务队列实时监控接口.
 * 主要涉及对实时抓取任务队列的操作,包括查看当前抓取队列长度,添加任务/清空队列等操作.
 *
 * 队列名的组成:QueueName+QueueType
 *
 * GET:
 *    type和name均为可选参数
 *    只提供type时,返回该type的所有名称的队列长度;
 *    只提供name时,返回该名称的所有类型队列长度;
 *    同时提供则提供对应类型和名称的队列长度,否则返回所有类型所有名称的队列长度;
 *
 * POST:
 *    type/name/url均为必选参数,添加url到对应类型对应名称的抓取任务队列,url使用base64压缩.
 *
 * DELETE:
 *    type为可选参数,name为可选参数,flag为可选参数;
 *    如果只给出type,清空该类型所有队列;
 *    如果只给出name,清空该name对应所有类型的队列;
 *    同时给出,情况对应type和name的队列;
 *    flag为true,清空所有类型所有名称的的抓取队列.
 *
 */

trait SpiderQueueListRouter extends HttpService with MediaTypesHelper with SpiderQueueListDAO {

  val spiderQueueListOperations: Route = queuePostRoute ~ queueReadRoute ~ queuePutRoute ~ queueDeleteRoute

  def queueReadRoute: Route = getJson {
    path("spider" / "queues") {
      onComplete(getSpiderQueueList()) {
        case Success(queues) if queues.nonEmpty => complete(queues)
        case Success(empty)                     => complete(NotFound, "Queues not found")
        case Failure(ex)                        => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
    } ~
      path("spider" / "queues" / "qname" / Segment) { queueName =>
        onComplete(getSpiderQueueByQueueName(queueName)) {
          case Success(queue) if queue.nonEmpty => complete(queue)
          case Success(None)                    => complete(NotFound, s"Queue not found with QueueName: $queueName")
          case Failure(ex)                      => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      } ~
      path("spider" / "queues" / "spider" / Segment) { spiderName =>
        onComplete(getSpiderQueueBySpiderName(spiderName)) {
          case Success(queue) if queue.nonEmpty => complete(queue)
          case Success(None)                    => complete(NotFound, s"Queue not found with SpiderName: $spiderName")
          case Failure(ex)                      => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      } ~
      path("spider" / "queues" / "project" / Segment) { projectName =>
        onComplete(getSpiderQueuesByProjectName(projectName)) {
          case Success(queues) if queues.nonEmpty => complete(queues)
          case Success(empty)                     => complete(NotFound, s"Queue not found with ProjectName: $projectName")
          case Failure(ex)                        => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
  }

  def queuePostRoute: Route = postJson {
    path("spider" / "queues") {
      entity(as[SpiderQueueEntity]) { spiderQueueEntity =>
        onComplete(insertSpiderQueue(spiderQueueEntity)) {
          case Success(queue) => complete(queue)
          case Failure(ex)    => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }
  }

  def queuePutRoute: Route = putJson {
    path("spider" / "queues") {
      entity(as[SpiderQueueEntity]) { spiderQueueEntity =>
        onComplete(updateSpiderQueueByQueueName(spiderQueueEntity.queueName, spiderQueueEntity)) {
          case Success(queue) if queue.nonEmpty => complete(queue)
          case Success(None)                    => complete(NotFound, s"Not found with QueueName: ${spiderQueueEntity.queueName}")
          case Failure(ex)                      => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }
  }

  def queueDeleteRoute: Route = deleteJson {
    path("spider" / "queues" / "qname" / Segment) { queueName =>
      onComplete(deleteSpiderQueueByQueueName(queueName)) {
        case Success(1)  => complete(OK, "Success")
        case Success(x)  => complete(NotFound, s"Not found with qname: $queueName")
        case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
    }
  }
}
