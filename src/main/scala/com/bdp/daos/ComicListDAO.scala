package com.bdp.daos

import com.bdp.models.ComicEntity
import com.bdp.utils.SlickPostgresConfig

import scala.concurrent.Future

/**
 * Created by zhange on 15/12/15.
 *
 */

trait ComicListTable extends SlickPostgresConfig {

  import driver.api._

  class ComicList(tag: Tag) extends Table[ComicEntity](tag, "comiclist") {

    def comicUrl = column[String]("comic_url", O.PrimaryKey)

    def comicId = column[Long]("comic_id")

    def downloadUrl = column[String]("download_url")

    def name = column[Option[String]]("name")

    def originName = column[Option[String]]("origin_name")

    def author = column[String]("author")

    def category = column[Option[String]]("category")

    def areaLocation = column[Option[String]]("area_location")

    def tags = column[Option[List[String]]]("tags")

    def summary = column[Option[String]]("summary")

    def pubStatus = column[Int]("pub_status")

    def lastUpdateDate = column[Option[String]]("last_update_date")

    def titleImage = column[Option[String]]("title_image")

    def popularity = column[Option[String]]("popularity")

    def chapterNum = column[Int]("chapter_num")

    def comicIdUnique = index("IDX_COMICID", comicId, unique = true)

    def * = (comicUrl, comicId, downloadUrl, name, originName, author, category, areaLocation, tags, summary, pubStatus, lastUpdateDate, titleImage, popularity, chapterNum) <> ((ComicEntity.apply _).tupled, ComicEntity.unapply)
  }

  protected val comicList = TableQuery[ComicList]

}

trait ComicListDAO extends ComicListTable {
  import driver.api._

  def getComicList(page: Int, offset: Int): Future[Seq[ComicEntity]] = {
    db.run(comicList.drop(page * offset).take(offset).result)
  }

  def getComicByUrl(comicUrl: String): Future[Option[ComicEntity]] = {
    db.run(comicList.filter(_.comicUrl === comicUrl).result.headOption)
  }

  def insertComic(comic: ComicEntity): Future[ComicEntity] = {
    db.run(comicList returning comicList += comic)
  }
}
