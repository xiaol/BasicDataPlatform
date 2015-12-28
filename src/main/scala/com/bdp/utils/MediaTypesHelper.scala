package com.bdp.utils

import spray.http.MediaTypes
import spray.routing._

/**
 * Created by zhange on 15/11/14.
 *
 */

trait MediaTypesHelper extends HttpService {

  def getJson(route: Route): Route = {
    get {
      respondWithMediaType(MediaTypes.`application/json`) {
        route
      }
    }
  }

  def postJson(route: Route): Route = {
    post {
      respondWithMediaType(MediaTypes.`application/json`) {
        route
      }
    }
  }

  def putJson(route: Route): Route = {
    put {
      respondWithMediaType(MediaTypes.`application/json`) {
        route
      }
    }
  }

  def deleteJson(route: Route): Route = {
    delete {
      respondWithMediaType(MediaTypes.`application/json`) {
        route
      }
    }
  }

  def getHtml(route: Route): Route = {
    get {
      respondWithMediaType(MediaTypes.`text/html`) {
        route
      }
    }
  }

}
