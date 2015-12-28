package com.bdp.testanotherway.router

import com.bdp.testanotherway.service.ChannelService
import spray.routing.{ Route, HttpService }

import scala.util.{ Failure, Success }

import spray.http.MediaTypes._
import spray.http.StatusCodes._

import spray.httpx.SprayJsonSupport._

import scala.concurrent.ExecutionContext.Implicits.global

trait ChannelRouter extends HttpService {

  //  val channelService: ChannelService

  val channelOperations: Route = readRoute ~ readAllRoute ~ deleteRoute

  def readRoute = path("channels" / IntNumber) { userId =>
    get {
      respondWithMediaType(`application/json`) {
        onComplete(ChannelService.get(userId)) {
          case Success(Some(channel)) => complete(channel)
          case Success(None)          => complete(NotFound, "User not found")
          case Failure(ex)            => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }
  }

  def readAllRoute = path("channels") {
    get {
      respondWithMediaType(`application/json`) {
        onComplete(ChannelService.getAll()) {
          case Success(channels) => complete(channels)
          case Failure(ex)       => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }

    }
  }

  def deleteRoute = path("channels" / IntNumber) { channelId =>
    delete {
      respondWithMediaType(`application/json`) {
        onComplete(ChannelService.delete(channelId)) {
          case Success(ok) => complete(OK)
          case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }
  }

}
