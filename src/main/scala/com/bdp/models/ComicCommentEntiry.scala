package com.bdp.models

import spray.json._
import spray.json.DefaultJsonProtocol

/**
 * Created by zhange on 15/12/18.
 *
 */

case class ComicCommentEntiry(
  id: Option[Long] = None,
  comicUrl: String,
  commentId: Long,
  uid: Long,
  nickName: String,
  avatarUrl: String,
  pid: Long,
  comicId: Long,
  authorId: Long,
  author: String,
  content: String,
  createTime: Long,
  countReply: Long,
  up: Long,
  source: Long,
  place: String,
  ip: String,
  sourceName: String)

object ComicCommentEntiry extends DefaultJsonProtocol {
  implicit val comicCommentEntityFormat = jsonFormat18(ComicCommentEntiry.apply)
}

case class ComicComment(comment: ComicCommentEntiry, replies: List[ComicCommentEntiry])

object ComicComment extends DefaultJsonProtocol {
  implicit val comicCommentFormat = jsonFormat2(ComicComment.apply)
}