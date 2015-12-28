package com.bdp.daos

/**
 * Created by zhange on 15/12/18.
 *
 */

import com.bdp.models.{ ComicComment, ComicCommentEntiry }
import com.bdp.utils.SlickPostgresConfig

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ Await, Future }
import scala.util.{ Failure, Success }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import spray.httpx.SprayJsonSupport._

trait ComicCommentListTable extends SlickPostgresConfig {

  import driver.api._

  class ComicCommentList(tag: Tag) extends Table[ComicCommentEntiry](tag, "comiccommentlist") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def comicUrl = column[String]("comic_url")

    def commentId = column[Long]("comment_id")

    def uid = column[Long]("uid")

    def nickName = column[String]("nickname")

    def avatarUrl = column[String]("avatar_url")

    def pid = column[Long]("pid")

    def comicId = column[Long]("comic_id")

    def authorId = column[Long]("author_id")

    def author = column[String]("author")

    def content = column[String]("content")

    def createTime = column[Long]("createtime")

    def countReply = column[Long]("count_reply")

    def up = column[Long]("up")

    def source = column[Long]("source")

    def place = column[String]("place")

    def ip = column[String]("ip")

    def sourceName = column[String]("source_name")

    def * = (id.?, comicUrl, commentId, uid, nickName, avatarUrl, pid, comicId, authorId, author, content, createTime, countReply, up, source, place, ip, sourceName) <> ((ComicCommentEntiry.apply _).tupled, ComicCommentEntiry.unapply)
  }

  protected val comicCommentList = TableQuery[ComicCommentList]

}

trait ComicCommentListDAO extends ComicCommentListTable {

  import driver.api._

  def insertComment(comicComment: ComicCommentEntiry): Future[ComicCommentEntiry] = {
    db.run(comicCommentList returning comicCommentList += comicComment)
  }

  def getCommentListByComicUrl(comicUrl: String, page: Int, offset: Int): Future[Seq[ComicCommentEntiry]] = {
    db.run(comicCommentList.filter(_.pid === 0.toLong).filter(_.comicUrl === comicUrl).sortBy(_.up.desc).drop(page * offset).take(offset).result)
  }

  def getNestCommentListByComicUrl(comicUrl: String, page: Int, offset: Int): Future[Seq[ComicComment]] = {

    val topLayerCommentList = comicCommentList.filter(_.pid === 0L).filter(_.comicUrl === comicUrl).sortBy(_.up.desc).drop(page * offset).take(offset)

    val secondLayerCommentList = for {
      topLayerComment <- topLayerCommentList.filter(_.countReply =!= 0L)
      secondLayerComment <- comicCommentList.filter(_.comicUrl === comicUrl).filter(_.pid === topLayerComment.commentId)
    } yield (topLayerComment, secondLayerComment)

    val res = for {
      topLayerCommentSeq <- db.run(topLayerCommentList.result)
      secondLayerCommentSeq <- db.run(secondLayerCommentList.result)
    } yield topLayerCommentSeq match {
      case x =>
        val temp: mutable.HashMap[Long, ArrayBuffer[ComicCommentEntiry]] = mutable.HashMap.empty[Long, ArrayBuffer[ComicCommentEntiry]]
        for (tup <- secondLayerCommentSeq) {
          if (temp.contains(tup._1.id.get).equals(true)) temp(tup._1.id.get).append(tup._2)
          else temp(tup._1.id.get) = ArrayBuffer(tup._2)
        }
        val res = for {
          topLayerComment <- topLayerCommentSeq
        } yield {
          ComicComment(topLayerComment, temp.getOrElse(topLayerComment.id.get, ArrayBuffer()).toList)
        }
        res
    }
    res
  }

}
