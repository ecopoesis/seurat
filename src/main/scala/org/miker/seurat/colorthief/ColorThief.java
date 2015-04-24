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

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ColorThief {
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
    public static int[] getColor(opencv_core.Mat sourceImage) {
        int[][] palette = getPalette(sourceImage, 5);
        if (palette == null) {
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
     * @param ignoreWhite
     *            if <code>true</code>, white pixels are ignored
     *
     * @return the dominant color as RGB array
     */
    public static int[] getColor(opencv_core.Mat sourceImage, boolean ignoreWhite) {
        int[][] palette = getPalette(sourceImage, 5, ignoreWhite);
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
    public static int[][] getPalette(opencv_core.Mat sourceImage, int colorCount) {
        MMCQ.CMap cmap = getColorMap(sourceImage, colorCount);
        if (cmap == null) {
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
     * @param ignoreWhite
     *            if <code>true</code>, white pixels are ignored
     *
     * @return the palette as array of RGB arrays
     */
    public static int[][] getPalette(opencv_core.Mat sourceImage,int colorCount, boolean ignoreWhite) {
        MMCQ.CMap cmap = getColorMap(sourceImage, colorCount, ignoreWhite);
        if (cmap == null) {
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
    public static MMCQ.CMap getColorMap(opencv_core.Mat sourceImage, int colorCount) {
        return getColorMap(sourceImage, colorCount, DEFAULT_IGNORE_WHITE);
    }

    /**
     * Use the median cut algorithm to cluster similar colors.
     *
     * @param sourceImage
     *            the source image
     * @param colorCount
     *            the size of the palette; the number of colors returned
     * @param ignoreWhite
     *            if <code>true</code>, white pixels are ignored
     *
     * @return the color map
     */
    public static MMCQ.CMap getColorMap(opencv_core.Mat sourceImage, int colorCount, boolean ignoreWhite) {
        int[][] pixelArray = getPixels(sourceImage, ignoreWhite);

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
     * @param ignoreWhite
     *            if <code>true</code>, white pixels are ignored
     *
     * @return an array of pixels (each an RGB int array)
     */
    private static int[][] getPixels(opencv_core.Mat image, boolean ignoreWhite) {
        int numUsedPixels = 0;

        ByteBuffer buffer = image.createBuffer();

        int[][] res = new int[image.cols() * image.rows()][];
        int r, g, b;

        for (int x = 0; x < image.cols(); x++) {
            for (int y = 0; y < image.rows(); y++) {
                b = buffer.get((x * image.channels()) + (y * image.cols() * image.channels())) & 0xFF;
                g = buffer.get((x * image.channels()) + (y * image.cols() * image.channels()) + 1) & 0xFF;
                r = buffer.get((x * image.channels()) + (y * image.cols() * image.channels()) + 2) & 0xFF;
                if (!(ignoreWhite && r > 250 && g > 250 && b > 250)) {
                    res[numUsedPixels] = new int[]{r, g, b};
                    numUsedPixels++;
                }
            }
        }

        return Arrays.copyOfRange(res, 0, numUsedPixels);
    }

}