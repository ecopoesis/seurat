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
    val image = imread("data/IMG_0350.jpg")
    if (image.empty()) {
      // error handling
      // no image has been created...
      // possibly display an error message
      // and quit the application
      println("Error reading image...")
      System.exit(0)
    }

    detectFaces(image)

    // Create image window named "My Image".
    //
    // Note that you need to indicate to CanvasFrame not to apply gamma correction,
    // by setting gamma to 1, otherwise the image will not look correct.
    val canvas = new CanvasFrame("My Image", 1)

    // Request closing of the application when the image window is closed
    canvas.setDefaultCloseOperation(EXIT_ON_CLOSE)
    canvas.setCanvasSize(300, 400)

    println(s"x: ${image.cols} y: ${image.rows}")

    val frame = converter.convert(image)

    // Show image on window
    canvas.showImage(frame)

   }

  def crop(i: Mat) = {

  }


  def detectFaces(image: Mat) = {
    // We need a grayscale image in order to do the recognition, so we create a new image of the same size as the original one.
    //val grayImage = IplImage.create(originalImage.width(), originalImage.height(), IPL_DEPTH_8U, 1)
    val grayImage = new Mat(image.cols, image.rows, CV_8U)
    // We convert the original image to grayscale.
    cvtColor(image, grayImage, CV_BGR2GRAY)

    // We detect the faces.
    val faces = cvHaarDetectObjects(grayImage.asIplImage, cascade, storage, 1.1, 3, CV_HAAR_DO_CANNY_PRUNING, cvSize(0,0), cvSize(0,0))
    println(s"Faces: ${faces.total}")

    for (i <- 0 to faces.total) {
      val r = new CvRect(cvGetSeqElem(faces, i))
      cvRectangle(image.asIplImage, cvPoint(r.x, r.y), cvPoint(r.x+r.width, r.y+r.height), RED, 1, CV_AA, 0)
    }
  }
}
