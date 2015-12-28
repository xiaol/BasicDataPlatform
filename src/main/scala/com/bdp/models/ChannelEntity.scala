package com.bdp.models

import spray.json.DefaultJsonProtocol

/**
 * Created by zhange on 15/11/9.
 *
 */

case class ChannelEntity(
  id: Option[Long] = None,
  cname: String,
  des: Option[String] = None,
  img: Option[String] = None,
  adrImg: Option[String] = None,
  iosImg: Option[String] = None,
  online: Int)

object ChannelEntity extends DefaultJsonProtocol {
  implicit val channelEntityFormat = jsonFormat7(ChannelEntity.apply)
}