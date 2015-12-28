package com.bdp.rest

import akka.actor.{ Props, Actor }
import com.bdp.cores.RemoteProxyServerActor
import com.bdp.crawler.SpiderDataPipelineServerActor.{ SpiderComicPipelineTask, SpiderDataPipelineTask }
import com.bdp.crawler.SpiderMonitorActor.GetQueueStatus
import com.bdp.crawler.SpiderSchedulerActor.{ UpdateScheduler, StopScheduler, StartScheduler }
import com.bdp.crawler.{ SpiderDataPipelineServerActor, DataPipelineActor, SpiderMonitorActor, SpiderSchedulerActor }
import com.bdp.daos.SpiderQueueListDAO
import com.bdp.models.SpiderQueueEntity
import com.bdp.routers._
import com.bdp.services.{ SpiderQueueMonitorServer, SpiderSchedulerServer }
import com.bdp.utils.{ Base64Utils, MediaTypesHelper }

import spray.routing._
import spray.routing.{ HttpService, Route }
import spray.httpx.SprayJsonSupport._

import scala.util.{ Failure, Success }

import com.bdp.routers.TestCase._
import com.bdp.routers.ResponseFormatMessage._
import com.bdp.routers.ResponseErrorMessage._

/**
 * Created by zhange on 15/10/22.
 *
 */

class RestInterface extends HttpService with Actor with PerRequestCreator with MediaTypesHelper
    with Base64Utils
    with ADemoRouter
    with ChannelListRouter
    with NewsListRouter
    with SpiderMonitorListRouter
    with SpiderQueueListRouter
    with SpiderSourceListRouter
    with SpiderProjectListRouter
    with ScrapydControllerRouter
    with ComicListRouter {

  implicit def actorRefFactory = context

  /**
   * 数据处理任务上报接口.
   *
   */
  val dataPipelineActor = system.actorOf(Props[DataPipelineActor], "DataPipelineActor")
  def callDataPipelineServer(message: RestMessage): Route =
    ctx => perRequest(ctx, Props(new SpiderDataPipelineServerActor(dataPipelineActor)), message)
  val spiderDataPipelineRoute: Route = pathPrefix("bdp" / "spider" / "pipeline") {
    get {
      path("task" / Segment) { taskKey =>
        callDataPipelineServer {
          SpiderDataPipelineTask(decodeBase64(taskKey))
        }
      }
    } ~
      get {
        path("comic" / Segment) { taskKey =>
          callDataPipelineServer {
            SpiderComicPipelineTask(taskKey)
          }
        }
      }
  }

  /**
   * SpiderSchelduler调度接口
   *
   */
  //  val schedulerActor = context.actorOf(Props[SpiderSchedulerActor], "SpiderSchedulerActor")
  //  def callSpiderScheldulerServer(message: RestMessage): Route =
  //    ctx => perRequest(ctx, Props(new SpiderSchedulerServer(schedulerActor)), message)
  //  import SpiderSchedulerActor._
  //
  //  val spiderSchedulerRoute: Route = pathPrefix("spider" / "scheduler") {
  //    get {
  //      path("start" / Segment) { queueName =>
  //        callSpiderScheldulerServer {
  //          StartScheduler(queueName)
  //        }
  //      } ~
  //        path("stop" / Segment) { queueName =>
  //          callSpiderScheldulerServer {
  //            StopScheduler(queueName)
  //          }
  //        } ~
  //        path("update" / Segment) { queueName =>
  //          callSpiderScheldulerServer {
  //            UpdateScheduler(queueName)
  //          }
  //        }
  //    }
  //  }

  /**
   * 抓取队列状态(长度)查询接口
   *
   */
  val queueMonitorActor = context.actorOf(Props[SpiderMonitorActor], "SpiderMonitorActor")
  def callSpiderQueueMonitorServer(message: RestMessage): Route =
    ctx => perRequest(ctx, Props(new SpiderQueueMonitorServer(queueMonitorActor)), message)
  val spiderQueueMonitorRoute: Route = path("spider" / "queues" / "status") {
    getJson {
      callSpiderQueueMonitorServer { GetQueueStatus }
    }
  }

  val pingRoute: Route = path("ping") {
    complete("ping ping")
  }

  val testRemote = context.actorOf(Props[RemoteProxyServerActor], "RemoteProxyServerActor")

  def receive = runRoute(route)

  val route: Route = {
    demoOperations ~
      channelListOperations ~
      newsListOperations ~
      spiderMonitorListOperations ~
      spiderQueueListOperations ~
      spiderSourceListOperations ~
      spiderProjectListOperations ~
      scrapydControllerOperations ~
      //      spiderSchedulerRoute ~
      spiderQueueMonitorRoute ~
      spiderDataPipelineRoute ~
      comicOperations ~
      pingRoute
  }
}