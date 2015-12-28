package com.bdp.utils

import java.sql
import java.util
import java.time
import java.text.SimpleDateFormat

import spray.json._

/**
 * Created by zhange on 15/10/27.
 *
 */

trait DateUtils {

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
  val timeFormat = new SimpleDateFormat("HH:mm:ss")
  val datetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  val datetimeFormat1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

  private def getCurrentDateTime(datetimeFormatter: SimpleDateFormat): String = {
    val cal = util.Calendar.getInstance()
    datetimeFormatter.format(cal.getTime)
  }

  /**
   * Get current datetime by formatter.
   */
  def getCurrentDate(): String = getCurrentDateTime(dateFormat)
  def getCurrentTime(): String = getCurrentDateTime(timeFormat)
  def getCurrentDatetime(): String = getCurrentDateTime(datetimeFormat)
  def getCurrentTimestamps(): String = util.Calendar.getInstance().getTimeInMillis.toString

  /**
   * Convert timestamps to datetime.
   */
  def convertTimestampsToDatetime(timestamps: Long): String = {
    datetimeFormat.format(new util.Date(timestamps))
  }

  /**
   * Convert datetime String to java.sql.{Timestamp, Time, Date}.
   */
  def date(str: String) = new sql.Date(dateFormat.parse(str).getTime)
  def time(str: String) = new sql.Time(timeFormat.parse(str).getTime)
  def datetime(str: String) = new sql.Timestamp(datetimeFormat.parse(str).getTime)
  //  def datetime(str: String) = new sql.Timestamp( (if (str.contains(".")) datetimeFormat1 else datetimeFormat) .parse(str).getTime)
}

object DateUtilsJsonProtocol extends DefaultJsonProtocol with DateUtils {
  import java.sql._

  implicit object TimestampJsonFormat extends RootJsonFormat[Timestamp] {
    //    def write(timestampObj: Timestamp) = JsString(timestampObj.getTime.toString)
    def write(timestampObj: Timestamp) = JsString(datetimeFormat.format(timestampObj.getTime))

    def read(value: JsValue) = value match {
      case JsString(timestampStr) => datetime(timestampStr)
      case _                      => throw new DeserializationException("Java.Timestamp.Time format err.")
    }
  }

  implicit object TimeJsonFormat extends RootJsonFormat[Time] {
    def write(timeObj: Time) = JsString(timeObj.toString)

    def read(value: JsValue) = value match {
      case JsString(timeStr) => time(timeStr)
      case _                 => throw new DeserializationException("Java.sql.Time format err.")
    }
  }

  implicit object DateJsonFormat extends RootJsonFormat[Date] {
    def write(dateObj: Date) = JsString(dateObj.toString)

    def read(value: JsValue) = value match {
      case JsString(dateStr) => date(dateStr)
      case _                 => throw new DeserializationException("Java.sql.Date format err.")
    }
  }

}