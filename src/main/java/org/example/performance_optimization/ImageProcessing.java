/*
 * MIT License
 *
 * Copyright (c) 2019 Michael Pogrebinsky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.example.performance_optimization;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;

public class ImageProcessing {

    public static final String SOURCE_FILE = "many-flowers.jpg";
    public static final String DESTINATION_FILE = "/out/many-flowers.jpg";

    public static void main(String[] args) throws IOException {

        InputStream is = ImageProcessing.class.getClassLoader().getResourceAsStream(SOURCE_FILE);


        BufferedImage originalImage = ImageIO.read(is);
        BufferedImage resultImage = new BufferedImage(
                originalImage.getWidth(), originalImage.getHeight(), TYPE_INT_RGB);

        long startTime = System.currentTimeMillis();
//        recolorSingleThreaded(originalImage, resultImage);
        recolorMultiThreaded(originalImage, resultImage, Runtime.getRuntime().availableProcessors());
        long duration = System.currentTimeMillis() - startTime;

        File outputFile = new File(DESTINATION_FILE);
        File outFolder = outputFile.getParentFile();

        if (!outFolder.exists()) {
            outFolder.mkdir();
        }

        ImageIO.write(resultImage, "jpg", outputFile);

        System.out.println("It took " + duration + " ms");
    }

    public static void recolorMultiThreaded(BufferedImage originalImage, BufferedImage resultImage, int numberOfThreads) {

        List<Thread> threads = new ArrayList<>();
        int width = originalImage.getWidth();
        int height = originalImage.getHeight() / numberOfThreads;

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadMultiplier = i;

            Thread thread = new Thread(() -> {
                int leftCorner = 0;
                int topCorner = height * threadMultiplier;

                recolorImage(originalImage, resultImage, leftCorner, topCorner, width, height);
            });

            threads.add(thread);
        }

        for (Thread thread: threads) {
            thread.start();
        }

        for (Thread thread: threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.err.println(e);
            }
        }
    }

    public static void recolorSingleThreaded(BufferedImage originalImage, BufferedImage resultImage) {
        recolorImage(originalImage, resultImage, 0, 0, originalImage.getWidth(), originalImage.getHeight());
    }

    public static void recolorImage(BufferedImage originalImage, BufferedImage resultImage,
                                    int leftCorner, int topCorner, int width, int height) {

        for (int x = leftCorner; x < leftCorner + width && x < originalImage.getWidth(); x++) {

            for (int y = topCorner; y < topCorner + height && y < originalImage.getHeight(); y++) {
                recolorPixel(originalImage, resultImage ,x, y);
            }
        }
    }

    public static void recolorPixel(BufferedImage originalImage, BufferedImage resultImage, int x, int y) {

        int rgb = originalImage.getRGB(x, y);

        int red = getRed(rgb);
        int green = getGreen(rgb);
        int blue = getBlue(rgb);

        int newRed = red;
        int newGreen = green;
        int newBlue = blue;

        if (isShadeOfGray(red, green, blue)) {
            newRed = Math.min(255, red + 10);
            newGreen = Math.max(0, green - 80);
            newBlue = Math.max(0, blue - 20);
        }

        int newRgb = createRgbFromColors(newRed, newGreen, newBlue);
        setRgb(resultImage, x, y, newRgb);
    }

    public static void setRgb(BufferedImage image, int x, int y, int rgb) {
        image.getRaster().setDataElements(x, y, image.getColorModel().getDataElements(rgb, null));
    }

    public static boolean isShadeOfGray(int red, int green, int blue) {
        return Math.abs(red - green) < 30
                && Math.abs(red - blue) < 30
                && Math.abs(green - blue) < 30;
    }

    public static int createRgbFromColors(int red, int green, int blue) {

        int rgb = 0;

        rgb |= blue;
        rgb |= green << 8;
        rgb |= red << 16;

        rgb |= 0xFF000000;

        return rgb;

    }

    public static int getRed(int rgb) {
        int red = (rgb & 0x00FF0000) >> 16;
//        System.out.println("In RGB: " + Integer.toBinaryString(rgb) + " red is " + Integer.toBinaryString(red));
        return red;
    }

    public static int getGreen(int rgb) {
        int green = (rgb & 0x0000FF00) >> 8;
//        System.out.println("In RGB: " + Integer.toBinaryString(rgb) + " green is " + Integer.toBinaryString(green));
        return green;
    }

    public static int getBlue(int rgb) {
        int blue = rgb & 0x000000FF;
//        System.out.println("In RGB: " + Integer.toBinaryString(rgb) + " blue is " + Integer.toBinaryString(blue));
        return blue;
    }
}
