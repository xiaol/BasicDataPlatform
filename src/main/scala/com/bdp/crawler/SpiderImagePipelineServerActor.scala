package com.bdp.crawler

import java.io._
import java.security.MessageDigest

import scala.collection.mutable.ArrayBuffer
import scala.util.{ Failure, Success }
import scala.concurrent.duration._

import akka.event.Logging
import akka.actor.{ Props, ActorRef, Actor }
import spray.client.pipelining._

import com.sksamuel.scrimage.nio.JpegWriter
import com.sksamuel.scrimage._
import com.sksamuel.scrimage.Format.GIF

import com.aliyun.oss.OSSClient
import com.aliyun.oss.model.{ PutObjectResult, ObjectMetadata }

import com.bdp.rest.AccessTimeout
import com.bdp.utils.{ Base64Utils, DateUtils, UserAgents, ExceptionHandler }

import ImagePipelineActor._
import SpiderImageServerActor._

/**
 * Created by zhange on 15/10/26.
 *
 * 爬虫系统图片下载与上传.
 *
 */

object SpiderImageServerActor {
  case class ImageTaskError(imageUrl: String)
  case class ImageCropTask(imagePath: String)
  case class ImageDownloadTask(imageUrl: String)

  case class ListImageTaskList(taskList: List[String])
  case class ListImageOssTaskList(ossTaskList: ImageOssObjectList)
  case class ListImageResultList(imageList: List[String])

  case class ContentImageTaskList(taskList: List[String])
  case class ImageOssObject(originUrl: String, OssUrl: String)
  case class ImageOssObjectList(ossList: List[ImageOssObject])
}

class SpiderImagePipelineServerActor(imagePipelineActor: ActorRef) extends Actor {

  val log = Logging(context.system, this)

  override def receive = {
    case ContentImageTaskList(taskList) =>
      log.debug("Get ContentImageTaskList")
      val originalSender = sender()

      context.actorOf(Props(new Actor() {
        import context.dispatcher
        val timeoutMessager = context.system.scheduler.scheduleOnce(300.seconds, self, AccessTimeout)
        val imageOssObjectList: ArrayBuffer[ImageOssObject] = ArrayBuffer()
        val imageProcessErrList: ArrayBuffer[String] = ArrayBuffer()

        override def receive = {
          case AccessTimeout =>
            println("Image process timeout, shutdown!"); replyAndShutDown()
          case imageOssObject: ImageOssObject =>
            imageOssObjectList.append(imageOssObject); checkReadyReply()
          case ImageTaskError(imageUrl) => imageProcessErrList.append(imageUrl)
          case _                        =>
        }

        def checkReadyReply() = {
          println(s"ContentImageTaskList not Ready: all:${taskList.size}, done:${imageOssObjectList.size}, err:${imageProcessErrList.size}")
          if (taskList.size == (imageOssObjectList.size + imageProcessErrList.size)) replyAndShutDown()
        }

        def replyAndShutDown() = {
          originalSender ! ImageOssObjectList(imageOssObjectList.toList)
          timeoutMessager.cancel()
          context.stop(self)
        }

        taskList.foreach(imagePipelineActor ! ImageDownloadTask(_))
      }))

    case ListImageTaskList(taskList) =>
      log.debug("Get ListImageTaskList")
      val originalSender = sender()

      context.actorOf(Props(new Actor() {
        import context.dispatcher
        val timeoutMessager = context.system.scheduler.scheduleOnce(300.seconds, self, AccessTimeout)
        val imageOssObjectList: ArrayBuffer[ImageOssObject] = ArrayBuffer()
        val imageProcessErrList: ArrayBuffer[String] = ArrayBuffer()

        override def receive = {
          case AccessTimeout =>
            println("Image process timeout!"); replyAndShutDown()
          case imageOssObject: ImageOssObject =>
            imageOssObjectList.append(imageOssObject); checkReadyReply()
          case ImageTaskError(imageUrl) =>
            imageProcessErrList.append(imageUrl); checkReadyReply()
          case _ =>
        }

        def checkReadyReply() = {
          println(s"ListImageTaskList not Ready: all:${taskList.size}, done:${imageOssObjectList.size}, err:${imageProcessErrList.size}")
          if (taskList.size == (imageOssObjectList.size + imageProcessErrList.size)) replyAndShutDown()
        }

        def replyAndShutDown() = {
          originalSender ! ListImageResultList(for (imageOssObject <- imageOssObjectList.toList) yield imageOssObject.OssUrl)
          timeoutMessager.cancel()
          context.stop(self)
        }

        taskList.foreach(imagePipelineActor ! ImageDownloadTask(_))
      }))

    case ListImageOssTaskList(ossTaskList) =>
      log.debug("Get ListImageOssTaskList, resize with local file and upload")
      val originalSender = sender()

      context.actorOf(Props(new Actor() {
        import context.dispatcher
        val timeoutMessager = context.system.scheduler.scheduleOnce(300.seconds, self, AccessTimeout)
        val imageOssObjectList: ArrayBuffer[ImageOssObject] = ArrayBuffer()
        val imageProcessErrList: ArrayBuffer[String] = ArrayBuffer()

        override def receive = {
          case AccessTimeout =>
            println("Image process timeout!"); replyAndShutDown()
          case imageOssObject: ImageOssObject =>
            imageOssObjectList.append(imageOssObject); checkReadyReply()
          case ImageTaskError(imageUrl) =>
            imageProcessErrList.append(imageUrl); checkReadyReply()
          case _ =>
        }

        def checkReadyReply() = {
          println(s"ListImageTaskList not Ready: all:${ossTaskList.ossList.size}, done:${imageOssObjectList.size}, err:${imageProcessErrList.size}")
          if (ossTaskList.ossList.size == (imageOssObjectList.size + imageProcessErrList.size)) replyAndShutDown()
        }

        def replyAndShutDown() = {
          originalSender ! ListImageResultList(for (imageOssObject <- imageOssObjectList.toList) yield imageOssObject.OssUrl)
          timeoutMessager.cancel()
          context.stop(self)
        }

        ossTaskList.ossList.foreach { ossObject =>
          imagePipelineActor ! ImageCropTask(createImageLocalPathWithName(ossObject.OssUrl.split("/").last))
        }
      }))

    case _ => log.debug("Uncatch message.")
  }
}

