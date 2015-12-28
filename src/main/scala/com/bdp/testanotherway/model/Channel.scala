package com.bdp.testanotherway.model

import spray.json.DefaultJsonProtocol

/**
 * Created by zhange on 15/11/14.
 *
 */

case class Channel(
  id: Option[Int] = None,
  cname: String,
  des: Option[String] = None,
  img: Option[String] = None,
  adrImg: Option[String] = None,
  iosImg: Option[String] = None,
  online: Int)

object Channel extends DefaultJsonProtocol {
  implicit val userFormat = jsonFormat7(Channel.apply)
}

