package com.bdp.crawler

import akka.actor.Actor
import spray.client.pipelining._
import com.bdp.models.{ ComicEntity, NewsEntity }
import com.bdp.models.NewsEntity._
import com.bdp.models.ComicEntity._
import spray.httpx.SprayJsonSupport._

import scala.util.{ Failure, Success }

/**
 * Created by zhange on 15/12/14.
 *
 */

class ElasticPipelineActor extends Actor {

  implicit val system = context.system
  import system.dispatcher

  override def receive = {
    case newsEntity: NewsEntity   => postNewsToElastic(newsEntity)
    case comicEntity: ComicEntity => postComicToElastic(comicEntity)
    case _                        =>
  }

  def postNewsToElastic(newsEntity: NewsEntity) = {
    val elasticPostApiForNews = "http://120.27.162.230:9200/news/fulltext/"
    val pipeline = sendReceive
    val responseFuture = pipeline {
      Post(elasticPostApiForNews, newsEntity)
    }
    responseFuture onComplete {
      case Success(response) => println("Insert elasticsearch success.")
      case Failure(error)    => println("Insert elasticsearch failed.")
    }
  }

  def postComicToElastic(comicEntity: ComicEntity) = {
    val elasticPostApiForComic = "http://120.27.162.230:9200/cartoon/fulltext/"
    val pipeline = sendReceive
    val responseFuture = pipeline {
      Post(elasticPostApiForComic, comicEntity)
    }
    responseFuture onComplete {
      case Success(response) => println("Insert elasticsearch success.")
      case Failure(error)    => println("Insert elasticsearch failed.")
    }
  }
}
