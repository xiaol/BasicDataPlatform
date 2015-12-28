package com.bdp.routers

import com.bdp.rest.RestMessage
import org.json4s.DefaultFormats
import spray.json._
import spray.json.DefaultJsonProtocol._

/**
 * Created by zhange on 15/12/21.
 *
 */

case class TestCase(key: String, value: String)

object TestCase extends DefaultJsonProtocol {
  implicit val TestCaseFormat = jsonFormat2(TestCase.apply)
}

case class ResponseFormatMessage[T](code: Int, message: String, data: T)

object ResponseFormatMessage extends DefaultJsonProtocol {
  implicit def responseFormatMessageFormat[T: JsonFormat] = jsonFormat3(ResponseFormatMessage.apply[T])
}

case class ResponseErrorMessage(code: Int, message: String)

object ResponseErrorMessage extends DefaultJsonProtocol {
  implicit def ResponseErrorMessageFormat = jsonFormat2(ResponseErrorMessage.apply)
}