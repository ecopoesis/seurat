import java.awt.image.BufferedImage
import java.io.{IOException, File}
import javax.imageio.ImageIO
import org.bytedeco.javacpp.opencv_core.cvLoad
import org.bytedeco.javacpp.opencv_core.IplImage
import org.bytedeco.javacpp.opencv_objdetect.CvHaarClassifierCascade
import org.bytedeco.javacv.CanvasFrame
import org.slf4j.LoggerFactory
import javax.swing.JFrame._
import org.bytedeco.javacpp.opencv_highgui._


object Main {
  val logger = LoggerFactory.getLogger(this.getClass)

  //val defaultCascade = new CvHaarClassifierCascade(cvLoad(CASCADE_PATH + "haarcascade_frontalface_alt.xml"));
  //val profileCascade = new CvHaarClassifierCascade(cvLoad(CASCADE_PATH + "haarcascade_profileface.xml"));


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

    // Create image window named "My Image".
    //
    // Note that you need to indicate to CanvasFrame not to apply gamma correction,
    // by setting gamma to 1, otherwise the image will not look correct.
    val canvas = new CanvasFrame("My Image", 1)

    // Request closing of the application when the image window is closed
    canvas.setDefaultCloseOperation(EXIT_ON_CLOSE)

    // Show image on window
    canvas.showImage(image)
  }

  def load(filename: String): Option[BufferedImage] = {
    try {
      Some(ImageIO.read(new File(filename)))
    } catch {
      case e: IOException => {
        logger.error(s"error loading file ${filename}")
        None
      }
    }
  }

  def detectFaces(i: IplImage) = {

  }
}
