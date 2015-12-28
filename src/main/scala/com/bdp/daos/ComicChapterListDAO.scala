package com.bdp.daos

import com.bdp.models.ComicChapterEntity
import com.bdp.utils.SlickPostgresConfig

import scala.concurrent.Future

/**
 * Created by zhange on 15/12/15.
 *
 */

trait ComicChapterListTable extends SlickPostgresConfig {

  import driver.api._

  class ComicChapterList(tag: Tag) extends Table[ComicChapterEntity](tag, "comicchapterlist") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def chapterUrl = column[String]("chapter_url")

    def chapterOrder = column[Int]("chapter_order")

    def comicUrl = column[String]("comic_url")

    def name = column[String]("name")

    def images = column[List[String]]("images")

    def * = (id.?, chapterUrl, chapterOrder, comicUrl, name, images) <> ((ComicChapterEntity.apply _).tupled, ComicChapterEntity.unapply)
  }

  protected val comicChapterList = TableQuery[ComicChapterList]
}

trait ComicChapterListDAO extends ComicChapterListTable {
  import driver.api._

  def getComicChapterListByUrl(comicUrl: String, page: Int, offset: Int): Future[Seq[ComicChapterEntity]] = {
    db.run(comicChapterList.filter(_.comicUrl === comicUrl).sortBy(_.chapterOrder).drop(page * offset).take(offset).result)
  }

  def insertComicChapter(comicChapter: ComicChapterEntity): Future[ComicChapterEntity] = {
    db.run(comicChapterList returning comicChapterList += comicChapter)
  }
}
