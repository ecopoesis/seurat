import java.awt.image.BufferedImage
import java.io.{IOException, File}
import java.sql.DriverManager
import javax.imageio.ImageIO
import ch.qos.logback.classic.{Level, Logger}
import org.bytedeco.javacpp.{opencv_objdetect, Loader}
import org.bytedeco.javacpp.helper.opencv_core._
import org.bytedeco.javacpp.helper.opencv_core.CvArr
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_objdetect._
import org.bytedeco.javacv.CanvasFrame
import org.bytedeco.javacv.OpenCVFrameConverter.ToIplImage
import org.flywaydb.core.Flyway
import org.miker.seurat.{ProcessedImages, Rgb2Lab, Lab, Crop}
import org.miker.seurat.colorthief.ColorThief
import org.slf4j.LoggerFactory
import javax.swing.JFrame._
import org.bytedeco.javacpp.opencv_highgui._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacpp.helper.opencv_core.AbstractCvScalar._
import scalikejdbc.{ConnectionPoolSettings, ConnectionPool}
import scala.collection.JavaConversions._
import scalikejdbc._

object Main {
  Class.forName("org.sqlite.JDBC")

  val logger = LoggerFactory.getLogger(this.getClass)
  val root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
  root.asInstanceOf[Logger].setLevel(Level.INFO)

  val database = "jdbc:sqlite:data/processed_images.db"

  val storage = AbstractCvMemStorage.create

  val cascade = new CvHaarClassifierCascade(cvLoad(getClass.getResource("haarcascade_frontalface_alt.xml").getFile))
  //val profileCascade = new CvHaarClassifierCascade(cvLoad("/Users/miker/code/seurat/src/main/resources/data/haarcascades/haarcascade_profileface.xml"))

  val converter = new ToIplImage

  def main(args: Array[String]) = {
    migrateDatabase

    ConnectionPool.singleton("jdbc:sqlite:data/processed_images.db", "", "")

    val settings = ConnectionPoolSettings(
      initialSize = 1,
      maxSize = 1,
      connectionTimeoutMillis = 3000L,
      validationQuery = "select 1 from dual")

    if (args.length == 0) {
      System.exit(1)
    }

    val processedImages = if (args.length == 2) {
      processImages(args(1))
    } else {
      loadProcessedImages
    }

    println(processedImages.length)
   // val mosaic = createMosaic(args(0), processedImages)
  }

  def loadProcessedImages: Seq[ProcessedImages] = DB autoCommit { implicit session =>
      sql"select path, x, y, w, h, l, a, b from processed_images".map(rs =>
        ProcessedImages(
          rs.string("path"),
          new Rect(rs.int("x"), rs.int("y"), rs.int("w"), rs.int("h")),
          Lab(rs.int("l"), rs.int("a"), rs.int("b"))
        )).list.apply()
  }

  def processImages(directory: String): Seq[ProcessedImages] = {
    logger.info(s"Processing ${directory} for source images")

    val images = getFileTree(new File(directory)).filter(f => f.getName.endsWith(".jpeg") || f.getName.endsWith(".jpg"))

    logger.info(s"${images.length} images found to process...")
    var count = 0

    images.toList.map(f => {
      val i = imread(f.getAbsolutePath)
      if (i.empty()) {
        logger.error("Error reading image...")
        System.exit(0)
      }

      val r = squareCrop(i)

      val croppedImage = new Mat(i, r)
      val labColor = getColor(croppedImage)

      DB autoCommit { implicit session =>
        sql"INSERT OR REPLACE INTO processed_images (path, x, y, w, h, l, a, b) VALUES (${f.getAbsolutePath}, ${r.x}, ${r.y}, ${r.width}, ${r.height}, ${labColor.l}, ${labColor.a}, ${labColor.b})".update.apply()
      }

      count += 1
      if (count % 10 == 0) {
        logger.info(s"${count} images processed")
      }

      ProcessedImages(f.getAbsolutePath, r, labColor)
    })
  }

  def migrateDatabase = {
    val flyway = new Flyway
    flyway.setDataSource(database, "", "")
    flyway.migrate
  }

  def getColor(i: Mat): Lab = {
    val color = ColorThief.getColor(i, 1, false)
    Rgb2Lab.convert(color(0), color(1), color(2))
  }

  def squareCrop(i: Mat): Rect = {
    if (i.cols == i.rows) {
      new Rect(0, 0, i.cols, i.cols)
    }

    detectFaces(i) match {
      case Some(r) => r
      case None => centerCrop(i)
    }
  }

  def getFileTree(f: File): Stream[File] = f #:: (if (f.isDirectory) f.listFiles().toStream.flatMap(getFileTree) else Stream.empty)

  def detectFaces(i: Mat): Option[Rect] = {
    val grayImage = new Mat(i.cols, i.rows, CV_8U)
    cvtColor(i, grayImage, CV_BGR2GRAY)

    val min = if (i.cols < i.rows) i.cols / 10 else i.rows / 10
    val faces = cvHaarDetectObjects(grayImage.asIplImage, cascade, storage, 1.1, 3, CV_HAAR_DO_CANNY_PRUNING, cvSize(min, min), cvSize(0,0))

    // find the average center of the faces detected
    var xTotal, yTotal: Long = 0
    if (faces.total > 0) {
      for (f <- 0 until faces.total) {
        val r = new CvRect(cvGetSeqElem(faces, f))
        xTotal += r.x + (r.width / 2)
        yTotal += r.y + (r.height / 2)
        cvRectangle(i.asIplImage, cvPoint(r.x, r.y), cvPoint(r.x+r.width, r.y+r.height), RED, 1, CV_AA, 0)
      }
      Some(Crop.squareCropCenter(i.cols, i.rows, (xTotal / faces.total).toInt, (yTotal / faces.total).toInt))
    } else {
      None
    }
  }

  def centerCrop(i: Mat): Rect = {
    if (i.cols < i.rows) {
      new Rect(0, (i.rows - i.cols) / 2, i.cols, i.cols)
    } else {
      new Rect((i.cols - i.rows) / 2, 0, i.rows, i.rows)
    }
  }
}
