package com.bdp.services

import akka.actor.{ ActorRef, Actor }
import com.bdp.crawler.SpiderMonitorActor.{ QueueStatusPairs, GetQueueStatus }
import com.bdp.rest.ResponseMessage

import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by zhange on 15/11/20.
 *
 */

class SpiderQueueMonitorServer(spiderMonitorActor: ActorRef) extends Actor {

  override def receive = {
    case GetQueueStatus                     => spiderMonitorActor ! GetQueueStatus

    case queueStatusPairs: QueueStatusPairs => context.parent ! queueStatusPairs

    case responseMessage: ResponseMessage   => context.parent ! responseMessage

    case _                                  =>
  }

}
