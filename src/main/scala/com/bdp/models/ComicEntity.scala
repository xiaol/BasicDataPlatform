package com.bdp.models

import spray.json.DefaultJsonProtocol

/**
 * Created by zhange on 15/12/15.
 *
 */

case class ComicEntity(
  comicUrl: String,
  comicId: Long,
  downloadUrl: String,
  name: Option[String] = None,
  originName: Option[String] = None,
  author: String,
  category: Option[String] = None,
  areaLocation: Option[String] = None,
  tags: Option[List[String]] = None,
  summary: Option[String] = None,
  pubStatus: Int,
  lastUpdateDate: Option[String] = None,
  titleImage: Option[String] = None,
  popularity: Option[String] = None,
  chapterNum: Int)

object ComicEntity extends DefaultJsonProtocol {
  implicit val comicEntityFormat = jsonFormat15(ComicEntity.apply)
}