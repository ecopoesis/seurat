package org.miker.seurat

import org.bytedeco.javacpp.opencv_core.Rect
import org.slf4j.LoggerFactory

object Crop {

  val logger = LoggerFactory.getLogger(this.getClass)

  def squareCropCenter(xMax: Int, yMax: Int, centerX: Int, centerY: Int): Rect = {
    if (xMax < yMax) {
      val x = 0
      val y = if (centerY - (xMax / 2) < 0) {
        0
      } else if (centerY + (xMax / 2) > yMax) {
        yMax - xMax
      } else {
        centerY - (xMax / 2)
      }
      val w = xMax
      val h = xMax
      new Rect(x, y, w, h)
    } else {
      val x = if (centerX - (yMax / 2) < 0) {
        0
      } else if (centerX + (yMax / 2) > xMax) {
        xMax - yMax
      } else {
        centerX - (yMax / 2)
      }
      val y = 0
      val w = yMax
      val h = yMax
      new Rect(x, y, w, h)
    }
  }

}
