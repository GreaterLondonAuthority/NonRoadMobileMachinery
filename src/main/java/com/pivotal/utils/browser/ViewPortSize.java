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
 * Describes the size of the web page
 */
public class ViewPortSize {
    private int height, width;

    /**
     * Create a region using the specified position and sizes
     * @param width Width in pixels
     * @param height Height in pixel
     */
    public ViewPortSize(int width, int height) {
        this.height = height;
        this.width = width;
    }

    /**
     * Returns a JSON representation of the viewport
     * @return JSON representation of a viewport attached to an object called "page"
     */
    public String getJSON() {
        return String.format("page.viewportSize = {\n" +
                "  width: %d,\n" +
                "  height: %d\n" +
                "};\n", width, height);
    }

    /**
     * Gets the height in pixels of the viewport
     * @return Height in pixels
     */
    public int getHeight() {
        return height;
    }

    /**
     * Sets the height of the vieporet in pixesl
     * @param height Height in pixels
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Gets the width of the viewport in pixels
     * @return Width in pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     * Sets the width in pixels
     * @param width Width in pixels
     */
    public void setWidth(int width) {
        this.width = width;
    }
}
