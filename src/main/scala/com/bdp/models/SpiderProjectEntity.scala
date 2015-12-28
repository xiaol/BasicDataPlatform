package com.bdp.models

import spray.json.DefaultJsonProtocol

/**
 * Created by zhange on 15/11/16.
 *
 */

case class SpiderProjectEntity(pname: String, serverList: List[String])

object SpiderProjectEntity extends DefaultJsonProtocol {
  implicit val spiderProjectEntityFormat = jsonFormat2(SpiderProjectEntity.apply)
}