/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils.imaging;

import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.image.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws a heatmap image based on a series of data points.
 */
public class HeatMap implements Serializable{

    static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HeatMap.class);
    private List<Point> points = new ArrayList<>();
    private List<Point> boundaryCorners = new ArrayList<>();
    private BufferedImage gradientImage = null;


    private Double minValue = null;
    private Double maxValue = null;
    private Double averageValue = null;
    private Double mapWidth = null;
    private Double mapHeight = null;
    private Double originX = 0.0;
    private Double originY = 0.0;
    private double radius = 1;


    /**
     * Get the minimum value on this heatmap's scale
     *
     * @return minimum value
     */
    public Double getMinValue() {
        return minValue;
    }

    /**
     * Get the maximum value on this heatmap's scale
     *
     * @return maximum value
     */
    public Double getMaxValue() {
        return maxValue;
    }

    /**
     * Gets the average value of this heatmap's scale
     * @return average value
     */
    public Double getAverageValue(){
        return averageValue;
    }

    /**
     * Create a 256-pixel wide gradient image of the given colours, spaced evenly.
     *
     * @param colors The colours to use in the gradient, in the order they appear.
     * @return The gradient image.
     */
    private static BufferedImage CreateEvenlyDistributedGradientImage(Color... colors) {
        Dimension size = new Dimension(1, 256);
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        float[] fractions = new float[colors.length];
        float step = 1.0f / (colors.length - 1);
        for (int i = 0; i < colors.length; ++i) {
            fractions[i] = i * step;
        }
        LinearGradientPaint gradient = new LinearGradientPaint(0, size.height, 1, 0, fractions, colors, MultipleGradientPaint.CycleMethod.REPEAT);
        graphics.setPaint(gradient);
        graphics.fillRect(0, 0, size.width, size.height);
        graphics.dispose();
        return image;
    }

    /**
     * Using an image returned by <code>CreateEvenlyDistributedGradientImage()</code>, generate a lookup table that converts grayscale images
     * to images coloured according to the gradient.
     *
     * @param image Gradient image of the desired colours
     * @return LookupTable suitable for performing the conversion.
     */
    private static LookupTable CreateColorLookupTable(BufferedImage image) {
        int tableSize = 256;
        Raster imageRaster = image.getData();
        double sampleStep = ((double) image.getHeight()) / tableSize; // Sample pixels evenly (cast to avoid integer division)
        short[][] colorTable = new short[4][tableSize];
        int[] pixel = new int[1]; // Sample pixel
        Color color;
        for (int i = 0; i < tableSize; i++) {
            imageRaster.getDataElements(0, (int) ((tableSize - 1 - i) * sampleStep), pixel);
            color = new Color(pixel[0]);
            colorTable[0][i] = (short) color.getRed();
            colorTable[1][i] = (short) color.getGreen();
            colorTable[2][i] = (short) color.getBlue();
            colorTable[3][i] = (short) i;
        }
        return new ShortLookupTable(0, colorTable);
    }

    /**
     * Draw a oval on the given graphics that fades from the given colour in the centre, to fully transparent at the edges.
     *
     * @param graphics Graphics to draw to
     * @param x        X position of the center of the oval
     * @param y        Y position of the center of the oval
     * @param xsize    Width of the oval
     * @param ysize    Height of the oval
     * @param color    Color at the center of the oval
     */
    private static void drawFuzzyCircle(Graphics2D graphics, int x, int y, int xsize, int ysize, Color color) {

        int radius = Math.max(xsize, ysize) * 2;
        Color transparentColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);

        // Create a gradient fading from the solid colour to transparent
        RadialGradientPaint paint = new RadialGradientPaint((float) x, (float) y, radius, new float[]{0f, 1f}, new Color[]{color, transparentColor});

        graphics.setPaint(paint);

        graphics.fillOval(x - xsize * 2, y - ysize * 2, xsize * 4, ysize * 4);

    }

    /**
     * Draw half a oval on the given graphics that fades from the given colour in the centre, to fully transparent at the edges.
     *
     * @param graphics  Graphics to draw to
     * @param x         X position of the center of the oval
     * @param y         Y position of the center of the oval
     * @param xsize     Width of the oval
     * @param ysize     Height of the oval
     * @param direction Direction the semi-circle is facing
     * @param angle     The angle of the segment
     * @param color     Color at the center of the oval
     */
    private static void drawFuzzySemiCircle(Graphics2D graphics, int x, int y, int xsize, int ysize, double direction, double angle, Color color) {

        int radius = Math.max(xsize, ysize) * 2;
        Color transparentColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);

        // Create a gradient fading from the solid colour to transparent
        RadialGradientPaint paint = new RadialGradientPaint((float) x, (float) y, radius, new float[]{0f, 1f}, new Color[]{color, transparentColor});

        Arc2D arc2D = new Arc2D.Double(x - xsize * 2, y - ysize * 2, xsize * 4, ysize * 4, direction - angle/2, angle, Arc2D.PIE);

        graphics.setPaint(paint);

        graphics.fill(arc2D);

    }

    /**
     * Create and image of the given size and fill it with a solid colour.
     *
     * @param width  Width of the image, in pixels.
     * @param height Height of the image, in pixels.
     * @param color  Color to fill the image with.
     * @return The image.
     */
    private static BufferedImage getSolidColorImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        return image;
    }

    /**
     * Add a point to the data.
     *
     * @param x     X coordinate of the point, in meters.
     * @param y     Y coordinate of the point, in meters.
     * @param value The value at the given point.
     */
    public void addPoint(double x, double y, double value) {
        addPoint(x, y, value, 1);
    }

    /**
     * Add a point to the data, and scale the size of the point that is drawn for it.
     *
     * @param x     X coordinate of the point, in meters.
     * @param y     Y coordinate of the point, in meters.
     * @param value The value at the given point.
     * @param scale Amount to scale the size by.
     */
    public void addPoint(double x, double y, double value, double scale) {
        points.add(new Point(x, y, value, scale));
    }

    /**
     * Add a point to the data that will be drawn as a semicircle
     *
     * @param x         X coordinate of the point, in meters.
     * @param y         Y coordinate of the point, in meters.
     * @param value     The value at the given point.
     * @param direction Direction the semi-circle is facing
     * @param angle     The angle of the segment
     */
    public void addSemiCirclePoint(double x, double y, double value, double direction, double angle) {
        points.add(new Point(x, y, value, 1, direction, angle));
    }

    /**
     * Add a point to the data that will be drawn as a semicircle, and scale the size of it
     *
     * @param x         X coordinate of the point, in meters.
     * @param y         Y coordinate of the point, in meters.
     * @param value     The value at the given point.
     * @param scale     Amount to scale the size by.
     * @param direction Direction the semi-circle is facing
     * @param angle     The angle of the segment
     */
    public void addSemiCirclePoint(double x, double y, double value, double scale, double direction, double angle) {
        points.add(new Point(x, y, value, scale, direction, angle));
    }

    /**
     * Set the colours to use for the scale of the heatmap, in order from the colour representing the lowest value,
     * to the colour representing the highest value.
     *
     * @param colors the colours to use
     */
    public void setGradient(Color... colors) {
        gradientImage = CreateEvenlyDistributedGradientImage(colors);
    }

    /**
     * Set the scale of the heatmap, i.e. the least and greatest values that will be displayed.
     *
     * @param min The least value that will be displayed on the heatmap.
     * @param max The greatest value that will be displayed on the heatmap.
     */
    public void setScale(Double min, Double max) {
        this.minValue = min;
        this.maxValue = max;
        this.averageValue = (min + max) / 2;
    }

    /**
     * Add a corner to the bounding polygon of this map. If set, this will blank out areas of the map
     * outside of this polygon.
     *
     * @param x X-coordinate of the corner.
     * @param y Y-coordinate of the corner.
     */
    public void addCorner(double x, double y) {
        boundaryCorners.add(new Point(x, y, 0, 0));
    }

    /**
     * Search through the data to find the least and greatest values and automatically set the scale to those.
     */
    public void autoSetScale() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        double totalPoints = 0;
        int pointCount = 0;
        for (Point point : points) {
            if (point.value < min) {
                min = point.value;
            }
            if (point.value > max) {
                max = point.value;
            }
            pointCount += 1;
            totalPoints += point.value;
        }
        averageValue = totalPoints / pointCount;
        setScale(min, max);
    }

    /**
     * Set the size, in meters, of the area being mapped.
     *
     * @param width  The width of the map.
     * @param height The height of the map.
     */
    public void setMapSize(double width, double height) {
        this.mapWidth = width;
        this.mapHeight = height;
    }

    /**
     * Set the origin of the map - this is the point in the bottom left corner the image.
     *
     * @param x X-coordinate of the point.
     * @param y Y-coordinate of the point.
     */
    public void setOrigin(double x, double y) {
        this.originX = x;
        this.originY = y;
    }

    /**
     * Search through the data to find the greatest x and y co-ordinates, and fit the map to those.
     */
    public void autoSetMapSize() {
        double maxX = 0;
        double maxY = 0;
        for (Point point : points) {
            if (point.x > maxX) {
                maxX = point.x;
            }
            if (point.y > maxY) {
                maxY = point.y;
            }
        }

        setMapSize(maxX, maxY);
    }

    /**
     * Search through the data to find the greatest and least x and y co-ordinates, and fit the map to those.
     */
    public void autoCrop() {

        Double maxX = null;
        Double maxY = null;
        Double minX = null;
        Double minY = null;

        for (Point point : points) {
            if (maxX == null || point.x > maxX) {
                maxX = point.x;
            }
            if (maxY == null || point.y > maxY) {
                maxY = point.y;
            }
            if (minX == null || point.x < minX) {
                minX = point.x;
            }
            if (minY == null || point.y < minY) {
                minY = point.y;
            }
        }

        if (minX != null && minY != null && maxX != null && maxY != null) {
            setOrigin(minX, minY);
            setMapSize(maxX - minX, maxY - minY);
        }
    }

    /**
     * Set the radius, in meters, of the data points
     *
     * @param radius The radius of each data point plotted on the map.
     */
    public void setPointRadius(double radius) {
        this.radius = radius;
    }

    /**
     * Set a width for the image, and automatically calculate the height to match the aspect ratio of the map size.
     *
     * @param imageHeight The height of the image, in pixels.
     * @return The heatmap image
     */
    public BufferedImage getImageByHeight(int imageHeight) {
        int imageWidth = (int) (imageHeight * this.mapWidth / this.mapHeight);
        return getImage(imageWidth, imageHeight);
    }

    /**
     * Set a height for the image, and automatically calculate the width to match the aspect ratio of the map size.
     *
     * @param imageWidth The width of the image, in pixels.
     * @return The heatmap image
     */
    public BufferedImage getImageByWidth(int imageWidth) {
        int imageHeight = (int) (imageWidth * this.mapHeight / this.mapWidth);
        return getImage(imageWidth, imageHeight);
    }

    /**
     * Returns a <code>BufferedImage</code> containing a semi-transparent image of the heatmap.
     *
     * @param imageWidth  The width of the image, in pixels.
     * @param imageHeight The height of the image, in pixels.
     * @return The heatmap image.
     */
    public BufferedImage getImage(int imageWidth, int imageHeight) {

        // We initially draw the image in grayscale (black & white), then convert it to colour afterwards using a LookupTable.
        // This prevents colours unintentionally blending in different ways - for example, a red area next to a blue area would blend to a purple area,
        // But the colour scale may ask for green to be between red and blue.

        BufferedImage heatmapImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D heatmapGraphics = (Graphics2D) heatmapImage.getGraphics();

        // If we don't have manual settings, get them automatically.
        if (this.maxValue == null || this.minValue == null) {
            autoSetScale();
        }

        if (this.mapWidth == null || this.mapHeight == null) {
            autoSetMapSize();
        }

        // Work out the scaling to convert from meters to pixels
        double scaleX = imageWidth / this.mapWidth;
        double scaleY = imageHeight / this.mapHeight;


        // Draw the points
        try {

            // Convert the radius from meters to pixels.
            int rectWidth = (int) (scaleX * radius) + 1;
            int rectHeight = (int) (scaleY * radius) + 1;

            for (Point point : points) {
                int x = (int) ((point.x - originX) * scaleX);

                int y = (int) ((point.y - originY) * scaleY);

                // Convert from a value in the data to a value between 0 and 1.
                float value = (float) ((point.value - this.minValue) / (this.maxValue - this.minValue));
                if (value >= 1) value = 1;
                if (value < 0) value = 0;

                int width = (int) (rectWidth * point.scale);
                int height = (int) (rectHeight * point.scale);

                // Draw a gray colour of the corresponding shade.
                Color color = new Color(value, value, value);
                if (point.semiCircle) {
                    drawFuzzySemiCircle(heatmapGraphics, x, y, width, height, point.direction, point.angle, color);
                }
                else {
                    drawFuzzyCircle(heatmapGraphics, x, y, width, height, color);
                }
            }
        }
        catch (Exception e) {
            logger.error("Problem creating heatmap - " + PivotalException.getErrorMessage(e));
        }

        heatmapGraphics.dispose();

        // If no gradient was chosen, use a default blue-to-red rainbow gradient.
        if (gradientImage == null) {
            gradientImage = CreateEvenlyDistributedGradientImage(Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW, Color.RED);
        }

        // Get the lookup table, make the image semi-transparent, and convert the grayscale image to colour.
        LookupTable colorTable = CreateColorLookupTable(gradientImage);
        BufferedImage coloredImage = new LookupOp(colorTable, null).filter(heatmapImage, null);

        // Create a solid white background and draw the transparent image on top of it.

        BufferedImage transparentImage = getSolidColorImage(coloredImage.getWidth(), coloredImage.getHeight(), new Color(0, 0, 0, 0));
        Graphics2D transparentGraphics = transparentImage.createGraphics();
        transparentGraphics.drawImage(coloredImage, null, 0, 0);


        Polygon boundaryPolygon = new Polygon();

        if (!boundaryCorners.isEmpty()) {
            for (Point corner : boundaryCorners) {
                int x = (int) (scaleX * (corner.x - originX));
                int y = (int) (scaleY * (corner.y - originY));
                boundaryPolygon.addPoint(x, y);
                logger.debug("corner (%s, %s) -> (%s, %s)", corner.x, corner.y, x, y);
            }

            Area area = new Area(boundaryPolygon.getBounds2D());
            area.subtract(new Area(boundaryPolygon));
            transparentGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f));
            transparentGraphics.setPaint(new Color(0, 0, 0, 0));
            transparentGraphics.fill(area);
        }

        transparentGraphics.dispose();
        return transparentImage;

    }

    /**
     * Returns a scaled version of a gradient image - width is always 45 pixels
     * @param height Height to get
     * @return Scaled image gradient
     */
    public BufferedImage getScaleImage(int height) {
        if (this.maxValue == null || this.minValue == null) {
            autoSetScale();
        }

        if (gradientImage == null) {
            gradientImage = CreateEvenlyDistributedGradientImage(Color.BLUE, Color.CYAN, Color.GREEN, Color.YELLOW, Color.RED);
        }

        BufferedImage image = getSolidColorImage(45, height, Color.WHITE);
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(Common.scaleImage(gradientImage, 45, height), 0, 0, null);
        graphics.dispose();

        return image;
    }

    /**
     * Stores a point on the map and it's value.
     */
    static class Point {
        double x;
        double y;
        double value;
        double scale;
        double direction;
        double angle;
        boolean semiCircle;

        /**
         * Create a point with the specified location and value.
         *
         * @param x     X-coordinate of the point.
         * @param y     Y-coordinate of the point.
         * @param value Value at the point.
         * @param scale Amount to scale the size by.
         */
        private Point(double x, double y, double value, double scale) {
            this.x = x;
            this.y = y;
            this.value = value;
            this.scale = scale;
            this.semiCircle = false;
        }

        /**
         * Creates a directional semi-circle point at the specified location
         *
         * @param x             X-coordinate of the point.
         * @param y             Y-coordinate of the point.
         * @param value         Value at the point.
         * @param scale         Amount to scale the size by.
         * @param direction     Direction the semi-circle is facing
         * @param angle         The angle of the segment
         */
        private Point(double x, double y, double value, double scale, double direction, double angle) {
            this.x = x;
            this.y = y;
            this.value = value;
            this.scale = scale;
            this.semiCircle = true;
            this.direction = direction;
            this.angle = angle;
        }
    }

}
