package com.bdp.routers

import com.bdp.daos.{ ComicCommentListDAO, ComicChapterListDAO, ComicListDAO }
import com.bdp.models.ComicCommentEntiry
import com.bdp.models.ComicCommentEntiry._

import com.bdp.utils.{ Base64Utils, MediaTypesHelper }
import spray.routing._

import scala.language.postfixOps
import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global

import spray.httpx.SprayJsonSupport._
import spray.http.StatusCodes._
import spray.json.DefaultJsonProtocol._

/**
 * Created by zhange on 15/12/16.
 *
 */

trait ComicListRouter extends HttpService with MediaTypesHelper with ComicListDAO with ComicChapterListDAO with ComicCommentListDAO with Base64Utils {

  val comicOperations: Route = comicReadBDPRoute ~ comicChapterReadBDPRoute ~ comicCommentReadBDPRoute ~ comicCommentPostBDPRoute

  def comicReadBDPRoute: Route = getJson {
    path("bdp" / "comic" / "list") {
      parameters('page.as[Int]?, 'offset.as[Int]?) {
        (page, offset) =>
          onComplete(getComicList(page.getOrElse(1) - 1, offset.getOrElse(20))) {
            case Success(comics) if comics.nonEmpty => complete(comics)
            case Success(_)                         => complete(NotFound, "News not found")
            case Failure(ex)                        => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
          }
      }
    }
  }

  def comicChapterReadBDPRoute: Route = getJson {
    path("bdp" / "comic" / "chapter") {
      parameters('curl.as[String], 'page.as[Int]?, 'offset.as[Int]?) {
        (comicUrl, page, offset) =>
          onComplete(getComicChapterListByUrl(decodeBase64(comicUrl), page.getOrElse(1) - 1, offset.getOrElse(20))) {
            case Success(chapters) if chapters.nonEmpty => complete(chapters)
            case Success(_)                             => complete(NotFound, "News not found")
            case Failure(ex)                            => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
          }
      }
    }
  }

  def comicCommentReadBDPRoute: Route = getJson {
    path("bdp" / "comic" / "comment") {
      parameters('curl.as[String], 'page.as[Int]?, 'offset.as[Int]?) {
        (comicUrl, page, offset) =>
          onComplete(getNestCommentListByComicUrl(decodeBase64(comicUrl), page.getOrElse(1) - 1, offset.getOrElse(20))) {
            case Success(comments) if comments.nonEmpty => complete(comments)
            case Success(_)                             => complete(NotFound, "News not found")
            case Failure(ex)                            => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
          }
      }
    }
  }

  def comicCommentPostBDPRoute: Route = postJson {
    path("bdp" / "comic" / "comment") {
      entity(as[ComicCommentEntiry]) { commentEntity =>
        onComplete(insertComment(commentEntity)) {
          case Success(comment) => complete(comment)
          case Failure(ex)      => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }
  }
}
