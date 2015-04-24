import java.awt.image.BufferedImage
import java.io.{IOException, File}
import java.nio.ByteBuffer
import java.sql.DriverManager
import javax.imageio.ImageIO
import ch.qos.logback.classic.{Level, Logger}
import org.bytedeco.javacpp.indexer.UByteIndexer
import org.bytedeco.javacpp.{opencv_objdetect, Loader}
import org.bytedeco.javacpp.helper.opencv_core._
import org.bytedeco.javacpp.helper.opencv_core.CvArr
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_objdetect._
import org.bytedeco.javacv.CanvasFrame
import org.bytedeco.javacv.OpenCVFrameConverter.ToIplImage
import org.flywaydb.core.Flyway
import org.miker.seurat._
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

    // 3x4 source at 21" x 28", 300 dpi
    val mosaic = createMosaic(args(0), processedImages, 42, 56, 150)

   // val mosaic = imread("data/IMG_2089.jpg")
   // tint(mosaic, 255, 0, 0)

    imwrite("data/output.png", mosaic)

    val canvas = new CanvasFrame("Seurat", 1)
    canvas.setDefaultCloseOperation(EXIT_ON_CLOSE)
    canvas.setCanvasSize(900, 1200)
    canvas.showImage(converter.convert(mosaic))
  }

  def createMosaic(f: String, processedImages: Seq[ProcessedImage], xImages: Int, yImages: Int, pixelSize: Int): Mat = {
    // load original and resize
    val i = imread(f)
    resize(i, i, new Size(xImages, yImages), 0, 0, CV_INTER_LANCZOS4)

    val result = new Mat(yImages * pixelSize, xImages * pixelSize, CV_8UC3)

    val buffer = i.createBuffer.asInstanceOf[ByteBuffer]
    for (x <- 0 until xImages) {
      for (y <- 0 until yImages) {
        val b = buffer.get((x * i.channels) + (y * i.cols * i.channels)) & 0xFF
        val g = buffer.get((x * i.channels) + (y * i.cols * i.channels) + 1) & 0xFF
        val r = buffer.get((x * i.channels) + (y * i.cols * i.channels) + 2) & 0xFF
        val meta = findMatch(Rgb2Lab.convert(r, g, b), processedImages, deltaE)

        // load "pixel"
        val pixel = imread(meta.path)
        val cropped = new Mat(pixel, meta.crop)
        resize(cropped, cropped, new Size(pixelSize, pixelSize), 0, 0, CV_INTER_LANCZOS4)
        tint(cropped, Rgb(r, g, b))
        cropped.copyTo(result(new Rect(x * pixelSize, y * pixelSize, cropped.cols, cropped.rows)))
      }
    }

    result
  }

  def tint(image: Mat, color: Rgb) = {
    val i = image.createIndexer().asInstanceOf[UByteIndexer]

    val brg = new Array[Int](3)
    for (y <- 0 until image.rows) {
      for (x <- 0 until image.cols) {
        i.get(y, x, brg)
        brg(0) = ((brg(0) * .75) + (color.b * .25)).toInt
        brg(1) = ((brg(1) * .75) + (color.r * .25)).toInt
        brg(2) = ((brg(2) * .75) + (color.g * .25)).toInt
        i.put(y, x, brg, 0, brg.length)
      }
    }
  }

  def border(image: Mat, r: Int, g: Int, b: Int) = {
    for (y <- 0 until image.rows) {

    }

  }

  def deltaE(c1: Lab, c2: Lab): Double  = Math.sqrt(Math.pow(c2.l - c1.l, 2) + Math.pow(c2.a - c1.a, 2) + Math.pow(c2.b - c1.a, 2))

  def findMatch(color: Lab, processedImages: Seq[ProcessedImage], diff: (Lab, Lab) => Double): ProcessedImage = {
    var best = processedImages.head
    processedImages.foreach(pi => {
      if (diff(color, best.lab) > diff(color, pi.lab)) {
        best = pi
      }
    })
    best
  }
  
  def loadProcessedImages: Seq[ProcessedImage] = DB autoCommit { implicit session =>
    sql"select path, x, y, w, h, lab_l, lab_a, lab_b, rgb_r, rgb_g, rgb_b from processed_images".map(rs =>
      ProcessedImage(
        rs.string("path"),
        new Rect(rs.int("x"), rs.int("y"), rs.int("w"), rs.int("h")),
        Lab(rs.int("lab_l"), rs.int("lab_a"), rs.int("lab_b")),
        Rgb(rs.int("rgb_r"), rs.int("rgb_g"), rs.int("rgb_b"))
      )).list.apply()
  }

  def processImages(directory: String): Seq[ProcessedImage] = {
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
      val color = getColor(croppedImage)
      val labColor = Rgb2Lab.convert(color.r, color.g, color.b)

      DB autoCommit { implicit session =>
        sql"INSERT OR REPLACE INTO processed_images (path, x, y, w, h, lab_l, lab_a, lab_b, rgb_r, rgb_g, rgb_b) VALUES (${f.getAbsolutePath}, ${r.x}, ${r.y}, ${r.width}, ${r.height}, ${labColor.l}, ${labColor.a}, ${labColor.b}, ${color.r}, ${color.g}, ${color.b})".update.apply()
      }

      count += 1
      if (count % 10 == 0) {
        logger.info(s"${count} images processed")
      }

      ProcessedImage(f.getAbsolutePath, r, labColor, color)
    })
  }

  def migrateDatabase = {
    val flyway = new Flyway
    flyway.setDataSource(database, "", "")
    flyway.migrate
  }

  def getColor(i: Mat): Rgb = {
    val color = ColorThief.getColor(i, false)
    Rgb(color(0), color(1), color(2))
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
      new Rect(0, ((i.rows - i.cols) / 2), i.cols, i.cols)
    } else {
      new Rect(((i.cols - i.rows) / 2), 0, i.rows, i.rows)
    }
  }
}
