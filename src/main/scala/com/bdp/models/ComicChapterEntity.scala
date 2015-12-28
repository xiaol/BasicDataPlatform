package com.bdp.models

import spray.json.DefaultJsonProtocol

/**
 * Created by zhange on 15/12/15.
 *
 */

case class ComicChapterEntity(
  id: Option[Long] = None,
  chapterUrl: String,
  chapterOrder: Int,
  comicUrl: String,
  name: String,
  images: List[String])

object ComicChapterEntity extends DefaultJsonProtocol {
  implicit val comicChapterEntityFormat = jsonFormat6(ComicChapterEntity.apply)
}
