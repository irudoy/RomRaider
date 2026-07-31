/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */

package com.romraider.theme;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

/**
 * Preserves source pixels when an icon is rendered on a Retina display.
 *
 * RomRaider stores toolbar icons at approximately twice their configured
 * logical size. Creating a smaller raster before painting discards those
 * pixels. A multi-resolution image keeps the logical dimensions while the
 * Java's macOS Retina pipeline can request a denser backing image.
 */
public final class HiDpiIconScaler {
    private HiDpiIconScaler() {
        throw new UnsupportedOperationException();
    }

    public static ImageIcon scale(ImageIcon source, int percentOfOriginal) {
        int sourceWidth = source.getIconWidth();
        int sourceHeight = source.getIconHeight();
        int logicalWidth = (int) (sourceWidth * (percentOfOriginal * .01));
        int logicalHeight = (int) (sourceHeight * (percentOfOriginal * .01));

        if (logicalWidth < 1 || logicalHeight < 1) {
            return source;
        }

        return fixed(source, logicalWidth, logicalHeight);
    }

    public static ImageIcon fixed(ImageIcon source, int logicalWidth,
            int logicalHeight) {
        return fixed(source, logicalWidth, logicalHeight, false);
    }

    /**
     * Upscales low-resolution upstream artwork without changing its design.
     *
     * The source pixels remain untouched at their native size. Denser
     * variants use bicubic interpolation followed by a light alpha-safe
     * sharpening pass so small logger icons retain their original edges.
     */
    public static ImageIcon original(ImageIcon source, int logicalWidth,
            int logicalHeight) {
        return fixed(source, logicalWidth, logicalHeight, true);
    }

    private static ImageIcon fixed(ImageIcon source, int logicalWidth,
            int logicalHeight, boolean sharpenUpscale) {
        if (logicalWidth < 1 || logicalHeight < 1) {
            throw new IllegalArgumentException(
                    "Logical icon dimensions must be positive");
        }

        Image sourceImage = source.getImage();
        int sourceWidth = source.getIconWidth();
        int sourceHeight = source.getIconHeight();
        Image multiResolutionImage = createMultiResolutionImage(
                sourceImage,
                sourceWidth,
                sourceHeight,
                logicalWidth,
                logicalHeight,
                source.getImageObserver(),
                sharpenUpscale);
        return new ImageIcon(multiResolutionImage);
    }

    private static Image createMultiResolutionImage(Image source,
            int sourceWidth, int sourceHeight, int logicalWidth,
            int logicalHeight, java.awt.image.ImageObserver observer,
            boolean sharpenUpscale) {
        Image[] variants = {
            render(source, sourceWidth, sourceHeight,
                    logicalWidth, logicalHeight, observer,
                    sharpenUpscale),
            render(source, sourceWidth, sourceHeight,
                    logicalWidth * 2, logicalHeight * 2, observer,
                    sharpenUpscale)
        };
        return new BaseMultiResolutionImage(variants);
    }

    private static Image render(Image source, int sourceWidth,
            int sourceHeight, int width, int height,
            java.awt.image.ImageObserver observer, boolean sharpenUpscale) {
        if (sharpenUpscale
                && width == sourceWidth
                && height == sourceHeight) {
            return source;
        }

        BufferedImage image = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(
                RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics.drawImage(source, 0, 0, width, height, observer);
        graphics.dispose();
        if (sharpenUpscale
                && (width > sourceWidth || height > sourceHeight)) {
            return sharpen(image);
        }
        return image;
    }

    private static BufferedImage sharpen(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage sharpened = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_ARGB);
        int[] weights = {1, 2, 1, 2, 4, 2, 1, 2, 1};

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int center = source.getRGB(x, y);
                int alpha = center >>> 24;
                if (alpha == 0) {
                    sharpened.setRGB(x, y, center);
                    continue;
                }

                long red = 0;
                long green = 0;
                long blue = 0;
                long weightedAlpha = 0;
                int weightIndex = 0;
                for (int offsetY = -1; offsetY <= 1; offsetY++) {
                    int sampleY = clamp(y + offsetY, 0, height - 1);
                    for (int offsetX = -1; offsetX <= 1; offsetX++) {
                        int sampleX = clamp(x + offsetX, 0, width - 1);
                        int sample = source.getRGB(sampleX, sampleY);
                        int sampleAlpha = sample >>> 24;
                        int weight = weights[weightIndex++];
                        int alphaWeight = sampleAlpha * weight;
                        weightedAlpha += alphaWeight;
                        red += ((sample >>> 16) & 0xff) * alphaWeight;
                        green += ((sample >>> 8) & 0xff) * alphaWeight;
                        blue += (sample & 0xff) * alphaWeight;
                    }
                }

                if (weightedAlpha > 0) {
                    int centerRed = (center >>> 16) & 0xff;
                    int centerGreen = (center >>> 8) & 0xff;
                    int centerBlue = center & 0xff;
                    int blurredRed = (int) (red / weightedAlpha);
                    int blurredGreen = (int) (green / weightedAlpha);
                    int blurredBlue = (int) (blue / weightedAlpha);
                    int resultRed = sharpenChannel(centerRed, blurredRed);
                    int resultGreen =
                            sharpenChannel(centerGreen, blurredGreen);
                    int resultBlue =
                            sharpenChannel(centerBlue, blurredBlue);
                    center = (alpha << 24)
                            | (resultRed << 16)
                            | (resultGreen << 8)
                            | resultBlue;
                }
                sharpened.setRGB(x, y, center);
            }
        }
        return sharpened;
    }

    private static int sharpenChannel(int center, int blurred) {
        return clamp(
                Math.round(center + (center - blurred) * 0.35f),
                0,
                255);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
