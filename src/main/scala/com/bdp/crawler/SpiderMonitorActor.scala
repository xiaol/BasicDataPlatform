package com.bdp.crawler

import akka.actor.Actor
import com.bdp.crawler.SpiderMonitorActor.{ QueueStatusPairs, GetQueueStatus, QueueStatusPair }
import com.bdp.daos.SpiderQueueListDAO
import com.bdp.rest.{ ResponseMessage, RestMessage }
import com.bdp.utils.{ RedisConfig, Config }

import redis.RedisClient
import spray.json.DefaultJsonProtocol

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

/**
 * Created by zhange on 15/10/26.
 *
 * 对各个抓取源按小时段的已创建详情抓取任务统计/详情抓取成功统计/详情抓取失败统计 缓存记录 进行持久存储.
 *
 * 列表/详情抓取队列实时任务状态查询.
 *
 */

class SpiderMonitorActor extends Actor with RedisConfig with SpiderQueueListDAO {

  // 定时器1: 每10分钟查询SpiderQueueList表获取所有列表和详情队列(SpiderQueueActor),查询各个队列的任务数量,进行阈值检查并告警

  // 定时器2: 每小时,查询SpiderQueueList获取所有详情队列,按队列名称,
  // 查询缓存中各个详情队列的(已创建/抓取成功/抓取失败)缓存记录,持久存储,同时对抓取失败做阈值检查并告警

  implicit val system = context.system
  import system.dispatcher

  //  val redis = RedisClient(redisHost, redisPort, redisPassword)

  override def receive = {
    case GetQueueStatus => sender ! getSpiderQueueStatus()
    //      getQueueStatus().onComplete {
    //      case Success(res) => sender ! res
    //      case Failure(ex)  => sender ! ResponseMessage("Get QueueStatus Err.")
    //    }
    case _              =>
  }

  def getQueueStatus(): Future[List[QueueStatusPair]] = {

    val queues: ArrayBuffer[String] = ArrayBuffer()

    getSpiderQueueList().map { queueTables =>
      queueTables.foreach { listQueue =>
        queues ++= listQueue.queueName :: listQueue.queueName.replace("list_", "content_") :: Nil
      }
    }
    val status: List[Future[Long]] = for (queue <- queues.toList) yield redisClient.llen(queue)
    val statusList = Future.sequence(status)

    val queueStatusPairList = statusList.map { listStatus =>
      for {
        (queue, statu) <- queues.toList.zip(listStatus)
      } yield QueueStatusPair(queue, statu)
    }

    println(queueStatusPairList)
    queueStatusPairList
  }

  def getSpiderQueueStatus(): QueueStatusPairs = {
    val queueListResult = Await.result(getSpiderQueueList(), 10.second)
    val queueList: ArrayBuffer[String] = ArrayBuffer()
    queueListResult.foreach { queue =>
      queueList.append(queue.queueName)
      queueList.append(queue.queueName.replace("list_", "content_"))
    }
    val statuFutureList: List[Future[Long]] = for {
      queueName <- queueList.toList
    } yield redisClient.llen(queueName)

    val statusList = Future.sequence(statuFutureList)
    val status = Await.result(statusList, 10.second)

    val queueStatusPairs = for {
      (queue, length) <- queueList.toList.zip(status)
    } yield QueueStatusPair(queue, length)

    QueueStatusPairs(queueStatusPairs)
  }
}

object SpiderMonitorActor {

  case object GetQueueStatus extends RestMessage

  case class QueueStatusPair(queue: String, length: Long) extends RestMessage
  case class QueueStatusPairs(queueStatusPairs: List[QueueStatusPair]) extends RestMessage
  //  case class QueueStatusPairs(queueStatusPairs: Future[List[QueueStatusPair]])

  //  implicit val queueStatusPairFormat = jsonFormat2(QueueStatusPair.apply)
  //  implicit val queueStatusPairsFormat = jsonFormat1(QueueStatusPairs.apply)

  object QueueStatusPair extends DefaultJsonProtocol {
    implicit val queueStatusPairFormat = jsonFormat2(QueueStatusPair.apply)
  }

  object QueueStatusPairs extends DefaultJsonProtocol {
    implicit val queueStatusPairsFormat = jsonFormat1(QueueStatusPairs.apply)
  }
}

