package com.bdp.services

import akka.actor.ActorSystem
import com.bdp.daos.SpiderProjectListDao
import spray.client.pipelining._

import scala.concurrent.Future

/**
 * Created by zhange on 15/11/16.
 *
 * 监听接口: 查询所有工程和各个工程包含的爬虫(查询SpiderQueueList表)
 * 监听接口: 查询各个工程下已部署的爬虫和运行状态(转发Scrapyd接口)
 * 监听接口: (按 爬虫/工程)启动/关闭爬虫(转发Scrapyd接口)
 * 监听接口: 查询爬虫日志
 *
 */

trait SpiderScrapydControllerServer extends SpiderProjectListDao {

  implicit val system: ActorSystem = ActorSystem()

  def listProjects(serverUrl: String): Future[String] = {
    requestGet(s"http://$serverUrl/listprojects.json")
  }

  def listSpiders(serverUrl: String, projectName: String): Future[String] = {
    requestGet(s"http://$serverUrl/listspiders.json?project=$projectName")
  }

  def listJobs(serverUrl: String, projectName: String): Future[String] = {
    requestGet(s"http://$serverUrl/listjobs.json?project=$projectName")
  }

  def startSpider(serverUrl: String, projectName: String, spiderName: String): Future[String] = {
    requestGet(s"http://$serverUrl/listjobs.json?project=$projectName")
  }

  //  def startAllSpider(serverUrl: String, projectName: String)

  def stopSpider(serverUrl: String, projectName: String, spiderName: String): Future[String] = {
    requestGet(s"http://$serverUrl/listjobs.json?project=$projectName")
  }

  //  def stopAllSpider(serverUrl: String, projectName: String)

  def requestGet(url: String)(implicit system: ActorSystem): Future[String] = {
    import system.dispatcher
    val pipeline = sendReceive
    val responseFuture = pipeline { Get(url) }
    responseFuture.map(_.entity.asString)
  }

  def requestPost() = {}
}
