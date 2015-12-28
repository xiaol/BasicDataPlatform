package com.bdp.models

import org.joda.time._
import spray.json.DefaultJsonProtocol
import com.bdp.utils.JodaDateUtilsJsonProtocol._

/**
 * Created by zhange on 15/11/9.
 *
 */

case class SpiderMonitorEntity(
  id: Option[Long] = None,
  queueName: String,
  dateDay: LocalDate,
  dateHour: Int,
  createList: Option[List[String]] = None,
  succesList: Option[List[String]] = None,
  failedList: Option[List[String]] = None)

object SpiderMonitorEntity extends DefaultJsonProtocol {
  implicit val spiderMonitorEntityFormat = jsonFormat7(SpiderMonitorEntity.apply)
}