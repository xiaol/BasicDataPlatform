package com.bdp.routers

import com.bdp.daos.ChannelListDAO
import com.bdp.models.ChannelEntity
import com.bdp.utils.MediaTypesHelper

import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport._
import spray.routing.{ Route, HttpService }

import scala.language.postfixOps
import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by zhange on 15/11/14.
 *
 */

trait ChannelListRouter extends HttpService with MediaTypesHelper with ChannelListDAO {

  val channelListOperations: Route = channelPostRoute ~ channelReadRoute ~ channelPutRoute ~ channelDeleteRoute ~ channelReadBDPRoute

  def channelReadBDPRoute: Route = getJson {
    path("bdp" / "news" / "channels") {
      parameters('url.as[Int]?) { onlineFlag =>
        onComplete(getChannelsByOline(onlineFlag.getOrElse(1))) {
          //        onComplete(getChannelsOnlineCompiled(onlineFlag.getOrElse(1))) {
          case Success(channels) if channels.nonEmpty => complete(ResponseFormatMessage(0, "success", channels))
          case Success(empty)                         => complete(ResponseErrorMessage(1, s"Channels not found with online flag: $onlineFlag"))
          case Failure(ex)                            => complete(ResponseErrorMessage(2, s"An error occurred: ${ex.getMessage}"))
        }
      }
    }
  }

  def channelReadRoute: Route = getJson {
    path("channels") {
      onComplete(getChannelList()) {
        case Success(channels) if channels.nonEmpty => complete(channels)
        case Success(empty)                         => complete(NotFound, "Channels not found")
        case Failure(ex)                            => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
    } ~
      path("channels" / "id" / LongNumber) { channelId =>
        onComplete(getChannelById(channelId)) {
          case Success(channel) if channel.nonEmpty => complete(channel)
          case Success(None)                        => complete(NotFound, s"Channel not found with ID: $channelId")
          case Failure(ex)                          => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      } ~
      path("channels" / "name" / Segment) { channelName =>
        onComplete(getChannelByName(channelName)) {
          case Success(channel) if channel.nonEmpty => complete(channel)
          case Success(None)                        => complete(NotFound, s"Channel not found with ChannelName: $channelName")
          case Failure(ex)                          => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      } ~
      path("channels" / "online" / IntNumber) { onlineFlag =>
        onComplete(getChannelsByOline(onlineFlag)) {
          case Success(channels) if channels.nonEmpty => complete(channels)
          case Success(empty)                         => complete(NotFound, s"Channel not found with OnlineFlag: $onlineFlag")
          case Failure(ex)                            => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
  }

  def channelPostRoute: Route = postJson {
    path("channels") {
      entity(as[ChannelEntity]) { channelEntity =>
        onComplete(insertChannel(channelEntity)) {
          case Success(channel) => complete(channel)
          case Failure(ex)      => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
        }
      }
    }
  }

  def channelPutRoute: Route = putJson {
    path("channels") {
      entity(as[ChannelEntity]) { channelEntity =>
        if (channelEntity.id.isEmpty) { complete(BadRequest, "Must provide ID when updating.") }
        else {
          onComplete(updateChannelById(channelEntity.id.get, channelEntity)) {
            case Success(channel) if channel.nonEmpty => complete(channel)
            case Success(None)                        => complete(NotFound, s"Not found with ID: ${channelEntity.id.get}")
            case Failure(ex)                          => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
          }
        }
      }
    }
  }

  def channelDeleteRoute: Route = deleteJson {
    path("channels" / "id" / LongNumber) { channelId =>
      onComplete(deleteChannelById(channelId)) {
        case Success(1)  => complete(OK, "Success")
        case Success(x)  => complete(NotFound, s"Not found with ID: $channelId")
        case Failure(ex) => complete(InternalServerError, s"An error occurred: ${ex.getMessage}")
      }
    }
  }
}
