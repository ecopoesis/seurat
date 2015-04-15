package org.miker.seurat

import org.bytedeco.javacpp.opencv_core.Rect
import org.slf4j.LoggerFactory

object Crop {

  val logger = LoggerFactory.getLogger(this.getClass)

  def squareCropCenter(xMax: Int, yMax: Int, centerX: Int, centerY: Int): Rect = {
    logger.info(s"squareCropCenter - xMax: ${xMax}, yMax: ${yMax}, centerX: ${centerX}, centerY: ${centerY}")
    if (xMax < yMax) {
      val x = 0
      val y = if (centerY - (xMax / 2) < 0) {
        logger.info("center cropping a vertical image - too close to top")
        0
      } else if (centerY + (xMax / 2) > yMax) {
        logger.info("center cropping a vertical image - too close to bottom")
        yMax - xMax
      } else {
        logger.info("center cropping a vertical image")
        centerY - (xMax / 2)
      }
      val w = xMax
      val h = xMax
      new Rect(x, y, w, h)
    } else {
      val x = if (centerX - (yMax / 2) < 0) {
        logger.info("center cropping a horizontal image - too close to left")
        0
      } else if (centerX + (yMax / 2) > xMax) {
        logger.info("center cropping a horizontal image - too close to right")
        xMax - yMax
      } else {
        logger.info("center cropping a horizontal image")
        centerX - (yMax / 2)
      }
      val y = 0
      val w = yMax
      val h = yMax
      new Rect(x, y, w, h)
    }
  }

}
