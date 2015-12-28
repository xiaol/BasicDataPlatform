package com.bdp.models

import org.joda.time._
import spray.json.DefaultJsonProtocol
import com.bdp.utils.JodaDateUtilsJsonProtocol._

/**
 * Created by zhange on 15/11/9.
 *
 */

case class SpiderSourceEntity(
  id: Option[Long] = None,
  createTime: LocalDateTime,
  sourceUrl: String,
  sourceName: String,
  channelName: String,
  channelId: Long,
  descr: Option[String] = None,
  queueName: String,
  frequency: Int,
  status: Int,
  taskConf: Option[Map[String, String]] = None,
  online: Int)

object SpiderSourceEntity extends DefaultJsonProtocol {
  implicit val spiderSourceEntityFormat = jsonFormat12(SpiderSourceEntity.apply)
}