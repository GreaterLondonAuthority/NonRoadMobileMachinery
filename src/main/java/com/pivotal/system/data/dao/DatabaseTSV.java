/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.data.dao;

import java.io.File;

/**
 * Extends the standard DatabaseExcel class to cater for creating and
 * opening a TSV file as a database
 * What it actually does is create a temporary database from the
 * contents of the spreadsheet and opens it read only
 * When the database is closed, the database is destroyed
 */
public class DatabaseTSV extends DatabaseCSV {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatabaseTSV.class);

    /**
     * Constructs a wrapper for a CSV spreadsheet
     *
     * @param spreadsheetFile Spreadsheet to open and convert
     */
    public DatabaseTSV(File spreadsheetFile) {
        super(spreadsheetFile);
    }

    /**
     * {@inheritDoc}
     *
     * Opens the connection and throws any exceptions
     */
    @Override
    public void open() throws Exception {
        delimiterChar = '\t';
        super.open();
    }

}
