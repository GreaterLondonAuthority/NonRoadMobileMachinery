/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.utils.browser;

/**
 * Describes the rectangle that the export should be limited to
 */
public class ClipRect {
    private int top, left, height, width;

    /**
     * Create a clip region using the specified position and sizes
     * @param top Top in pixels
     * @param left Left in pixels
     * @param height Height in pixel
     * @param width Width in pixels
     */
    public ClipRect(int top, int left, int height, int width) {
        this.top = top;
        this.left = left;
        this.height = height;
        this.width = width;
    }

    /**
     * Returns a JSON representation of the clip region using
     * @return JSON representation of a clip region attached to an object called "page"
     */
    public String getJSON() {
        return String.format("page.clipRect = {\n" +
                "  top: %d,\n" +
                "  left: %d,\n" +
                "  width: %d,\n" +
                "  height: %d\n" +
                "};\n", top, left, width, height);
    }

    /**
     * Get the top position in pixels
     * @return Top position
     */
    public int getTop() {
        return top;
    }

    /**
     * Set the top position in pixels
     * @param top Top position
     */
    public void setTop(int top) {
        this.top = top;
    }

    /**
     * Get the left position in pixels
     * @return Left position
     */
    public int getLeft() {
        return left;
    }

    /**
     * Sets the left position in pixels
     * @param left Left position
     */
    public void setLeft(int left) {
        this.left = left;
    }

    /**
     * Gets the height in pixels of the region to clip
     * @return Height in pixels
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets the height of the clip region
     * @param height Height in pixels
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Gets the width in pixels of the region to clip
     * @return Width in pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     * Sets the width of the clip region
     * @param width Width in pixels
     */
    public void setWidth(int width) {
        this.width = width;
    }
}
