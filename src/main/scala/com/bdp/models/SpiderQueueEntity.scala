package com.bdp.models

import spray.json.DefaultJsonProtocol

/**
 * Created by zhange on 15/11/9.
 *
 */

case class SpiderQueueEntity(
  queueName: String,
  spiderName: String,
  projectName: String)

object SpiderQueueEntity extends DefaultJsonProtocol {
  implicit val spiderQueueEntityFormat = jsonFormat3(SpiderQueueEntity.apply)
}