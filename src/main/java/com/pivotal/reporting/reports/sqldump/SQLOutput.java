/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */

package com.pivotal.reporting.reports.sqldump;

import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;

import java.util.Map;

/**
 * This is the base class that is extended for all types of SQLDump outputs
 */
public abstract class SQLOutput {

    String filename;
    String compression;
    boolean noHeader;

    /**
     * Default constructor - only used by the sub-classed versions
     */
    protected SQLOutput() {
        filename=null;
        compression=null;
        noHeader=true;
    }

    /**
     * Creates an output channel for SQL rows
     *
     * @param filename File to create
     * @param compression Compression algorithm to use
     * @param noHeader True if headers are not to be output
     */
    public SQLOutput(String filename, String compression, boolean noHeader) {
        this.filename=filename;
        this.compression=compression;
        this.noHeader=noHeader;

        // Check the filename is OK

        if (Common.isBlank(filename))
            throw new PivotalException("The filename is not valid");

    }

    /**
     * Creates a new section for all the next rows
     */
    public void newSection() {
        newSection(null);
    }

    /**
     * Creates a new section for all the next rows
     *
     * @param name Optional name of the worksheet
     */
    public abstract void newSection(String name);

    /**
     * Adds a row to the current section
     *
     * @param rowValues Map of column values
     */
    public abstract void addRow(Map<String,Object> rowValues);

    /**
     * Closes the output and returns
     */
    public abstract void close();
}
