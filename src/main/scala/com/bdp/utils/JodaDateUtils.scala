package com.bdp.utils

import org.joda.time.format.DateTimeFormat
import spray.json._
import org.joda.time._

/**
 * Created by zhange on 15/12/11.
 *
 */

trait JodaDateUtils {
  val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
  val timeFormat = DateTimeFormat.forPattern("HH:mm:ss")
  val datetimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

  /**
   * Get current datetime String by formatter.
   */
  def getCurrentDate(): String = LocalDateTime.now.toString(dateFormat)
  def getCurrentTime(): String = LocalDateTime.now.toString(timeFormat)
  def getCurrentDatetime(): String = LocalDateTime.now.toString(datetimeFormat)
  def getCurrentMilliseconds(): String = DateTime.now.getMillis.toString

  /**
   * Convert milliseconds to datetime.
   */
  def millisecondsToDatetime(milliseconds: Long): LocalDateTime = {
    dateTimeStrToDateTime(new LocalDateTime(milliseconds).toString(datetimeFormat))
  }

  def dateStrToDate(dateStr: String) = LocalDate.parse(dateStr, dateFormat)
  def timeStrToTime(timeStr: String) = LocalTime.parse(timeStr, timeFormat)
  def dateTimeStrToDateTime(datetimeStr: String) = LocalDateTime.parse(datetimeStr, datetimeFormat)
}

object JodaDateUtilsJsonProtocol extends DefaultJsonProtocol with JodaDateUtils {

  implicit object DateTimeJsonFormat extends RootJsonFormat[LocalDateTime] {
    def write(datetimeObj: LocalDateTime) = JsString(datetimeObj.toString(datetimeFormat))

    def read(value: JsValue) = value match {
      case JsString(timestampStr) => dateTimeStrToDateTime(timestampStr)
      case _                      => throw new DeserializationException("joda.time.LocalDateTime format err.")
    }
  }

  implicit object TimeJsonFormat extends RootJsonFormat[LocalTime] {
    def write(timeObj: LocalTime) = JsString(timeObj.toString(timeFormat))

    def read(value: JsValue) = value match {
      case JsString(timeStr) => timeStrToTime(timeStr)
      case _                 => throw new DeserializationException("joda.time.LocalTime format err.")
    }
  }

  implicit object DateJsonFormat extends RootJsonFormat[LocalDate] {
    def write(dateObj: LocalDate) = JsString(dateObj.toString(dateFormat))

    def read(value: JsValue) = value match {
      case JsString(dateStr) => dateStrToDate(dateStr)
      case _                 => throw new DeserializationException("joda.time.LocalDate format err.")
    }
  }
}