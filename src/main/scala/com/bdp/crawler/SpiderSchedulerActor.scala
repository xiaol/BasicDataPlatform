package com.bdp.crawler

import redis.RedisClient
import spray.json._
import akka.actor.{ ActorRef, Props, Cancellable, Actor }
import akka.util.Timeout
import com.bdp.daos.SpiderSourceListDAO
import com.bdp.models.SpiderSourceEntity._
import com.bdp.rest.{ ResponseMessage, RestMessage }
import com.bdp.utils.{ RedisConfig, Config }

import scala.util.{ Random, Failure, Success }

/**
 * Created by zhange on 15/10/26.
 *
 * 这是一个有状态的抓取系统调度Actor,通过对SpiderMonitorActor的消息监听,管理各个爬虫的的调度状态,比如:开启/关闭/重启等.
 *
 * 父actor监听SpiderMonitorActor发送的消息,根据接收到的消息(爬虫名称/动作),执行对应的操作:
 *    开启一个调度器:检查调度器的name是否在运行列表中,如果没有则新建一个,如果有则检查状态是否有变更;
 *    关闭一个调度器:检查调度器的name是否在运行列表中,如果有,则根据PATH选择该actor并发送关闭消息,关闭成功后,在运行中列表移除该name
 *    修改调度器频率:关闭并根据新的属性重新创建调度器.
 *
 */

object SpiderSchedulerActor {
  case object SetupAllScheduler
  case object StopAllScheduler
  case class StartScheduler(queue: String) extends RestMessage
  case class StopScheduler(queue: String) extends RestMessage
  case class UpdateScheduler(queue: String) extends RestMessage

  case class Source(queue: String, rate: Int)
}

class SpiderSchedulerActor extends Actor with SpiderSourceListDAO with RedisConfig {

  implicit val system = context.system
  import system.dispatcher
  import SpiderSchedulerActor._
  import SubSchedulerActor._
  implicit val timeout: Timeout = 5000

  //  val rootPath = "akka://TestActor/user/SpiderSchedulerActor/"
  val rootPath = "akka://ROOT/user/REST/SpiderSchedulerActor/"

  val sourceList = Source("q1", 1) :: Source("q2", 2) :: Source("q5", 5) :: Nil

  // 初始化抓取任务: 查询SpiderSourceList表,按属性为每个爬虫创建一个子Actor,子Actor中创建定时器,定时Push抓取任务
  override def preStart(): Unit = {
    self ! SetupAllScheduler
  }

  // 消息来自REST接口对SpideSourceList表的操作
  // 监听消息: 关闭爬虫消息 => 关闭该爬虫的的子Actor,取消Push定时器
  // 监听消息: 开启爬虫消息 => 开启该爬虫的的子Actor,执行Push定时器
  // 监听消息: 重启爬虫消息 => 重启该爬虫的的子Actor,重启Push定时器

  override def receive = {
    case SetupAllScheduler => setupAllSubScheduler()
    //    case StopAllScheduler  => stopAllSubScheduler()
    //    case StartScheduler(queue) =>
    //      startSubScheduler(queue); sender ! ResponseMessage("Start Succes")
    //    case StopScheduler(queue) =>
    //      stopSubScheduler(queue); sender ! ResponseMessage("Stop Succes")
    //    case UpdateScheduler(queue) =>
    //      updateSubScheduler(queue); sender ! ResponseMessage("Update Succes")
    case _                 => println("Uncatch message.")
  }

  def setupAllSubScheduler() = {
    println("Extract each source and creat a sub actor with info.")

    getSpiderSourceList().map { sources =>
      sources.filter(_.status == 1).foreach { source =>
        println(source)
        val sourceSetInfo = redisClient.hset("spider_source:hashtable", source.sourceUrl, source.toJson.toString())
        sourceSetInfo.onComplete {
          case Success(re) =>
            println(s"Success, Hset ${source.sourceName} with $re.")
            println("createSubScheduler(source.queue, source.rate)")
            createSubScheduler(source.queueName, source.frequency, source.sourceUrl)
          case Failure(ex) => println(s"Failure, Hset ${source.sourceName} with $ex.")
        }
      }
    }
  }

  def startSubScheduler(queue: String) = {
    println(s"Select sub actor: $queue, start it with updated info.")

    context.actorSelection(s"$rootPath$queue").resolveOne().onComplete {
      case Success(actor) => println(s"Actor name: $queue is already exist!")
      case Failure(ex)    => createSubScheduler(queue, 1, "task")
    }
  }

  def updateSubScheduler(queue: String) = {
    println(s"Select sub actor: $queue, prestart it with updated info.")

    context.actorSelection(s"$rootPath$queue").resolveOne().onComplete {
      case Success(actor) =>
        println(s"Actor name: $queue is already exist!")
        println(s"Close it first!")
        actor ! ClosesubScheduler
        Thread.sleep(1000 * 5)

        // createSubScheduler(queue, 1)
        context.actorSelection(s"$rootPath$queue").resolveOne().onComplete {
          case Success(actor1) => println(s"Actor name: $queue is already exist!")
          case Failure(ex)     => createSubScheduler(queue, 1, "task")
        }
      case Failure(ex) => createSubScheduler(queue, 1, "task")
    }
  }

  def stopSubScheduler(queue: String) = {
    println(s"Select sub actor by queue: $queue, close it right now.")
    context.actorSelection(s"$rootPath$queue").resolveOne().onComplete {
      case Success(actor) => actor ! ClosesubScheduler
      case Failure(ex)    => println(s"Actor of name: $queue is not exist.")
    }
  }

  def stopAllSubScheduler() = {
    println("Select all spider setup source from postgresql with <SpiderSourceListDAO>.")
    println("Select all sub actor by queue, close all.")
  }

  def createSubScheduler(queue: String, rate: Int, task: String) = {
    context.actorOf(SubSchedulerActor.props(queue, rate, task), queue)
  }
}

object SubSchedulerActor {

  case object ClosesubScheduler
  case class PopSpiderTask(task: String)

  def props(queue: String, rate: Int, task: String): Props = Props(new SubSchedulerActor(queue: String, rate: Int, task: String))
}

class SubSchedulerActor(queue: String, rate: Int, task: String) extends Actor with RedisConfig {

  import SubSchedulerActor._

  implicit val system = context.system
  import system.dispatcher

  private var scheduler: Cancellable = _

  override def preStart(): Unit = {
    import scala.concurrent.duration._
    println(s"SubSchedulerActor setup with queue: $queue, rate: $rate, path: ${self.path}, pop task: $task")

    scheduler = context.system.scheduler.schedule(
      initialDelay = rate.minutes,
      interval = (rate + Random.nextInt(rate)).minutes,
      receiver = self,
      message = PopSpiderTask(task)
    )
  }

  override def postStop(): Unit = {
    println(s"cancel scheduler:$queue")
    scheduler.cancel()
  }

  def stopSelf() = context.stop(self)

  override def receive = {
    case ClosesubScheduler =>
      println(s"Stopping: $queue"); stopSelf()
    case PopSpiderTask(aTask) =>
      println(s"In SubSchedulerActor: $queue, Pop task: $aTask")
      redisClient.lpush(queue, aTask)
    case _ =>
  }
}