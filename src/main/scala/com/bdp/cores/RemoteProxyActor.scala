package com.bdp.cores

import akka.actor._
import akka.event.Logging
import com.bdp.cores.RemoteSchedulerActor._
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._
import akka.actor.Actor
import akka.actor.ActorIdentity
import akka.actor.ActorRef
import akka.actor.Identify
import akka.actor.ReceiveTimeout
import akka.actor.Terminated

/**
 * Created by zhange on 15/12/21.
 *
 */

case object Mess

class RemoteProxyServerActor extends Actor {

  implicit val proxySystem = ActorSystem("proxySystem", ConfigFactory.load("proxyserver"))

  import proxySystem.dispatcher

  val remotePath = "akka.tcp://BDP-RemoteSchedulerSystem@10.47.54.32:9000/user/SpiderSchedulerServerActor"
  val remoteSchedulerActor = proxySystem.actorOf(RemoteSchedulerActor.props(remotePath), "RemoteSchedulerActor")

  proxySystem.scheduler.schedule(5.second, 5.second, remoteSchedulerActor, Mess)

  override def receive = {
    case refreshScheduler: RefreshScheduler => remoteSchedulerActor ! refreshScheduler
    case startScheduler: StartScheduler     => remoteSchedulerActor ! startScheduler
    case stopScheduler: StopScheduler       => remoteSchedulerActor ! stopScheduler
    case StartAllScheduler                  => remoteSchedulerActor ! StartAllScheduler
    case StopAllScheduler                   => remoteSchedulerActor ! StopAllScheduler
    case _                                  =>
  }
}

object RemoteSchedulerActor {
  def props(remotePath: String): Props = Props(new RemoteSchedulerActor(remotePath: String))

  case class RefreshScheduler(queue: String, rate: Int, task: String)

  case class StartScheduler(queue: String)

  case class StopScheduler(queue: String)

  case object StartAllScheduler

  case object StopAllScheduler
}

class RemoteSchedulerActor(remotePath: String) extends Actor {

  val log = Logging(context.system, this)

  sendIdentifyRequest()

  def sendIdentifyRequest(): Unit = {
    context.actorSelection(remotePath) ! Identify(remotePath)
    import context.dispatcher
    context.system.scheduler.scheduleOnce(3.seconds, self, ReceiveTimeout)
  }

  def receive = identifying

  def identifying: Receive = {
    case ActorIdentity(`remotePath`, Some(remoteActor)) =>
      context.watch(remoteActor)
      context.become(active(remoteActor))
    case ActorIdentity(`remotePath`, None) => log.debug(s"Remote actor not available: $remotePath")
    case ReceiveTimeout =>
      log.debug("Accept timeout, check for remote."); sendIdentifyRequest()
    case _ => log.debug("Remote Actor Not ready yet")
  }

  def active(actor: ActorRef): Receive = {
    case Mess                               => actor ! "Message from LocalActor."
    case m: String                          => log.debug("LocalActor: Get reply from RemoteActor.")

    case refreshScheduler: RefreshScheduler => actor ! refreshScheduler
    case startScheduler: StartScheduler     => actor ! startScheduler
    case stopScheduler: StopScheduler       => actor ! stopScheduler
    case StartAllScheduler                  => actor ! StartAllScheduler
    case StopAllScheduler                   => actor ! StopAllScheduler

    case Terminated(`actor`) =>
      log.debug("LocalActor: RemoteSchedulerActor terminated")
      sendIdentifyRequest()
      context.become(identifying)
    case ReceiveTimeout => // ignore
  }
}
