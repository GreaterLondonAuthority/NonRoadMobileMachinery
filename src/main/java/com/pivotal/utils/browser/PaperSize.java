/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils.browser;

import com.pivotal.utils.Common;

/**
 * Defines a paper size to use by the browser
 */
public class PaperSize {

    private int width;
    private int height;
    private double borderWidth;

    private String format;
    private String orientation;
    private String border;

    /**
     * Constructs a paper size based on pixels
     *
     * @param width  Width in pixels
     * @param height Height in pixels
     * @param border Border in pixels
     */
    public PaperSize(int width, int height, int border) {
        this.width = width;
        this.height = height;
        this.borderWidth = border;
    }

    /**
     * Constructs a paper size based on common paper formats
     *
     * @param format      Format e.g. A4
     * @param orientation Orientation "portrait" or "landscape"
     * @param border      Border in cm e.g. 0.1cm
     */
    public PaperSize(String format, String orientation, String border) {
        this.format = format;
        this.orientation = orientation;
        this.border = border;
    }

    /**
     * Returns a paper size associated with the page
     *
     * @return JSON paper size as an object attached to Page
     */
    public String getJSON() {
        if (!Common.isBlank(format)) {
            return String.format("page.paperSize = {\n" +
                            "  format: '%s',\n" +
                            "  orientation: '%s',\n" +
                            "  border: '%s'\n" +
                            "  };\n",
                    format, Common.isBlank(orientation) ? "portrait" : orientation, Common.isBlank(border) ? "0cm" : border
            );
        }
        else
            return String.format("page.paperSize = {\n" +
                            "  width: '%dpx',\n" +
                            "  height: '%dpx',\n" +
                            "  border: '%fpx'\n" +
                            "};\n",
                    width, height, borderWidth);
    }

    /**
     * Gets the width in pixels
     * @return Width of paper in pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     * Sets the paper width in pixels
     * @param width Paper width in pixels
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * Gets the height of the paper in pixels
     * @return Paper height in pixels
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets the height of the paper in pixels
     * @param height Height in pixels
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Gets the border width in pixels
     * @return Border width in pixels
     */
    public double getBorderWidth() {
        return borderWidth;
    }

    /**
     * Sets the border width in pixels
     * @param borderWidth Border width in pixels
     */
    public void setBorderWidth(double borderWidth) {
        this.borderWidth = borderWidth;
    }

    /**
     * Gets the format of the paper
     * @return Format of the paper e.g. A4
     */
    public String getFormat() {
        return format;
    }

    /**
     * Sets the format of the paper
     * @param format Format of the paper e.g. A4
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * Gets the orientation of the paper
     * @return Orientation of the paper
     */
    public String getOrientation() {
        return orientation;
    }

    /**
     * Sets the orientation of the paper
     * @param orientation Orientation of the paper
     */
    public void setOrientation(String orientation) {
        this.orientation = Common.doStringsMatch(orientation, "portrait", "landscape")?orientation:"portrait";
    }

    /**
     * Returns the border expressed in real units
     * @return Border in real units e.g. 1cm
     */
    public String getBorder() {
        return border;
    }

    /**
     * Sets the border width in real units
     * @param border Border in real units e.g. 1cm
     */
    public void setBorder(String border) {
        this.border = border;
    }
}
