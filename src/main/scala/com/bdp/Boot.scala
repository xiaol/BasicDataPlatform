package com.bdp

import akka.actor.{ ActorSystem, Props }
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import spray.can.Http
import scala.concurrent.duration._

import com.bdp.utils.Config
import com.bdp.rest.RestInterface

/**
 * Created by zhange on 15/10/22.
 *
 */

object Boot extends App with Config {

  implicit val system = ActorSystem("ROOT")
  val service = system.actorOf(Props[RestInterface], "REST")

  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(20.seconds)

  IO(Http).ask(Http.Bind(service, interface = httpHost, port = httpPort))
    .mapTo[Http.Event]
    .map {
      case Http.Bound(address) =>
        println(s"REST interface bound to $address")
      case Http.CommandFailed(cmd) =>
        println("REST interface could not bind to " +
          s"$httpHost:$httpPort, ${cmd.failureMessage}")
        system.shutdown()
    }
}
