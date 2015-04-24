package org.miker.seurat

import org.bytedeco.javacpp.opencv_core.Rect

case class ProcessedImage (
  path: String,
  crop: Rect,
  lab: Lab,
  rgb: Rgb
)

case class Lab (l: Int, a: Int, b: Int)
case class Rgb (r: Int, g: Int, b: Int)