object ImagePipelineActor {
  def createImageLocalPath(imgUrl: String, format: String): String = {
    val imgMd5 = MessageDigest.getInstance("MD5").digest(imgUrl.getBytes)
    List("src/main/resources/images/", Base64Utils.encodeBase64(imgMd5.toString).replace("=", ""), s".$format").mkString("")
  }
  def createImageLocalPathWithName(imageName: String): String = {
    List("src/main/resources/images/", imageName).mkString("")
  }
}

class ImagePipelineActor extends Actor with UserAgents with ExceptionHandler with Base64Utils {

  implicit val system = context.system
  import system.dispatcher

  override def receive = {
    case ImageDownloadTask(imageUrl) => download(imageUrl, sender())

    case ImageCropTask(imagePath) =>
      val resizedImage = cropJpgByRatio(imagePath)
      resizedImage match {
        case Some(newImagePath) =>
          val imgOssUrlOption = upload(newImagePath)
          imgOssUrlOption match {
            case Some(imgOssUrl) =>
              println(s"Upload success: $imgOssUrl")
              sender ! ImageOssObject(imagePath, imgOssUrl)
            case _ =>
              println(s"Upload failed: $imagePath")
              sender ! ImageTaskError(imagePath)
          }
        case None => sender ! ImageTaskError(imagePath)

        case _    =>
      }
  }

  def download(imageUrl: String, sender: ActorRef) = {
    val pipeline = (
      addHeader("User-agent", mobile)
      ~> sendReceive
    )
    val responseFuture = pipeline {
      Get(imageUrl)
    }
    responseFuture onComplete {
      case Success(response) =>
        println("Successful download image.")
        val localImage = process(imageUrl, response.entity.data.toByteArray)
        localImage match {
          case Some(imagePath) =>
            val imgOssUrlOption = upload(imagePath)
            imgOssUrlOption match {
              case Some(imgOssUrl) =>
                println(s"Upload success: $imgOssUrl")
                sender ! ImageOssObject(imageUrl, imgOssUrl)
              case _ =>
                println(s"Upload failed: $imageUrl")
                sender ! ImageTaskError(imageUrl)
            }
          case _ => println(s"Process failed: $imageUrl"); sender ! ImageTaskError(imageUrl)
        }

      case Failure(error) => println(s"Download failed: $imageUrl"); sender ! ImageTaskError(imageUrl)
    }
  }

