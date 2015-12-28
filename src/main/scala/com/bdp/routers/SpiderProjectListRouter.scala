package com.bdp.routers

import com.bdp.daos.SpiderProjectListDao
import com.bdp.models.{ SpiderProjectEntity, SpiderQueueEntity }
import com.bdp.utils.MediaTypesHelper
import spray.routing._

import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global

import spray.httpx.SprayJsonSupport._

import spray.http.StatusCodes._

/**
 * Created by zhange on 15/11/16.
 *
 */

trait SpiderProjectListRouter extends HttpService with MediaTypesHelper with SpiderProjectListDao {

  val spiderProjectListOperations: Route = projectPostRoute ~ projectReadRoute ~ projectPutRoute ~ projectDeleteRoute

  def projectReadRoute: Route = getJson {
    path("spider" / "projects") {
      onComplete(getSpiderProjectList()) {
        case Success(projects) if projects.nonEmpty => complete(projects)
        case Success(empty)                         => complete(NotFound, "Projects not found")
        case Failure(ex)                            => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
    } ~
      path("spider" / "projects" / "pname" / Segment) { projectName =>
        onComplete(getSpiderProjectByName(projectName)) {
          case Success(project) if project.nonEmpty => complete(project)
          case Success(None)                        => complete(NotFound, s"Project not found with ProjectName: $projectName")
          case Failure(ex)                          => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
  }

  def projectPostRoute: Route = postJson {
    path("spider" / "projects") {
      entity(as[SpiderProjectEntity]) { spiderProjectEntity =>
        onComplete(insertSpiderProject(spiderProjectEntity)) {
          case Success(project) => complete(project)
          case Failure(ex)      => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }
  }

  def projectPutRoute: Route = putJson {
    path("spider" / "projects") {
      entity(as[SpiderProjectEntity]) { spiderProjectEntity =>
        onComplete(updateSpiderProjectByName(spiderProjectEntity.pname, spiderProjectEntity)) {
          case Success(project) if project.nonEmpty => complete(project)
          case Success(None)                        => complete(NotFound, s"Not found with QueueName: ${spiderProjectEntity.pname}")
          case Failure(ex)                          => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }
  }

  def projectDeleteRoute: Route = deleteJson {
    path("spider" / "projects" / "pname" / Segment) { projectName =>
      onComplete(deleteSpiderProjectByName(projectName)) {
        case Success(1)  => complete(OK, "Success")
        case Success(x)  => complete(NotFound, s"Not found with ProjectName: $projectName")
        case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
    }
  }

}
