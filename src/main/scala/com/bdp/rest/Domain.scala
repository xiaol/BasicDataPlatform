package com.bdp.rest

/**
 * Created by zhange on 15/10/22.
 *
 */

/*
  Messages for access timeout.
 */
case object AccessTimeout

/*
  Messages for http response.
  Any case class would use as a http response should extends trait RestMessage.
 */
trait RestMessage
case class Error(message: String) extends RestMessage
case class Validation(messgae: String)
case class ResponseMessage(key: String) extends RestMessage