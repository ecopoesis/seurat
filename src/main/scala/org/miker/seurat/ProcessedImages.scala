package org.miker.seurat

import org.bytedeco.javacpp.opencv_core.Rect

case class ProcessedImages (
  path: String,
  crop: Rect,
  color: Lab
)

case class Lab (l: Int, a: Int, b: Int)