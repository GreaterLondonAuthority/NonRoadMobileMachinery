/*
 ***************************************************************************
 *
 * Copyright (c) Greater London Authority, 2020. This source code is licensed under the Open Government Licence 3.0.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM.
 *
 ****************************************************************************
 */
package com.pivotal.system.data.dao;

import au.com.bytecode.opencsv.CSVReader;
import com.pivotal.utils.Common;
import com.pivotal.utils.PivotalException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Extends the standard DatabaseExcel class to cater for creating and
 * opening a CSV file as a database
 * What it actually does is create a temporary database from the
 * contents of the spreadsheet and opens it read only
 * When the database is closed, the database is destroyed
 */
public class DatabaseCSV extends DatabaseExcel {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DatabaseCSV.class);
    protected char delimiterChar = ',';
    protected char quoteChar = '"';
    protected String encoding = "ISO-8859-1";

    /**
     * Constructs a wrapper for a CSV spreadsheet
     *
     * @param spreadsheetFile Spreadsheet to open and convert
     */
    public DatabaseCSV(File spreadsheetFile) {
        this(spreadsheetFile, null);
    }

    /**
     * Constructs a wrapper for a CSV spreadsheet
     *
     * @param spreadsheetFile Spreadsheet to open and convert
     * @param encoding Character encoding to use.
     */
    public DatabaseCSV(File spreadsheetFile, String encoding) {
        super(spreadsheetFile);
        name = spreadsheetFile.getName();
        if (encoding != null) {
            this.encoding = encoding;
        }
    }

    /**
     * {@inheritDoc}
     *
     * Opens the connection and throws any exceptions
     */
    @Override
    public void open() throws Exception {
        close();

        CSVReader inp = null;
        Statement stat = null;
        try {
            // We now need to open the spreadsheet and turn it into a database
            // We're going to use good old SQLite - this is because it's blindingly
            // fast (native code), uses manifest typing which saves us a load of
            // hassle when dealing with data types from a spreadsheet and it's
            // all stored in a single file

            logger.debug("Converting CSV file [{}]", spreadsheetFile);
            sqliteFile=new File(Common.getTemporaryFilename("db3"));
            Class.forName("org.sqlite.JDBC");
            dbConnection = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile.getAbsolutePath());

            // Run the whole update in a single transaction

            dbConnection.setAutoCommit(false);
            stat = dbConnection.createStatement();

            // Get the sheet and create a table for it

            String tableName="sheet1";
            logger.debug("Loading sheet [{}] as table [{}]", tableName, tableName);
            String sql="create table " + tableName + " (";

            // We need to find the widest row so that we can figure out
            // what the column names will be

            int columnCount=0;
            inp = createCSVReader();
            String[] line;
            while ((line = inp.readNext()) != null) {
                if (!Common.isBlank(line) && line.length>columnCount)
                    columnCount=line.length;
            }
            inp.close();
            sql+=getCellNames(columnCount) + ");";
            logger.debug("Executing table creation statment [{}]", sql);
            stat.executeUpdate(sql);

            // Ok, now we go round again but this time we can put the data into
            // the database

            inp = createCSVReader();
            while ((line = inp.readNext()) != null) {
                if (!Common.isBlank(line)) {

                    // Tidy up the quotes and trim

                    for (int i=0; i<line.length; i++)
                        line[i]=line[i].replaceAll("'","''").trim();

                    // Add the record to the database

                    sql="insert into " + tableName + " (" + getCellNames(line.length) + ") values ('" + line.length + "','" + Common.join(line,"','") + "');";
                    logger.debug("Inserting data [{}]", sql);
                    stat.executeUpdate(sql);
                }
            }

            // Commit the transaction

            dbConnection.commit();
            logger.debug("Conversion of CSV file [%] complete", spreadsheetFile);
        }
        catch (ClassNotFoundException e) {
            throw new PivotalException("Cannot load driver [" +  "] for datasource [" + "] " + PivotalException.getErrorMessage(e));
        }
        catch (Exception e) {
            logger.error("Problem reading CSV file [{}] - {}", spreadsheetFile.getAbsolutePath(), PivotalException.getErrorMessage(e));
            close();
            throw e;
        }

        // Clean up after ourselves

        finally {
            Common.close(stat, inp);
        }
    }

    /**
     * Creates a new CSVReader object based on the spreadsheetFile, delimiterChar and quoteChar
     *
     * @return the new CSVReader object
     * @throws IOException whenever it is unable to open the file
     */
    private CSVReader createCSVReader() throws IOException {
        InputStreamReader isr = new InputStreamReader(new FileInputStream(spreadsheetFile), encoding);

        //not specifying the escapeChar='"' anymore because it would split a field with comma inside quoteChars
        return new CSVReader(isr, delimiterChar, quoteChar);
    }

    /**
     * Returns the cell representation of the column number as per Excel
     *
     * @param cellNumber Numeric position of the cell
     * @return String name of the cell
     */
    public static String getCellName(int cellNumber) {
        String returnValue="col";
        if (cellNumber > 25)
            returnValue+="" + (char)('a' + Math.abs((cellNumber-26) / 26));
        returnValue+=(char)('a' + (cellNumber % 26));
        return returnValue;
    }

    /**
     * Returns the cell names as a comma delimited list
     *
     * @param numberOfCells Number of columns
     *
     * @return Comma separated list
     */
    private static String getCellNames(int numberOfCells) {
        List<String> cells=new ArrayList<>();
        for (int i=0; i<numberOfCells; i++)
            cells.add(getCellName(i));
        return "size,"+Common.join(cells);
    }

}
