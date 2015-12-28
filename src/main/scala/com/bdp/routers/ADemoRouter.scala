package com.bdp.routers

import akka.actor.{ Actor, Props }
import com.bdp.utils.MediaTypesHelper
import spray.routing._

import scala.language.postfixOps
import scala.concurrent.Future
import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by zhange on 15/11/15.
 *
 */

trait ADemoRouter extends HttpService with MediaTypesHelper {

  val demoOperations: Route = futureRoute ~ multipleMethodRoute ~ staticRoute ~ multipleParamRoute

  // nested route path
  //  val route = {
  //    pathPrefix("abc") {
  //      // complete with response 'abc'
  //      pathEnd { complete("abc") } ~ // don't forget the twiggles
  //        pathPrefix("def") {
  //          pathEnd { complete("def") } ~
  //            pathPrefix("ghi") {
  //              pathEnd { complete("ghi") } ~
  //                path("jkl") { // path already includes pathEnd, path(x) = pathPrefix(x ~ PathEnd)
  //                  complete("jkl")
  //                }
  //            }
  //        }
  //    }
  //  }

  def futureRoute: Route = getJson {
    path("test") {
      onComplete(getFutureResult) {
        case Success(futureResult) => complete(futureResult)
        case Failure(thr)          => complete("No future")
      }
    }
  }
  def getFutureResult: Future[String] = Future { "FutureResult" }

  def multipleMethodRoute: Route = path("test" / "method") {
    getJson {
      complete {
        "Wellcome to Spray server, this is a get client!"
      }
    } ~
      postJson {
        complete {
          "Wellcome to Spray server, this is a post client!"
        }
      }
  }

  def staticRoute: Route = getHtml {
    path("static") {
      getFromResource("web/static.html")
    }
  }

  //  class HelloActor extends Actor {
  //    override def receive = {
  //      case ctx: RequestContext =>
  //        ctx.complete("Hello, wellcome to HelloActor!")
  //    }
  //  }
  //  lazy val helloActor = context.actorOf(Props[HelloActor], "HelloActor")
  //  def actorRoute:Route = getJson {
  //    path("helloactor") {
  //      ctx => helloActor ! ctx
  //    }
  //  }

  def multipleParamRoute: Route = getJson {
    path("multiple" / "param") {
      parameters('uid.as[Int], 'aid.as[Int], 'url.as[String]?, 'key.as[String]?) {
        (uid, aid, url, key) =>
          complete {
            s"Param uid and aid is type of Int and required, " +
              s"url and key is type of String and not " +
              s"required: $uid, $aid, ${url.getOrElse("No url")}, ${key.getOrElse("No key")}"
          }
      }
    }
  }
}
