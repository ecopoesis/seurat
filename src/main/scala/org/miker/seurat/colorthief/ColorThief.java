/*
 * Java Color Thief
 * by Sven Woltmann, Fonpit AG
 *
 * http://www.androidpit.com
 * http://www.androidpit.de
 *
 * License
 * -------
 * Creative Commons Attribution 2.5 License:
 * http://creativecommons.org/licenses/by/2.5/
 *
 * Thanks
 * ------
 * Lokesh Dhakar - for the original Color Thief JavaScript version
 * available at http://lokeshdhakar.com/projects/color-thief/
 */

package org.miker.seurat.colorthief;

import org.bytedeco.javacpp.opencv_core;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class ColorThief
{

    private static final int DEFAULT_QUALITY = 10;
    private static final boolean DEFAULT_IGNORE_WHITE = true;

    /**
     * Use the median cut algorithm to cluster similar colors and return the
     * base color from the largest cluster.
     *
     * @param sourceImage
     *            the source image
     *
     * @return the dominant color as RGB array
     */
    public static int[] getColor(opencv_core.Mat sourceImage)
    {
        int[][] palette = getPalette(sourceImage, 5);
        if (palette == null)
        {
            return null;
        }
        return palette[0];
    }

    /**
     * Use the median cut algorithm to cluster similar colors and return the
     * base color from the largest cluster.
     *
     * @param sourceImage
     *            the source image
     * @param quality
     *            0 is the highest quality settings. 10 is the default. There is
     *            a trade-off between quality and speed. The bigger the number,
     *            the faster a color will be returned but the greater the
     *            likelihood that it will not be the visually most dominant
     *            color.
     * @param ignoreWhite
     *            if <code>true</code>, white pixels are ignored
     *
     * @return the dominant color as RGB array
     */
    public static int[] getColor(
            opencv_core.Mat sourceImage,
            int quality,
            boolean ignoreWhite)
    {
        int[][] palette = getPalette(sourceImage, 5, quality, ignoreWhite);
        if (palette == null)
        {
            return null;
        }
        int[] dominantColor = palette[0];
        return dominantColor;
    }

    /**
     * Use the median cut algorithm to cluster similar colors.
     *
     * @param sourceImage
     *            the source image
     * @param colorCount
     *            the size of the palette; the number of colors returned
     *
     * @return the palette as array of RGB arrays
     */
    public static int[][] getPalette(opencv_core.Mat sourceImage, int colorCount)
    {
        MMCQ.CMap cmap = getColorMap(sourceImage, colorCount);
        if (cmap == null)
        {
            return null;
        }
        return cmap.palette();
    }

    /**
     * Use the median cut algorithm to cluster similar colors.
     *
     * @param sourceImage
     *            the source image
     * @param colorCount
     *            the size of the palette; the number of colors returned
     * @param quality
     *            0 is the highest quality settings. 10 is the default. There is
     *            a trade-off between quality and speed. The bigger the number,
     *            the faster the palette generation but the greater the
     *            likelihood that colors will be missed.
     * @param ignoreWhite
     *            if <code>true</code>, white pixels are ignored
     *
     * @return the palette as array of RGB arrays
     */
    public static int[][] getPalette(
            opencv_core.Mat sourceImage,
            int colorCount,
            int quality,
            boolean ignoreWhite)
    {
        MMCQ.CMap cmap = getColorMap(sourceImage, colorCount, quality, ignoreWhite);
        if (cmap == null)
        {
            return null;
        }
        return cmap.palette();
    }

    /**
     * Use the median cut algorithm to cluster similar colors.
     *
     * @param sourceImage
     *            the source image
     * @param colorCount
     *            the size of the palette; the number of colors returned
     *
     * @return the color map
     */
    public static MMCQ.CMap getColorMap(opencv_core.Mat sourceImage, int colorCount)
    {
        return getColorMap(
                sourceImage,
                colorCount,
                DEFAULT_QUALITY,
                DEFAULT_IGNORE_WHITE);
    }

    /**
     * Use the median cut algorithm to cluster similar colors.
     *
     * @param sourceImage
     *            the source image
     * @param colorCount
     *            the size of the palette; the number of colors returned
     * @param quality
     *            0 is the highest quality settings. 10 is the default. There is
     *            a trade-off between quality and speed. The bigger the number,
     *            the faster the palette generation but the greater the
     *            likelihood that colors will be missed.
     * @param ignoreWhite
     *            if <code>true</code>, white pixels are ignored
     *
     * @return the color map
     */
    public static MMCQ.CMap getColorMap(
            opencv_core.Mat sourceImage,
            int colorCount,
            int quality,
            boolean ignoreWhite)
    {
        int[][] pixelArray = getPixels(sourceImage, quality, ignoreWhite);

        // Send array to quantize function which clusters values using median
        // cut algorithm
        MMCQ.CMap cmap = MMCQ.quantize(pixelArray, colorCount);
        return cmap;
    }

    /**
     * Gets the image's pixels via BufferedImage.getRGB(..). Slow, but the fast
     * method doesn't work for all color models.
     *
     * @param image
     *            the source image
     * @param quality
     *            0 is the highest quality settings. 10 is the default. There is
     *            a trade-off between quality and speed. The bigger the number,
     *            the faster the palette generation but the greater the
     *            likelihood that colors will be missed.
     * @param ignoreWhite
     *            if <code>true</code>, white pixels are ignored
     *
     * @return an array of pixels (each an RGB int array)
     */
    private static int[][] getPixels(opencv_core.Mat image, int quality, boolean ignoreWhite) {
        int width = image.cols();
        int height = image.rows();

        int pixelCount = width * height;

        // numRegardedPixels must be rounded up to avoid an
        // ArrayIndexOutOfBoundsException if all pixels are good.
        int numRegardedPixels = (pixelCount + quality - 1) / quality;

        int numUsedPixels = 0;

        ByteBuffer buffer = image.createBuffer();

        int[][] res = new int[numRegardedPixels][];
        int r, g, b;

        for (int i = 0; i < pixelCount; i += quality)
        {
            int y = i / width;
            int x = i % width;

            int index = i * image.channels();

            b = buffer.get(index) & 0xFF;
            g = buffer.get(index + 1) & 0xFF;
            r = buffer.get(index + 2) & 0xFF;
            if (!(ignoreWhite && r > 250 && g > 250 && b > 250))
            {
                res[numUsedPixels] = new int[] {r, g, b};
                numUsedPixels++;
            }
        }

        return Arrays.copyOfRange(res, 0, numUsedPixels);
    }

}