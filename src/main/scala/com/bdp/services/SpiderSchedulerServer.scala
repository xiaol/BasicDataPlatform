package com.bdp.services

import akka.actor.{ ActorRef, Actor }
import com.bdp.crawler.SpiderSchedulerActor._
import com.bdp.rest.RestMessage

/**
 * Created by zhange on 15/11/17.
 *
 */

class SpiderSchedulerServer(schedulerActor: ActorRef) extends Actor {

  override def receive = {
    case startScheduler: StartScheduler   => schedulerActor ! startScheduler
    case stopScheduler: StopScheduler     => schedulerActor ! stopScheduler
    case updateScheduler: UpdateScheduler => schedulerActor ! updateScheduler

    case restMessage: RestMessage         => context.parent ! restMessage
    case _                                => println("errr")
  }

}