  def process(imgUrl: String, imgBytes: Array[Byte]): Option[String] = {
    val imageFormat = FormatDetector.detect(imgBytes)
    println("imageFormat = " + imageFormat)

    imageFormat match {
      case Some(GIF) =>
        val imgName = createImageLocalPath(imgUrl, "gif")
        saveGif(imgName, imgBytes) match {
          case Some(imgPath) => Some(imgPath)
          case None          => None
        }
      case Some(_) =>
        val imgName = createImageLocalPath(imgUrl, "jpg")
        saveJpg(imgName, imgBytes) match {
          case Some(imgPath) => Some(imgPath)
          case None          => None
        }
      case _ => None
    }
  }

  def saveGif(imgName: String, imgBytes: Array[Byte]): Option[String] = {
    val out = new BufferedOutputStream(new FileOutputStream(imgName))
    try {
      out.write(imgBytes)
      Some(imgName)
    } catch safely {
      case ex0: Throwable =>
        println(ex0)
        None
    } finally {
      out.close()
    }
  }

  def saveJpg(imgName: String, imgBytes: Array[Byte]) = {
    try {
      val imgStream = new ByteArrayInputStream(imgBytes)
      val image = Image.fromStream(imgStream)
      image.output(imgName)(JpegWriter())
      Some(imgName)
    } catch safely {
      case ex0: Throwable =>
        println(ex0)
        None
    }
  }

  def cropJpgByRatio(localImage: String): Option[String] = {
    try {
      val imgStream = new FileInputStream(new File(localImage))
      val image = Image.fromStream(imgStream)

      val maxWidth = image.width
      val maxHeight = image.height

      // require(maxWidth >= maxHeight)

      val featSizeList = for {
        width <- Range(maxWidth, 1, -1)
        height <- Range(maxHeight, 1, -1)
        if (width.toDouble / height) == 4.0 / 3
        if width <= 600 && height <= 600
      } yield (width, height)
      val (featWidth, featHeight) = if (featSizeList.nonEmpty) featSizeList.head else (0, 0)
      (featWidth, featHeight) match {
        case (0, 0) => None
        case _ =>
          println(s"Get suit size: ($featWidth, $featHeight)")
          val newLocalPath = createImageLocalPath(localImage, "jpg")
          image.resizeTo(featWidth, featHeight).scaleTo(200, 150).output(newLocalPath)(JpegWriter())
          Some(newLocalPath)
      }
    } catch safely {
      case ex0: Throwable =>
        println(ex0)
        None
    }
  }

  def upload(imagePath: String): Option[String] = {
    val endpoint = "oss-cn-hangzhou.aliyuncs.com"
    val accessKeyId = "QK8FahuiSCpzlWG8"
    val accessKeySecret = "TGXhTCwUoEU4yNEGsfZSDvp0dNqw2p"
    val bucketName = "bdp-images"
    val client = new OSSClient(endpoint, accessKeyId, accessKeySecret)

    val imageName = imagePath.split("/").last

    try {
      val file = new File(imagePath)
      val content: InputStream = new FileInputStream(file)
      val meta: ObjectMetadata = new ObjectMetadata()
      meta.setContentLength(file.length())
      meta.setContentType("image/jpeg")

      val result: PutObjectResult = client.putObject(bucketName, imageName, content, meta)
      println(result.getETag)

      // Some(s"http://$bucketName.oss-cn-hangzhou.aliyuncs.com/$imageName")
      Some(s"http://bdp-pic.deeporiginalx.com/$imageName")
    } catch safely {
      case e: Throwable =>
        println(e)
        None
    }
  }
}
