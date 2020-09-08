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
 * Extends the DatabaseCSV class to cater for creating and
 * opening CSV-like files. The field delimiter to be used is defined
 * in the constructor.
 * What it actually does is create a temporary database from the
 * contents of the spreadsheet and opens it read only
 * When the database is closed, the database is destroyed
 */
public class DatabaseCustomDelimiter extends DatabaseCSV {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatabaseCustomDelimiter.class);

    /**
     * Constructs a wrapper for a CSV spreadsheet
     *
     * @param spreadsheetFile Spreadsheet to open and convert
     * @param delimiterChar Char to be used as field delimiter
     */
    public DatabaseCustomDelimiter(File spreadsheetFile, char delimiterChar) {
        super(spreadsheetFile);
        this.delimiterChar = delimiterChar;
    }

    /**
     * Constructs a wrapper for a CSV spreadsheet
     *
     * @param spreadsheetFile Spreadsheet to open and convert
     * @param delimiterChar Char to be used as field delimiter
     * @param encoding input file encoding e.g. UTF8, UTF16, ISO-8859-1
     */
    public DatabaseCustomDelimiter(File spreadsheetFile, char delimiterChar, String encoding) {
        super(spreadsheetFile);
        this.delimiterChar = delimiterChar;
        this.encoding = encoding;
    }

    /**
     * Constructs a wrapper for a CSV spreadsheet
     *
     * @param spreadsheetFile Spreadsheet to open and convert
     * @param delimiterChar Char to be used as field delimiter
     * @param useQuoteChar Whether to use quote char or not
     * @param encoding input file encoding e.g. UTF8, UTF16, ISO-8859-1
     */
    public DatabaseCustomDelimiter(File spreadsheetFile, char delimiterChar, boolean useQuoteChar, String encoding) {
        super(spreadsheetFile);
        this.delimiterChar = delimiterChar;
        this.encoding = encoding;
        if (!useQuoteChar) {
            quoteChar = '\0';
        }
    }

    /**
     * Constructs a wrapper for a CSV spreadsheet
     *
     * @param spreadsheetFile Spreadsheet to open and convert
     * @param delimiterChar Char to be used as field delimiter
     * @param useQuoteChar Whether to use quote char or not
     */
    public DatabaseCustomDelimiter(File spreadsheetFile, char delimiterChar, boolean useQuoteChar) {
        super(spreadsheetFile);
        this.delimiterChar = delimiterChar;
        if (!useQuoteChar) {
            quoteChar = '\0';
        }
    }
}
