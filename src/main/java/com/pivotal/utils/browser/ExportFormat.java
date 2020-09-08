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
* This class describes the configuration for a Browser to use to operate
*/
public class ExportFormat {

    public static enum Format {
        PDF, HTML, PNG, GIF, JPG;

        /**
         * Returns the Format that matches the name
         *
         * @param name Name of the format
         * @return Format to use
         */
        public static Format get(String name) {
            Format returnValue = JPG;
            for (Format value : values()) {
                if (Common.doStringsMatch(name, value.name())) {
                    returnValue = value;
                    break;
                }
            }
            return returnValue;
        }

        @Override
        public String toString() {
            return name();
        }
    }

    private ClipRect clipRect;
    private PaperSize paperSize;
    private int scrollPosition=0;
    private ViewPortSize viewportSize;
    private Double zoomFactor = 1.0;
    private Double scaleFactor = 1.0;
    private Format format = Format.PNG;

    /**
     * Default constructor
     */
    public ExportFormat() {

        // Setup some base line settings

        viewportSize = new ViewPortSize(800, 600);
    }

    /**
     * Returns the file extension for the output type
     * @return File extension
     */
    public String getFormatFilenameExtension() {
        return format.name().toLowerCase();
    }

    /**
     * Get the clip rectangle used for export
     * @return Clip region
     */
    public ClipRect getClipRect() {
        return clipRect;
    }

    /**
     * Sets the region exported from the page
     * @param clipRect Region to export
     */
    public void setClipRect(ClipRect clipRect) {
        this.clipRect = clipRect;
    }

    /**
     * Get the paper size used for printing etc.
     * @return Paper size
     */
    public PaperSize getPaperSize() {
        return paperSize;
    }

    /**
     * Set sht epaper size to use for printing etc
     * @param paperSize Paper size
     */
    public void setPaperSize(PaperSize paperSize) {
        this.paperSize = paperSize;
    }

    /**
     * Gets the scroll position
     * @return Gets the scroll position to use
     */
    public int getScrollPosition() {
        return scrollPosition;
    }

    /**
     * Sets the scroll position to set the page at
     * @param scrollPosition Scroll position
     */
    public void setScrollPosition(int scrollPosition) {
        this.scrollPosition = scrollPosition;
    }

    /**
     * Get the viewport size - same as the page size in a browser
     * @return Viewport size
     */
    public ViewPortSize getViewportSize() {
        return viewportSize;
    }

    /**
     * Sets the viewport size
     * @param viewportSize Page size
     */
    public void setViewportSize(ViewPortSize viewportSize) {
        this.viewportSize = viewportSize;
    }

    /**
     * Gets the zoom factor to use when rendering the page
     * @return Zoom factor to use
     */
    public Double getZoomFactor() {
        return zoomFactor;
    }

    /**
     * Sets the zoom factor
     * @param zoomFactor Zoom factor e.g. 0.25
     */
    public void setZoomFactor(Double zoomFactor) {
        this.zoomFactor = zoomFactor;
    }

    /**
     * Returns the scale factor used to scale the final output
     * @return Scale factor
     */
    public Double getScaleFactor() {
        return scaleFactor;
    }

    /**
     * Sets the scale factor to use on the final outptut (only used for image exports)
     * @param scaleFactor Scale factor to apply to the export image
     */
    public void setScaleFactor(Double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    /**
     * Returns the export format to use for this configuration
     * @return Export format
     */
    public Format getFormat() {
        return format;
    }

    /**
     * Sets the export format to use
     * @param format Export format
     */
    public void setFormat(Format format) {
        this.format = format;
    }

    /**
     * Returns a JSON representation of the format
     * @return String representation of the format
     */
    public String getJSON() {
        StringBuilder command = new StringBuilder();
        command.append(String.format("    page.zoomFactor = %f;\n", getZoomFactor()));
        if (getClipRect()!=null)
            command.append(getClipRect().getJSON());
        if (getPaperSize()!=null)
            command.append(getPaperSize().getJSON());
        if (getViewportSize()!=null)
            command.append(getViewportSize().getJSON());
        return command.toString();
    }

}
