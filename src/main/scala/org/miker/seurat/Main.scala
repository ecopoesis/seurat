import java.awt.image.BufferedImage
import java.io.{IOException, File}
import javax.imageio.ImageIO
import org.bytedeco.javacpp.{opencv_objdetect, Loader}
import org.bytedeco.javacpp.helper.opencv_core._
import org.bytedeco.javacpp.helper.opencv_core.CvArr
import org.bytedeco.javacpp.opencv_core._
import org.bytedeco.javacpp.opencv_objdetect._
import org.bytedeco.javacv.CanvasFrame
import org.bytedeco.javacv.OpenCVFrameConverter.ToIplImage
import org.slf4j.LoggerFactory
import javax.swing.JFrame._
import org.bytedeco.javacpp.opencv_highgui._
import org.bytedeco.javacpp.opencv_imgproc._
import org.bytedeco.javacpp.helper.opencv_core.AbstractCvScalar._

object Main {
  val logger = LoggerFactory.getLogger(this.getClass)

  val storage = AbstractCvMemStorage.create

  val cascade = new CvHaarClassifierCascade(cvLoad(getClass.getResource("haarcascade_frontalface_alt.xml").getFile))
  //val profileCascade = new CvHaarClassifierCascade(cvLoad("/Users/miker/code/seurat/src/main/resources/data/haarcascades/haarcascade_profileface.xml"))

  val converter = new ToIplImage

  def main(args: Array[String]) = {
    // Read an image
    val i = imread("data/IMG_0350.jpg")
    if (i.empty()) {
      // error handling
      // no image has been created...
      // possibly display an error message
      // and quit the application
      println("Error reading image...")
      System.exit(0)
    }

    val r = crop(i)

    // Create image window named "My Image".
    //
    // Note that you need to indicate to CanvasFrame not to apply gamma correction,
    // by setting gamma to 1, otherwise the image will not look correct.
    val canvas = new CanvasFrame("My Image", 1)

    // Request closing of the application when the image window is closed
    canvas.setDefaultCloseOperation(EXIT_ON_CLOSE)
    canvas.setCanvasSize(400, 400)

    val cropped = new Mat(i, r)

    // Show image on window
    canvas.showImage(converter.convert(cropped))

   }

  def crop(i: Mat): Rect = {
    if (i.cols == i.rows) {
      new Rect(0, 0, i.cols, i.cols)
    }

    detectFaces(i) match {
      case Some(r) => r
      case None => centerCrop(i)
    }
  }

  def detectFaces(i: Mat): Option[Rect] = {
    val grayImage = new Mat(i.cols, i.rows, CV_8U)
    cvtColor(i, grayImage, CV_BGR2GRAY)

    val min = if (i.cols < i.rows) i.cols / 10 else i.rows / 10
    val faces = cvHaarDetectObjects(grayImage.asIplImage, cascade, storage, 1.1, 3, CV_HAAR_DO_CANNY_PRUNING, cvSize(min, min), cvSize(0,0))

    // find the average center of the faces detected
    var totalX, totalY: Long = 0
    if (faces.total > 0) {
      for (f <- 0 to faces.total) {
        val r = new CvRect(cvGetSeqElem(faces, f))
        totalX += r.x + (r.width / 2)
        totalY += r.y + (r.height / 2)
        cvRectangle(i.asIplImage, cvPoint(r.x, r.y), cvPoint(r.x+r.width, r.y+r.height), RED, 1, CV_AA, 0)
      }

      if (i.cols < i.rows) {
        val center = totalY / faces.total
        val x = 0
        val y = if (center - (i.cols / 2) < 0) {
          0
        } else if (center + (i.cols / 2) > i.rows) {
          i.rows - i.cols
        } else {
          (center - (i.cols / 2)).toInt
        }
        val w = i.cols
        val h = i.cols
        Some(new Rect(x, y, w, h))
      } else {
        val center = totalX / faces.total
        val x = if (center - (i.rows / 2) < 0) {
          0
        } else if (center + (i.rows / 2) > i.cols) {
          i.cols - i.rows
        } else {
          (center - (i.rows / 2)).toInt
        }
        val y = 0
        val w = i.rows
        val h = i.rows
        Some(new Rect(x, y, w, h))
      }
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
