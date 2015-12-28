package com.bdp.routers

import com.bdp.services.SpiderScrapydControllerServer
import com.bdp.utils.MediaTypesHelper
import spray.routing.{ Route, HttpService }

import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by zhange on 15/11/16.
 *
 */

trait ScrapydControllerRouter extends HttpService with MediaTypesHelper with SpiderScrapydControllerServer {

  val scrapydControllerOperations: Route =
    listProjectsRoute ~
      listSpidersRoute ~
      listJobsRoute ~
      startSpiderRoute ~
      //      startSpidersRoute ~
      closeSpiderRoute
  //      closeSpidersRoute

  def listProjectsRoute: Route = getJson {
    path("spider" / "scrapyd" / "listprojects" / Segment) { serverUrl =>
      onComplete(listProjects(serverUrl)) {
        case Success(re) => complete(re)
        case Failure(ex) => complete("err")
      }
    }
  }

  def listSpidersRoute: Route = getJson {
    path("spider" / "scrapyd" / "listspiders" / Segment / Segment) { (serverUrl, projectName) =>
      onComplete(listSpiders(serverUrl, projectName)) {
        case Success(re) => complete(re)
        case Failure(ex) => complete("err")
      }
    }
  }

  def listJobsRoute: Route = getJson {
    path("spider" / "scrapyd" / "listjobs" / Segment / Segment) { (serverUrl, projectName) =>
      onComplete(listJobs(serverUrl, projectName)) {
        case Success(re) => complete(re)
        case Failure(ex) => complete("err")
      }
    }
  }

  def startSpiderRoute: Route = postJson {
    path("spider" / "scrapyd" / "schedule" / Segment / Segment / Segment) { (serverUrl, projectName, spiderName) =>
      complete("")
      // 按提供的 serverUrl, projectName, spiderName开启单个爬虫
    }
  }

  //  def startSpidersRoute: Route = postJson {
  //    path("spider" / "scrapyd" / "schedule" / Segment / Segment) { (serverUrl, projectName) =>
  //      complete("")
  //      // 按提供的 serverUrl, projectName 开启该project下所有爬虫
  //    }
  //  }

  def closeSpiderRoute: Route = postJson {
    path("spider" / "scrapyd" / "cancel" / Segment / Segment / Segment) { (serverUrl, projectName, spiderName) =>
      complete("")
      // 按提供的 serverUrl, projectName, spiderName关闭单个爬虫
    }
  }

  //  def closeSpidersRoute: Route = postJson {
  //    path("spider" / "scrapyd" / "cancel" / Segment / Segment) { (serverUrl, projectName) =>
  //      complete("")
  //      // 按提供的 serverUrl, projectName 关闭该project下所有爬虫
  //    }
  //  }
}
