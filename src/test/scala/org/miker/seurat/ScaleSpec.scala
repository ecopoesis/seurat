package org.miker.seurat

import org.scalatest.{FlatSpec, Matchers}

class SquareCropCenterSpec extends FlatSpec with Matchers {

  "SquareCropCenter" should "crop a vertical image" in {
    val crop = Crop.squareCropCenter(100, 200, 50, 100)
    assert(crop.x == 0)
    assert(crop.y == 50)
    assert(crop.width == 100)
    assert(crop.height == 100)
  }

  it should "crop a vertical image with the center too close to the top" in {
    val crop = Crop.squareCropCenter(100, 200, 50, 1)
    assert(crop.x == 0)
    assert(crop.y == 0)
    assert(crop.width == 100)
    assert(crop.height == 100)
  }

  it should "crop a vertical image with the center too close to the bottom" in {
    val crop = Crop.squareCropCenter(100, 200, 50, 199)
    assert(crop.x == 0)
    assert(crop.y == 100)
    assert(crop.width == 100)
    assert(crop.height == 100)
  }

  it should "crop a horizontal image" in {
    val crop = Crop.squareCropCenter(200, 100, 100, 50)
    assert(crop.x == 50)
    assert(crop.y == 0)
    assert(crop.width == 100)
    assert(crop.height == 100)
  }

  it should "crop a horizontal image with the center too close to the left" in {
    val crop = Crop.squareCropCenter(200, 100, 1, 50)
    assert(crop.x == 0)
    assert(crop.y == 0)
    assert(crop.width == 100)
    assert(crop.height == 100)
  }

  it should "crop a horizontal image with the center too close to the right" in {
    val crop = Crop.squareCropCenter(200, 100, 199, 50)
    assert(crop.x == 100)
    assert(crop.y == 0)
    assert(crop.width == 100)
    assert(crop.height == 100)
  }
}
